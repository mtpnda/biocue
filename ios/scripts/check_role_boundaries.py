#!/usr/bin/env python3
"""
Fails (exit 1) if the iOS source tree violates the wearer / caregiver separation.
Run from ios/:   python3 scripts/check_role_boundaries.py        (wire it into CI)

Layout it enforces (see docs/SPEC.md section 4):
  Sources/Wearer/     WEARER-ONLY: watch link (CoreBluetooth), raw vitals, detection, event creation
  Sources/Caregiver/  CAREGIVER-ONLY: pairing by code, reading + labelling a paired wearer's events
  Sources/Shared/     role-AGNOSTIC: must not depend on either role's code
  Sources/App/        composition root: may reference both (it is the only place that decides)
"""
import re, sys, pathlib

root = pathlib.Path(__file__).resolve().parent.parent / "BioCue" / "Sources"

CAREGIVER_SYMBOLS = ["CaregiverCloud", "CaregiverViewModel", "CaregiverRoot", "CaregiverHomeView", "CaregiverPairingView",
                     "AccessRevokedView", "observePairings", "leavePairing", "lookupCode", "reRequest", "createPairing",
                     "CaregiverPairingLifecycle", "CaregiverPairingStore", "CaregiverAccess", "ReviewSubmission", "ReviewStore", "writeReview"]
WEARER_SYMBOLS = ["WearerCloud", "WearerService", "WearerRoot", "WearerHomeView", "WearerCircleViewModel", "WatchLink",
                  "WatchLinking", "CoreBluetooth", "CBCentralManager", "SensorWindow", "DetectionPipeline", "EventIngest",
                  "FileEventStore", "FileWindowStore", "EventStore", "WindowStore", "issueCode", "consumeCodes", "markApproved", "markRemoved",
                  "deletePairing", "WearerPairingLifecycle", "WearerPairingStore", "syncEvent", "observeCaregivers", "LocalAlertNotifier",
                  "AlertNotifying"]

# Files in Shared/ that legitimately DECLARE or IMPLEMENT the role-split interfaces.
SHARED_EXEMPT = {"CloudModels.swift", "FirebaseCloud.swift"}

def code_lines(path):
    for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        code = line.split("//")[0]          # ignore comments
        if code.strip():
            yield i, code

def scan(files, forbidden, who):
    bad = []
    for f in files:
        for i, code in code_lines(f):
            for sym in forbidden:
                if re.search(r"\b" + re.escape(sym) + r"\b", code):
                    bad.append(f"{who}: {f.relative_to(root)}:{i} references '{sym}'")
    return bad

def swift(dirname):
    d = root / dirname
    return sorted(d.rglob("*.swift")) if d.exists() else []

problems = []
wearer, caregiver, shared = swift("Wearer"), swift("Caregiver"), [f for f in swift("Shared") if f.name not in SHARED_EXEMPT]

# the scan must not pass vacuously
for name, files, minimum in (("Wearer", wearer, 6), ("Caregiver", caregiver, 2), ("Shared", shared, 3)):
    if len(files) < minimum:
        problems.append(f"expected >= {minimum} files in Sources/{name}, found {len(files)} (wrong working directory?)")

problems += scan(wearer, CAREGIVER_SYMBOLS, "WEARER code reaches into caregiver-only API")
problems += scan(caregiver, WEARER_SYMBOLS, "CAREGIVER code reaches into wearer-only API")
problems += scan(shared, CAREGIVER_SYMBOLS + WEARER_SYMBOLS, "SHARED code depends on a role")

if problems:
    print("Role boundary violations:\n  " + "\n  ".join(problems))
    sys.exit(1)
print(f"OK: {len(wearer)} wearer, {len(caregiver)} caregiver, {len(shared)} shared files — no boundary violations")
