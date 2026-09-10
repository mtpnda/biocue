html = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>BioCue V3 - Apple Design Redesign</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
        /* Emulate San Francisco / Apple System Font */
        body { 
            font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", sans-serif;
            background-color: #000000; 
            margin: 0; 
            padding: 40px; 
            color: #ffffff;
            /* subtle ambient gradient */
            background: radial-gradient(circle at top left, #1a1a2e, #000000 70%);
            min-height: 100vh;
        }
        
        /* Glassmorphism utilities */
        .glass-panel {
            background: rgba(255, 255, 255, 0.05);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3);
        }
        
        .glass-button {
            background: rgba(255, 255, 255, 0.1);
            backdrop-filter: blur(12px);
            -webkit-backdrop-filter: blur(12px);
            transition: all 0.2s cubic-bezier(0.25, 1, 0.5, 1);
        }
        .glass-button:active {
            transform: scale(0.96);
            background: rgba(255, 255, 255, 0.2);
        }

        /* Apple-style squircle watch */
        .watch-frame {
            width: 200px; height: 240px; /* Apple Watch Ultra ratio approx */
            border-radius: 40px;
            background-color: #000;
            border: 10px solid #2a2a2a;
            box-shadow: inset 0 0 0 2px #444, 0 20px 40px rgba(0,0,0,0.5);
            position: relative; overflow: hidden;
        }

        /* Apple-style iPhone */
        .phone-frame {
            width: 390px; height: 844px; /* iPhone 14 Pro size */
            background-color: #000000;
            border-radius: 55px;
            border: 12px solid #222;
            box-shadow: inset 0 0 0 2px #444, 0 30px 60px -15px rgba(0,0,0,0.8);
            position: relative; overflow: hidden;
            display: flex; flex-direction: column;
        }
        
        /* Dynamic Island */
        .dynamic-island {
            position: absolute; top: 12px; left: 50%; transform: translateX(-50%);
            width: 120px; height: 35px;
            background-color: #000;
            border-radius: 20px;
            z-index: 100;
            box-shadow: inset 0 0 1px 1px rgba(255,255,255,0.05);
        }

        .scrollable::-webkit-scrollbar { display: none; }
        .scrollable { overflow-y: auto; scroll-behavior: smooth; }
        
        /* iOS Toggle Switch */
        .ios-toggle {
            appearance: none;
            width: 50px; height: 30px;
            background: #39393D;
            border-radius: 30px;
            position: relative;
            cursor: pointer;
            outline: none;
            transition: background 0.3s;
        }
        .ios-toggle::after {
            content: ''; position: absolute;
            top: 2px; left: 2px;
            width: 26px; height: 26px;
            background: #fff;
            border-radius: 50%;
            transition: transform 0.3s cubic-bezier(0.2, 0.85, 0.32, 1.2);
            box-shadow: 0 2px 5px rgba(0,0,0,0.2);
        }
        .ios-toggle:checked { background: #34C759; }
        .ios-toggle:checked::after { transform: translateX(20px); }
    </style>
</head>
<body class="flex flex-col items-center">
    
    <div class="mb-12 text-center max-w-2xl">
        <h1 class="text-3xl font-semibold text-white mb-2 tracking-tight">BioCue V2 - Apple Design Redesign</h1>
        <p class="text-gray-400 text-lg">Grounded in HIG: Liquid glass, typographic hierarchy, and fluid interactions.</p>
    </div>

    <div class="flex flex-wrap justify-center items-start gap-16 w-full max-w-7xl">
        
        <!-- ======================= -->
        <!-- WEARER APP (WATCH)      -->
        <!-- ======================= -->
        <div class="flex flex-col items-center gap-4">
            <div class="text-center">
                <h2 class="text-lg font-semibold text-white">Apple Watch</h2>
                <p class="text-sm text-gray-400">Interaction Flow</p>
            </div>
            
            <div class="watch-frame text-white">
                <div class="absolute top-2 left-0 right-0 text-center z-50 flex justify-end px-3">
                    <span class="text-[12px] font-medium tracking-tight text-white/80">9:41</span>
                </div>
                
                <!-- State: ANCHOR -->
                <div id="w-anchor" class="absolute inset-0 flex flex-col items-center justify-center p-3 transition-opacity duration-300">
                    <div class="flex items-center gap-1.5 mb-6 opacity-70">
                        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#34C759" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"></rect><line x1="12" y1="18" x2="12.01" y2="18"></line></svg>
                        <span class="text-[10px] font-medium text-[#34C759]">Connected</span>
                    </div>
                    
                    <button onclick="setWatch('CAROUSEL')" class="w-[120px] h-[120px] rounded-full bg-gradient-to-b from-[#2C2C2E] to-[#1C1C1E] border border-white/10 shadow-[0_10px_30px_rgba(0,0,0,0.5),_inset_0_2px_4px_rgba(255,255,255,0.1)] flex items-center justify-center active:scale-95 transition-all duration-300">
                        <div class="w-16 h-16 rounded-full bg-white/5 border border-white/5 backdrop-blur-md"></div>
                    </button>
                    <p class="text-white/40 text-[10px] mt-4 font-medium tracking-wide">TAP IF OVERWHELMED</p>
                </div>
                
                <!-- State: CAROUSEL -->
                <div id="w-carousel" class="absolute inset-0 flex flex-col items-center justify-start pt-8 pb-4 px-2 hidden opacity-0 transition-opacity duration-300 overflow-y-auto scrollable">
                    <h3 class="text-white/80 text-[13px] font-semibold mb-3 tracking-tight">How do you feel?</h3>
                    <div class="flex flex-col gap-2 w-full">
                        <button onclick="setWatch('ONGOING', '#FF3B30', 'Overwhelmed', '🌩️')" class="w-full py-3 glass-button rounded-[16px] text-[14px] font-semibold text-[#FF3B30] flex items-center px-4">
                            <span class="text-lg mr-2">🌩️</span> Overwhelmed
                        </button>
                        <button onclick="setWatch('ONGOING', '#FF9500', 'Excited', '✨')" class="w-full py-3 glass-button rounded-[16px] text-[14px] font-semibold text-[#FF9500] flex items-center px-4">
                            <span class="text-lg mr-2">✨</span> Excited
                        </button>
                        <button onclick="setWatch('ONGOING', '#5AC8FA', 'Active', '🏃')" class="w-full py-3 glass-button rounded-[16px] text-[14px] font-semibold text-[#5AC8FA] flex items-center px-4">
                            <span class="text-lg mr-2">🏃</span> Active
                        </button>
                    </div>
                </div>

                <!-- State: ONGOING -->
                <div id="w-ongoing" class="absolute inset-0 flex flex-col items-center justify-center p-3 hidden opacity-0 transition-opacity duration-300">
                    <button onclick="setWatch('SUCCESS')" class="relative w-32 h-32 rounded-full flex items-center justify-center active:scale-95 transition-all">
                        <!-- Pulsing rings -->
                        <div id="w-pulse-1" class="absolute inset-0 rounded-full border-2 opacity-50 animate-[ping_2s_cubic-bezier(0,0,0.2,1)_infinite]"></div>
                        <div id="w-pulse-bg" class="absolute inset-2 rounded-full opacity-20"></div>
                        <div id="w-pulse-core" class="w-20 h-20 rounded-full flex items-center justify-center shadow-[0_0_20px_currentColor]">
                            <span id="w-ongoing-icon" class="text-3xl">🌩️</span>
                        </div>
                    </button>
                    <p id="w-ongoing-label" class="font-bold text-[13px] mt-4 tracking-wide uppercase">ACTIVE</p>
                    <p class="text-white/50 text-[11px] mt-1">Tap to end event</p>
                </div>

                <!-- State: SUCCESS -->
                <div id="w-success" class="absolute inset-0 flex flex-col items-center justify-center p-4 hidden opacity-0 transition-opacity duration-300">
                    <div class="w-16 h-16 rounded-full bg-[#34C759]/20 flex items-center justify-center mb-3">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#34C759" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                    </div>
                    <h2 class="text-[#34C759] font-semibold text-[15px]">Recorded</h2>
                </div>
            </div>
        </div>

        <!-- ======================= -->
        <!-- WEARER APP (PHONE)      -->
        <!-- ======================= -->
        <div class="flex flex-col items-center gap-4">
            <div class="text-center">
                <h2 class="text-lg font-semibold text-white">iPhone - Wearer</h2>
                <p class="text-sm text-gray-400">Settings & Context</p>
            </div>
            
            <div class="phone-frame">
                <div class="dynamic-island"></div>
                
                <!-- Background Gradient -->
                <div class="absolute inset-0 bg-gradient-to-br from-[#0a0a1a] to-[#000000] -z-10"></div>
                <!-- Ambient colorful blobs behind glass -->
                <div class="absolute top-[-100px] right-[-100px] w-64 h-64 bg-purple-600/30 rounded-full blur-3xl -z-10"></div>
                <div class="absolute bottom-[200px] left-[-50px] w-48 h-48 bg-blue-600/20 rounded-full blur-3xl -z-10"></div>
                
                <div class="pt-16 pb-4 px-6 shrink-0">
                    <h1 class="text-[34px] font-bold text-white tracking-tight leading-tight">BioCue</h1>
                    <p class="text-[15px] font-semibold text-[#5AC8FA] mt-1">Wearer Companion</p>
                </div>

                <div class="px-5 pb-8 scrollable flex-1 flex flex-col gap-5">
                    
                    <!-- Alert: Disconnected -->
                    <div class="glass-panel rounded-[24px] p-5 flex items-start gap-4 border-l-4 border-l-[#FF9500]">
                        <div class="bg-[#FF9500]/20 p-2 rounded-full shrink-0">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#FF9500" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                        </div>
                        <div>
                            <h3 class="text-white font-semibold text-[16px] mb-1">Watch Disconnected</h3>
                            <p class="text-white/60 text-[14px] leading-snug">Unable to monitor stress levels. Please ensure the watch is worn and charged.</p>
                        </div>
                    </div>

                    <!-- Quick Actions -->
                    <div>
                        <h2 class="text-white/50 font-semibold text-[12px] uppercase tracking-wider mb-2 ml-2">Quick Actions</h2>
                        <div class="glass-panel rounded-[24px] overflow-hidden">
                            <div class="p-4 flex justify-between items-center border-b border-white/5">
                                <div class="flex items-center gap-3">
                                    <div class="w-8 h-8 rounded-full bg-[#5AC8FA]/20 flex items-center justify-center">🎵</div>
                                    <span class="text-white text-[16px] font-medium">Calm Audio</span>
                                </div>
                                <input type="checkbox" class="ios-toggle">
                            </div>
                            <div class="p-4 flex justify-between items-center">
                                <div class="flex items-center gap-3">
                                    <div class="w-8 h-8 rounded-full bg-[#FF3B30]/20 flex items-center justify-center">🔕</div>
                                    <span class="text-white text-[16px] font-medium">Silent Mode</span>
                                </div>
                                <input type="checkbox" class="ios-toggle">
                            </div>
                        </div>
                    </div>

                    <!-- Developer Stubs -->
                    <div>
                        <h2 class="text-white/50 font-semibold text-[12px] uppercase tracking-wider mb-2 ml-2">Developer Tools</h2>
                        <div class="glass-panel rounded-[24px] p-5">
                            <p class="text-white/80 text-[14px] font-medium mb-3">Simulate Watch Tap</p>
                            <div class="flex gap-2">
                                <button class="flex-1 glass-button py-2.5 rounded-[12px] text-[13px] font-semibold text-[#FF3B30]">Overwhelm</button>
                                <button class="flex-1 glass-button py-2.5 rounded-[12px] text-[13px] font-semibold text-[#FF9500]">Excited</button>
                                <button class="flex-1 glass-button py-2.5 rounded-[12px] text-[13px] font-semibold text-[#5AC8FA]">Active</button>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>

        <!-- ======================= -->
        <!-- CAREGIVER APP (PHONE)   -->
        <!-- ======================= -->
        <div class="flex flex-col items-center gap-4">
            <div class="text-center">
                <h2 class="text-lg font-semibold text-white">iPhone - Caregiver</h2>
                <p class="text-sm text-gray-400">Clinical & Review UI</p>
            </div>
            
            <div class="phone-frame">
                <div class="dynamic-island"></div>
                <div class="absolute inset-0 bg-[#000000] -z-10"></div>
                <!-- Ambient glow behind review cards -->
                <div class="absolute top-[30%] left-[50%] -translate-x-1/2 w-[300px] h-[300px] bg-red-600/10 rounded-full blur-[60px] -z-10"></div>
                
                <!-- Navbar (Liquid Glass) -->
                <div class="absolute top-0 left-0 right-0 h-[100px] glass-panel z-40 border-t-0 border-x-0 rounded-b-none flex items-end pb-3 px-6">
                    <h1 class="text-[28px] font-bold text-white tracking-tight">Alex's Dashboard</h1>
                    <div class="w-8 h-8 rounded-full bg-white/10 ml-auto flex items-center justify-center text-[12px]">👤</div>
                </div>

                <div class="px-5 pt-[115px] pb-8 scrollable flex-1 flex flex-col gap-5">
                    
                    <!-- Critical Alert (Red tinted glass) -->
                    <div class="backdrop-blur-xl bg-[#FF3B30]/15 border border-[#FF3B30]/30 rounded-[24px] p-5 shadow-[0_10px_30px_rgba(255,59,48,0.15)]">
                        <div class="flex items-center gap-2 mb-2">
                            <div class="w-2 h-2 rounded-full bg-[#FF3B30] animate-pulse"></div>
                            <h3 class="text-[#FF3B30] font-bold text-[12px] tracking-widest uppercase">Ongoing Event</h3>
                        </div>
                        <p class="text-white font-medium text-[15px] leading-snug">Stress has been elevated for 5 minutes. They may need support.</p>
                    </div>

                    <!-- Pattern Review -->
                    <div>
                        <div class="flex justify-between items-end mb-2 ml-2 mr-2">
                            <h2 class="text-white/50 font-semibold text-[12px] uppercase tracking-wider">Pattern Review</h2>
                            <span class="bg-[#FF3B30] text-white text-[11px] font-bold px-2 py-0.5 rounded-full">1 Pending</span>
                        </div>
                        
                        <div class="glass-panel rounded-[24px] p-5 relative overflow-hidden" id="c-review-card">
                            <p class="text-white font-medium text-[15px] leading-snug mb-4">User reported feeling <span class="text-[#FF3B30] font-bold">Overwhelmed</span> at 3:30 PM. Add context to help BioCue learn.</p>
                            
                            <div class="bg-black/30 rounded-[16px] p-4 mb-5 border border-white/5">
                                <p class="text-white/40 text-[11px] font-semibold uppercase tracking-wider mb-3">Context Cues</p>
                                <div class="flex items-center gap-3 mb-2">
                                    <span class="text-[16px]">🔄</span>
                                    <span class="text-white/80 text-[14px]">Manually Triggered</span>
                                </div>
                                <div class="flex items-center gap-3">
                                    <span class="text-[16px]">☁️</span>
                                    <span class="text-white/80 text-[14px]">Synced via Cloud</span>
                                </div>
                            </div>

                            <p class="text-[#34C759] text-[14px] font-semibold mb-3">What was happening around Alex?</p>
                            
                            <div class="flex flex-col gap-2.5">
                                <button onclick="resolveReview(this)" class="w-full text-left py-3.5 px-4 rounded-[16px] flex items-center bg-[#FF3B30]/10 text-[#FF3B30] font-semibold text-[15px] transition active:scale-95 border border-[#FF3B30]/20">
                                    <span class="mr-3 text-lg">🔊</span> Loud Noises
                                </button>
                                <button onclick="resolveReview(this)" class="w-full text-left py-3.5 px-4 rounded-[16px] flex items-center bg-[#FF9500]/10 text-[#FF9500] font-semibold text-[15px] transition active:scale-95 border border-[#FF9500]/20">
                                    <span class="mr-3 text-lg">👥</span> Crowded Space
                                </button>
                                <button onclick="resolveReview(this)" class="w-full text-left py-3.5 px-4 rounded-[16px] flex items-center bg-[#34C759]/10 text-[#34C759] font-semibold text-[15px] transition active:scale-95 border border-[#34C759]/20">
                                    <span class="mr-3 text-lg">📋</span> Task Demand
                                </button>
                                <button onclick="resolveReview(this)" class="w-full text-left py-3.5 px-4 rounded-[16px] flex items-center bg-[#5AC8FA]/10 text-[#5AC8FA] font-semibold text-[15px] transition active:scale-95 border border-[#5AC8FA]/20">
                                    <span class="mr-3 text-lg">🔄</span> Routine Change
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Journal -->
                    <div>
                        <div class="flex justify-between items-end mb-2 ml-2 mr-2">
                            <h2 class="text-white/50 font-semibold text-[12px] uppercase tracking-wider">Journal</h2>
                            <div class="flex items-center gap-1 text-[#5AC8FA] text-[13px] font-medium">
                                <span>Today</span>
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
                            </div>
                        </div>
                        
                        <div class="glass-panel rounded-[24px] p-5">
                            <div class="flex items-center gap-4">
                                <div class="w-10 h-10 rounded-full bg-[#FF3B30]/20 flex items-center justify-center text-xl shrink-0">🌩️</div>
                                <div class="flex-1">
                                    <p class="text-white font-semibold text-[15px]">Loud Noises</p>
                                    <p class="text-white/50 text-[13px]">Manual Trigger • Overwhelmed</p>
                                </div>
                                <span class="text-white/40 text-[13px] font-medium">08:30</span>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>

    </div>

    <script>
        // Watch Logic
        function setWatch(state, color = '', text = '', icon = '') {
            ['w-anchor', 'w-carousel', 'w-ongoing', 'w-success'].forEach(id => {
                const el = document.getElementById(id);
                el.style.opacity = '0';
                setTimeout(() => {
                    el.classList.add('hidden');
                    
                    if (id === 'w-' + state.toLowerCase()) {
                        el.classList.remove('hidden');
                        setTimeout(() => { el.style.opacity = '1'; }, 50);
                    }
                }, 300);
            });

            if (state === 'ONGOING') {
                document.getElementById('w-pulse-1').style.borderColor = color;
                document.getElementById('w-pulse-bg').style.backgroundColor = color;
                document.getElementById('w-pulse-core').style.backgroundColor = color;
                document.getElementById('w-ongoing-icon').innerText = icon;
                document.getElementById('w-ongoing-label').innerText = text;
                document.getElementById('w-ongoing-label').style.color = color;
            }

            if (state === 'SUCCESS') {
                setTimeout(() => setWatch('ANCHOR'), 2000);
            }
        }

        // Caregiver Interaction
        function resolveReview(btn) {
            btn.style.opacity = '0.5';
            btn.style.transform = 'scale(0.95)';
            
            setTimeout(() => {
                const card = document.getElementById('c-review-card');
                card.style.transition = 'all 0.5s cubic-bezier(0.25, 1, 0.5, 1)';
                card.style.opacity = '0';
                card.style.transform = 'translateY(10px)';
                setTimeout(() => card.style.display = 'none', 500);
            }, 400);
        }
    </script>
</body>
</html>
"""

with open('.agents/SPEC/PROTOTYPE.html', 'w') as f:
    f.write(html)
print("Redesigned PROTOTYPE.html using Apple Design HIG.")
