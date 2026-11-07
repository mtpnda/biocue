#!/usr/bin/env python3
"""
BioCue - Autonomous Wear OS Watch Wireless Connect Engine
Zero-dependency Python 3 utility to automatically discover, pair, and connect
to a physical Wear OS smartwatch (e.g. Galaxy Watch 4 Classic) over local Wi-Fi.

Minimizes human intervention:
  1. Checks if watch is already active in `adb devices`.
  2. Queries mDNS via ADB (`adb mdns services`) for auto-discovery of dynamic TLS ports.
  3. Uses cached watch IP (~/.android/watch_ip) and fast parallel port probing if mDNS is blocked.
  4. Automatically promotes connection to standard port 5555 (`adb tcpip 5555`).
"""

import argparse
import concurrent.futures
import os
import re
import socket
import subprocess
import sys
import time

CACHE_FILE = os.path.expanduser("~/.android/watch_ip")


def run_cmd(cmd, timeout=10):
    try:
        res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=timeout)
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except subprocess.TimeoutExpired:
        return -1, "", "Timeout"
    except Exception as e:
        return -1, "", str(e)


def is_already_connected():
    code, out, _ = run_cmd(["adb", "devices", "-l"])
    if code != 0:
        return False, None
    for line in out.splitlines()[1:]:
        line = line.strip()
        if not line:
            continue
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            # Check if this looks like a watch or network device
            identifier = parts[0]
            if any(k in line.lower() for k in ["watch", "wear", "wisee", "sm-r8", "sm_r8"]):
                return True, identifier
            if ":" in identifier:
                # Connected via IP:PORT
                return True, identifier
    return False, None


