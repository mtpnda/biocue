import re

with open('.agents/SPEC/PROTOTYPE.html', 'r') as f:
    html = f.read()

# Replace the Pattern Review section
# From <div class="bg-[#1E293B] rounded-2xl p-4"> (the pattern review wrapper)
# To just before Journal

new_pattern_review = """                    <!-- Pattern Review -->
                    <div class="bg-[#1E293B] rounded-2xl p-4 shadow-sm">
                        <div class="flex justify-between items-center mb-4">
                            <h2 class="text-[#94A3B8] font-bold text-xs tracking-widest uppercase">PATTERN REVIEW</h2>
                            <span class="bg-[#EF4444] text-white text-[10px] font-bold px-2 py-0.5 rounded-full">1</span>
                        </div>
                        
                        <p class="text-white text-[13px] mb-4 leading-relaxed">User reported feeling Overwhelmed. Please add environmental context to help BioCue learn.</p>
                        <p class="text-white font-bold text-sm mb-3">3:30 PM</p>
                        
                        <div class="bg-[#0F172A] rounded-xl p-4 mb-4">
                            <p class="text-[#94A3B8] text-[10px] tracking-widest uppercase mb-3">CONTEXT CUES</p>
                            <div class="flex items-center gap-2 mb-2">
                                <span class="text-sm">🔄</span>
                                <span class="text-[#CBD5E1] text-[13px]">Manually Triggered</span>
                            </div>
                            <div class="flex items-center gap-2">
                                <span class="text-sm">☁️</span>
                                <span class="text-[#CBD5E1] text-[13px]">Synced from Wearer</span>
                            </div>
                        </div>

                        <p class="text-[#34D399] text-[13px] font-medium mb-4">💡 What was happening around User?</p>
                        
                        <div class="flex flex-col gap-3">
                            <button class="w-full text-left py-3 px-4 rounded-xl flex items-center bg-[#fca5a5]/15 text-[#fca5a5] font-bold text-[13px] transition active:scale-95">
                                <span class="mr-3 text-lg">🔊</span> Loud Noises
                            </button>
                            <button class="w-full text-left py-3 px-4 rounded-xl flex items-center bg-[#fdba74]/15 text-[#fdba74] font-bold text-[13px] transition active:scale-95">
                                <span class="mr-3 text-lg">👥</span> Crowded Space
                            </button>
                            <button class="w-full text-left py-3 px-4 rounded-xl flex items-center bg-[#6ee7b7]/15 text-[#6ee7b7] font-bold text-[13px] transition active:scale-95">
                                <span class="mr-3 text-lg">📋</span> Task Demand
                            </button>
                            <button class="w-full text-left py-3 px-4 rounded-xl flex items-center bg-[#93c5fd]/15 text-[#93c5fd] font-bold text-[13px] transition active:scale-95">
                                <span class="mr-3 text-lg">🔄</span> Routine Change
                            </button>
                            <button class="w-full text-left py-3 px-4 rounded-xl flex items-center bg-[#94a3b8]/15 text-[#94a3b8] font-bold text-[13px] transition active:scale-95">
                                <span class="mr-3 text-lg">🤷</span> Unknown Context
                            </button>
                        </div>
                    </div>"""

# Use regex to replace the Pattern Review block
pattern = r"<!-- Pattern Review -->.*?<!-- Journal -->"
html = re.sub(pattern, new_pattern_review + "\n\n                    <!-- Journal -->", html, flags=re.DOTALL)

with open('.agents/SPEC/PROTOTYPE.html', 'w') as f:
    f.write(html)

print("Patched Pattern Review HTML.")
