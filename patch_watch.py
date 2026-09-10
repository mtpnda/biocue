import re

with open('.agents/SPEC/PROTOTYPE.html', 'r') as f:
    html = f.read()

new_watch_content = """        <!-- ======================= -->
        <!-- WEARER APP (WATCH)      -->
        <!-- ======================= -->
        <div class="flex flex-col items-center gap-4">
            <div class="text-center">
                <h2 class="text-xl font-bold text-gray-800">Wearer Watch</h2>
                <p class="text-sm text-gray-500">Permission State</p>
            </div>
            <div class="watch-frame items-center justify-center bg-black p-4 text-center">
                <span class="absolute top-2 text-[10px] text-white font-medium tracking-wider">1:21</span>
                
                <h2 class="text-white font-bold text-sm mb-1 mt-4">Welcome to BioCue</h2>
                <p class="text-gray-300 text-[11px] mb-4 leading-relaxed px-2">Sensor access is needed to monitor stress.</p>
                
                <button class="bg-[#5B4CFF] text-[#1a1060] font-medium text-xs px-5 py-2.5 rounded-full w-[80%] hover:brightness-110 transition">
                    Grant Access
                </button>
            </div>
        </div>"""

pattern = r"<!-- ======================= -->\s*<!-- WEARER APP \(WATCH\)      -->\s*<!-- ======================= -->.*?</div>\s*</div>"
html = re.sub(pattern, new_watch_content, html, flags=re.DOTALL)

with open('.agents/SPEC/PROTOTYPE.html', 'w') as f:
    f.write(html)

print("Patched Watch HTML.")
