import re

html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BioCue V3 - Current Android Layout Prototype</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
        body { font-family: 'Inter', sans-serif; background-color: #e5e7eb; margin: 0; padding: 40px; }
        
        .watch-frame {
            width: 220px; height: 220px;
            border-radius: 50%;
            background-color: #000;
            border: 12px solid #374151;
            box-shadow: 0 15px 35px rgba(0,0,0,0.3);
            position: relative; overflow: hidden;
            display: flex; flex-direction: column;
        }

        .phone-frame {
            width: 375px; height: 812px;
            background-color: #0F172A; /* BgSlate */
            border-radius: 40px;
            border: 14px solid #111827;
            box-shadow: 0 25px 50px -12px rgba(0,0,0,0.5);
            position: relative; overflow: hidden;
            display: flex; flex-direction: column;
        }
        
        .phone-frame::before {
            content: '';
            position: absolute; top: 0; left: 50%; transform: translateX(-50%);
            width: 120px; height: 30px;
            background-color: #111827;
            border-bottom-left-radius: 16px; border-bottom-right-radius: 16px;
            z-index: 50;
        }

        .scrollable::-webkit-scrollbar { display: none; }
        .scrollable { overflow-y: auto; scroll-behavior: smooth; }
    </style>
</head>
<body class="flex flex-col items-center">
    
    <div class="mb-12 text-center max-w-2xl">
        <h1 class="text-3xl font-bold text-gray-900 mb-2">BioCue - Current App Baseline (1:1)</h1>
        <p class="text-gray-600 text-lg">A strict HTML reproduction of the current Jetpack Compose layout, preparing for Ideation (V2).</p>
    </div>

    <div class="flex flex-wrap justify-center items-start gap-12 w-full max-w-7xl">
        
        <!-- ======================= -->
        <!-- WEARER APP (WATCH)      -->
        <!-- ======================= -->
        <div class="flex flex-col items-center gap-4">
            <div class="text-center">
                <h2 class="text-xl font-bold text-gray-800">Wearer Watch</h2>
                <p class="text-sm text-gray-500">Carousel State</p>
            </div>
            <div class="watch-frame p-4 justify-center gap-2 items-center bg-black">
                <h3 class="text-white text-xs mb-1">How do you feel?</h3>
                <button class="w-11/12 py-2 bg-[#1E1E2E] rounded-full text-xs text-white flex items-center justify-center gap-1.5">
                    🌩️ Overwhelmed
                </button>
                <button class="w-11/12 py-2 bg-[#1E1E2E] rounded-full text-xs text-white flex items-center justify-center gap-1.5 opacity-80 scale-95">
                    ✨ Excited
                </button>
                <button class="w-11/12 py-2 bg-[#1E1E2E] rounded-full text-xs text-white flex items-center justify-center gap-1.5 opacity-60 scale-90">
                    🏃 Active
                </button>
            </div>
        </div>

        <!-- ======================= -->
        <!-- WEARER APP (PHONE)      -->
        <!-- ======================= -->
        <div class="flex flex-col items-center gap-4">
            <div class="text-center">
                <h2 class="text-xl font-bold text-gray-800">Wearer Phone</h2>
                <p class="text-sm text-gray-500">Matching the screenshots</p>
            </div>
            
            <div class="phone-frame text-white">
                <!-- Top Header Bar -->
                <div class="pt-14 pb-2 px-4 flex justify-between items-center shrink-0">
                    <div>
                        <h1 class="text-2xl font-bold text-white leading-tight">BioCue</h1>
                        <p class="text-[13px] font-bold text-[#34D399]">Wearer</p>
                    </div>
                    <div class="w-11 h-11 bg-[#334155] rounded-full flex items-center justify-center text-[#3b82f6] text-xl opacity-80">
                        👤
                    </div>
                </div>

                <div class="px-4 pb-6 scrollable flex-1 flex flex-col gap-4">
                    
                    <!-- Developer Stubs -->
                    <div class="bg-[#1E293B] rounded-2xl p-4 shadow-sm">
                        <h3 class="text-yellow-400 text-[13px] font-bold mb-3">Developer Stubs</h3>
                        
                        <p class="text-[#94A3B8] text-[13px] mb-2">Continuous Sensor Mock</p>
                        <div class="flex gap-2 mb-4">
                            <button class="flex-1 bg-[#334155] text-white text-[13px] font-medium py-2 rounded-xl">Low</button>
                            <button class="flex-1 bg-[#334155] text-white text-[13px] font-medium py-2 rounded-xl">Medium</button>
                            <button class="flex-1 bg-[#334155] text-white text-[13px] font-medium py-2 rounded-xl">High</button>
                        </div>
                        
                        <p class="text-[#94A3B8] text-[13px] mb-2">Simulate Activity Override</p>
                        <div class="flex gap-2 mb-4">
                            <button class="flex-1 bg-white text-black text-[13px] font-medium py-2 rounded-xl">Auto</button>
                            <button class="flex-1 bg-[#334155] text-white text-[13px] font-medium py-2 rounded-xl">Stimming</button>
                            <button class="flex-1 bg-[#334155] text-white text-[13px] font-medium py-2 rounded-xl">Exercise</button>
                        </div>
                        
                        <p class="text-[#94A3B8] text-[13px] mb-2">Simulate Watch Tap (Manual Event)</p>
                        <div class="flex gap-2">
                            <button class="flex-1 bg-[#334155] text-white text-[10px] font-medium py-2.5 rounded-xl truncate px-1">OVERWHEL</button>
                            <button class="flex-1 bg-[#334155] text-white text-[10px] font-medium py-2.5 rounded-xl truncate px-1">EXCITED</button>
                            <button class="flex-1 bg-[#334155] text-white text-[10px] font-medium py-2.5 rounded-xl truncate px-1">ACTIVE</button>
                        </div>
                    </div>

                    <!-- Watch Disconnected -->
                    <div class="flex flex-col items-center justify-center text-center py-4">
                        <span class="text-4xl mb-2">⚠️</span>
                        <h3 class="text-[#FBBF24] font-bold text-base mb-2">Watch is disconnected</h3>
                        <p class="text-[#94A3B8] text-[13px] px-2 leading-relaxed">Unable to monitor User's stress levels. Please ensure the watch is worn and connected.</p>
                    </div>

                    <!-- Ongoing Event Warning -->
                    <div class="bg-[#EF4444]/10 border border-[#EF4444]/5 rounded-2xl p-4 shadow-sm">
                        <h3 class="text-[#EF4444] font-bold text-[11px] tracking-widest uppercase mb-2">PUSH NOTIFICATION</h3>
                        <p class="text-white text-[14px]">User's stress has been elevated for 5 minutes. They may need support.</p>
                    </div>

                    <!-- Quick Actions -->
                    <div class="bg-[#1E293B] rounded-2xl p-4 shadow-sm">
                        <h2 class="text-[#94A3B8] font-bold text-xs tracking-widest uppercase mb-4">QUICK ACTIONS</h2>
                        <button class="w-full text-left py-3 px-4 rounded-xl flex items-center bg-[#334155] text-white font-bold text-[13px] transition active:scale-95">
                            <span class="mr-3 text-lg opacity-80">🎵</span> Calm Audio
                        </button>
                    </div>

                    <!-- Pattern Review Empty -->
                    <div class="bg-[#1E293B] rounded-2xl p-4 shadow-sm">
                        <h2 class="text-[#94A3B8] font-bold text-xs tracking-widest uppercase mb-4">PATTERN REVIEW</h2>
                        <div class="flex flex-col items-center justify-center py-4">
                            <span class="text-3xl mb-2">✨</span>
                            <h3 class="text-[#34D399] font-bold text-[15px] mb-1">All caught up</h3>
                            <p class="text-[#94A3B8] text-[13px]">BioCue is learning User's patterns.</p>
                        </div>
                    </div>

                    <!-- Journal Empty -->
                    <div class="bg-[#1E293B] rounded-2xl p-4 shadow-sm">
                        <div class="flex justify-between items-center mb-6">
                            <h2 class="text-[#94A3B8] font-bold text-xs tracking-widest uppercase">JOURNAL</h2>
                            <div class="flex items-center gap-2">
                                <span class="text-[#94A3B8] text-[10px]">◀</span>
                                <span class="text-white text-[13px] font-bold">Today</span>
                            </div>
                        </div>
                        <p class="text-[#94A3B8] text-[13px]">No events on this day. All calm ✨</p>
                    </div>

                </div>
            </div>
        </div>

        <!-- ======================= -->
        <!-- CAREGIVER APP (PHONE)   -->
        <!-- ======================= -->
        <div class="flex flex-col items-center gap-4">
            <div class="text-center">
                <h2 class="text-xl font-bold text-gray-800">Caregiver Phone</h2>
                <p class="text-sm text-gray-500">Current Compose Layout</p>
            </div>
            
            <div class="phone-frame text-white">
                <div class="p-4 pt-12 scrollable flex-1 flex flex-col gap-4">
                    
                    <!-- Ongoing Event Warning -->
                    <div class="bg-[#EF4444]/10 border border-[#EF4444]/5 rounded-2xl p-4 shadow-sm">
                        <h3 class="text-[#EF4444] font-bold text-[11px] tracking-widest uppercase mb-2">PUSH NOTIFICATION</h3>
                        <p class="text-white text-[14px]">Alex's stress has been elevated for 5 minutes. They may need support.</p>
                    </div>

                    <!-- Pattern Review -->
                    <div class="bg-[#1E293B] rounded-2xl p-4 shadow-sm">
                        <div class="flex justify-between items-center mb-5">
                            <h2 class="text-[#94A3B8] font-bold text-xs tracking-widest uppercase">PATTERN REVIEW</h2>
                            <span class="bg-[#EF4444] text-white text-[10px] font-bold px-2 py-0.5 rounded-full">1</span>
                        </div>
                        
                        <p class="text-white text-[14px] mb-4 leading-relaxed">User reported feeling Overwhelmed. Please add environmental context to help BioCue learn.</p>
                        <p class="text-white font-bold text-[15px] mb-4">3:30 PM</p>
                        
                        <div class="bg-[#0F172A] rounded-xl p-4 mb-5 shadow-inner">
                            <p class="text-[#94A3B8] text-[10px] tracking-widest uppercase mb-3">CONTEXT CUES</p>
                            <div class="flex items-center gap-3 mb-2">
                                <span class="text-sm bg-gray-200 rounded text-center leading-none">🔄</span>
                                <span class="text-[#CBD5E1] text-[14px]">Manually Triggered</span>
                            </div>
                            <div class="flex items-center gap-3">
                                <span class="text-sm">☁️</span>
                                <span class="text-[#CBD5E1] text-[14px]">Synced from Wearer</span>
                            </div>
                        </div>

                        <p class="text-[#34D399] text-[13px] font-medium mb-4">💡 What was happening around User?</p>
                        
                        <div class="flex flex-col gap-3">
                            <button class="w-full text-left py-3.5 px-4 rounded-xl flex items-center bg-[#fca5a5]/15 text-[#fca5a5] font-bold text-[14px] transition active:scale-95">
                                <span class="mr-3 text-[16px]">🔊</span> Loud Noises
                            </button>
                            <button class="w-full text-left py-3.5 px-4 rounded-xl flex items-center bg-[#fdba74]/15 text-[#fdba74] font-bold text-[14px] transition active:scale-95">
                                <span class="mr-3 text-[16px]">👥</span> Crowded Space
                            </button>
                            <button class="w-full text-left py-3.5 px-4 rounded-xl flex items-center bg-[#6ee7b7]/15 text-[#6ee7b7] font-bold text-[14px] transition active:scale-95">
                                <span class="mr-3 text-[16px]">📋</span> Task Demand
                            </button>
                            <button class="w-full text-left py-3.5 px-4 rounded-xl flex items-center bg-[#93c5fd]/15 text-[#93c5fd] font-bold text-[14px] transition active:scale-95">
                                <span class="mr-3 text-[16px] bg-white rounded-full leading-none">🔄</span> Routine Change
                            </button>
                            <button class="w-full text-left py-3.5 px-4 rounded-xl flex items-center bg-[#94a3b8]/15 text-[#94a3b8] font-bold text-[14px] transition active:scale-95">
                                <span class="mr-3 text-[16px]">🤷</span> Unknown
                            </button>
                        </div>
                    </div>

                    <!-- Journal -->
                    <div class="bg-[#1E293B] rounded-2xl p-4 shadow-sm">
                        <div class="flex justify-between items-center mb-5">
                            <h2 class="text-[#94A3B8] font-bold text-xs tracking-widest uppercase">JOURNAL</h2>
                            <div class="flex items-center gap-2">
                                <span class="text-[#94A3B8] text-[10px]">◀</span>
                                <span class="text-white text-[13px] font-bold">Today</span>
                                <span class="text-transparent text-[10px]">▶</span>
                            </div>
                        </div>
                        <div class="flex items-center gap-3">
                            <span class="text-2xl">🌩️</span>
                            <div>
                                <p class="text-white text-[14px] font-bold">OVERWHELMED - LOUD NOISES</p>
                                <p class="text-[#94A3B8] text-[13px]">Manual Event</p>
                            </div>
                            <span class="ml-auto text-[#94A3B8] text-[13px] font-bold">08:30 AM</span>
                        </div>
                    </div>

                </div>
            </div>
        </div>

    </div>

    <script>
        // Make Pattern Review buttons interactive
        document.addEventListener('DOMContentLoaded', () => {
            const buttons = document.querySelectorAll('.bg-\\\\[\\\\#1E293B\\\\] .flex-col.gap-3 button');
            
            buttons.forEach(btn => {
                btn.addEventListener('click', () => {
                    // Extract color class logic for faking click (simplified for prototype)
                    btn.style.opacity = '0.5';
                    btn.style.transform = 'scale(0.95)';
                    
                    // Fake a resolve animation after a short delay
                    setTimeout(() => {
                        const parentCard = btn.closest('.bg-\\\\[\\\\#1E293B\\\\]');
                        if (parentCard) {
                            parentCard.style.transition = 'all 0.5s ease';
                            parentCard.style.opacity = '0';
                            setTimeout(() => parentCard.style.display = 'none', 500);
                        }
                    }, 400);
                });
            });
        });
    </script>
</body>
</html>"""

with open('.agents/SPEC/PROTOTYPE.html', 'w') as f:
    f.write(html_content)

print("Rewritten PROTOTYPE.html completely.")