def get_cached_ip():
    if os.path.isfile(CACHE_FILE):
        try:
            with open(CACHE_FILE, "r") as f:
                ip = f.read().strip()
                if re.match(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$", ip):
                    return ip
        except Exception:
            pass
    return None


def save_cached_ip(ip):
    try:
        os.makedirs(os.path.dirname(CACHE_FILE), exist_ok=True)
        with open(CACHE_FILE, "w") as f:
            f.write(ip.strip())
    except Exception:
        pass


def discover_via_mdns():
    """Ask adb mdns services for broadcasting Wear OS watches."""
    run_cmd(["adb", "mdns", "check"])
    code, out, _ = run_cmd(["adb", "mdns", "services"], timeout=5)
    if code != 0 or not out:
        return []

    results = []
    # Lines look like:
    # adb-XXXXXXXX-YYYYYY\t_adb-tls-connect._tcp.\t192.168.1.50:41235
    for line in out.splitlines():
        if "_adb-tls-connect._tcp" in line or "_adb._tcp" in line:
            match = re.search(r"(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d{4,5})", line)
            if match:
                results.append((match.group(1), int(match.group(2))))
    return results


def check_port(ip, port, timeout=0.08):
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(timeout)
            if s.connect_ex((ip, port)) == 0:
                return port
    except Exception:
        pass
    return None


def fast_probe_ports(ip, port_candidates):
    """Rapidly probe port candidates in parallel using standard sockets."""
    with concurrent.futures.ThreadPoolExecutor(max_workers=100) as executor:
        futures = {executor.submit(check_port, ip, port): port for port in port_candidates}
        for future in concurrent.futures.as_completed(futures):
            res = future.result()
            if res is not None:
                # Cancel remaining
                for f in futures:
                    f.cancel()
                return res
    return None


def scan_known_ip(ip):
    """Check standard 5555 first, then the Android ephemeral wireless debugging range (30000-45000)."""
    # 1. Quick test on 5555
    if check_port(ip, 5555, timeout=0.2):
        return 5555

    # 2. Parallel scan across common Android wireless debugging range
    print(f"  ⚡ Probing active wireless debugging port on {ip}...")
    # Typically Android 11+ picks ports in 32000-46000
    ports = list(range(36000, 45000)) + list(range(30000, 36000))
    found = fast_probe_ports(ip, ports)
    return found


def connect(ip, port):
    target = f"{ip}:{port}"
    print(f"  🔌 Connecting to {target}...")
    code, out, err = run_cmd(["adb", "connect", target], timeout=8)
    print(f"  ↳ {out}")
    if "connected to" in out.lower() and "cannot" not in out.lower():
        save_cached_ip(ip)
        # Attempt to lock to port 5555 for future instant connections
        if port != 5555:
            print(f"  🔒 Promoting connection to fixed port 5555 (`adb tcpip 5555`)...")
            run_cmd(["adb", "-s", target, "tcpip", "5555"], timeout=5)
        return True
    return False


def pair(ip, port, code):
    print(f"  🔑 Pairing Mac with {ip}:{port} using code {code}...")
    cmd = ["adb", "pair", f"{ip}:{port}", str(code)]
    ret, out, err = run_cmd(cmd, timeout=10)
    print(f"  ↳ {out or err}")
    if "successfully paired" in (out + err).lower():
        save_cached_ip(ip)
        print("  ✓ Pairing successful! Saved watch IP.")
        return True
    return False


def main():
    parser = argparse.ArgumentParser(description="BioCue Autonomous Watch Auto-Connect Engine")
    parser.add_argument("--ip", help="Known IP address of the Galaxy Watch (overrides cached IP)")
    parser.add_argument("--port", type=int, help="Specific port to connect to")
    parser.add_argument("--pair", nargs=2, metavar=("PORT", "CODE"), help="One-time pairing: --pair <pairing_port> <pairing_code>")
    args = parser.parse_args()

    print("══════════════════════════════════════════════════════════════")
    print("⌚ BioCue Wear OS Watch Autonomous Connect Engine")
    print("══════════════════════════════════════════════════════════════")

    # 1. Handle one-time pairing if requested
    if args.pair:
        port, code = args.pair
        ip = args.ip or get_cached_ip()
        if not ip:
            print("❌ Please provide the watch IP for pairing via --ip <watch_ip>")
            sys.exit(1)
        if pair(ip, port, code):
            print("\nNext step: run `make connect-watch` to establish daily connection.")
            sys.exit(0)
        else:
            sys.exit(1)

    # 2. Check if already connected
    connected, ident = is_already_connected()
    if connected:
        print(f"✓ Watch is already connected and active: [{ident}]")
        sys.exit(0)

    # 3. Try mDNS discovery
    print("🔍 Discovering Wear OS device over Wi-Fi via mDNS...")
    services = discover_via_mdns()
    if services:
        for ip, port in services:
            print(f"  ✓ Found mDNS broadcast: {ip}:{port}")
            if connect(ip, port):
                print(f"🎉 Successfully connected to watch at {ip}:{port}!")
                sys.exit(0)

    # 4. Fallback to IP probe
    target_ip = args.ip or get_cached_ip()
    if target_ip:
        print(f"🔍 Attempting connection to known watch IP: {target_ip}...")
        if args.port:
            if connect(target_ip, args.port):
                print(f"🎉 Successfully connected to {target_ip}:{args.port}!")
                sys.exit(0)
        else:
            found_port = scan_known_ip(target_ip)
            if found_port:
                print(f"  ✓ Found active ADB port: {found_port}")
                if connect(target_ip, found_port):
                    print(f"🎉 Successfully connected to watch at {target_ip}:{found_port}!")
                    sys.exit(0)

    print("\n⚠️  Could not automatically discover active watch.")
    print("   Please ensure:")
    print("   1. Watch and Mac are on the same Wi-Fi network.")
    print("   2. Wireless Debugging is turned ON in Watch Settings -> Developer Options.")
    print("   3. (First time only) Pair using: make pair-watch IP=<watch_ip> PORT=<pair_port> CODE=<code>")
    sys.exit(1)


if __name__ == "__main__":
    main()
