import re

with open('.agents/SPEC/PROTOTYPE.html', 'r') as f:
    content = f.read()

script = """
    <script>
        // Make Pattern Review buttons interactive
        document.addEventListener('DOMContentLoaded', () => {
            const buttonGroups = [
                // Group 1: The First Event buttons
                document.querySelectorAll('.border-b.border-gray-700.pb-3.mb-3 .grid button'),
                // Group 2: The Second Event buttons
                document.querySelectorAll('.flex.flex-wrap.gap-2 button')
            ];

            buttonGroups.forEach(group => {
                group.forEach(btn => {
                    btn.addEventListener('click', () => {
                        // Reset all in this group
                        group.forEach(b => {
                            b.classList.remove('bg-[#0EA5E9]', 'border-[#0EA5E9]');
                            b.classList.add('bg-[#334155]', 'border-gray-600');
                        });
                        // Highlight clicked
                        btn.classList.remove('bg-[#334155]', 'border-gray-600');
                        btn.classList.add('bg-[#0EA5E9]', 'border-[#0EA5E9]');
                        
                        // Fake a resolve animation after a short delay
                        setTimeout(() => {
                            const parentCard = btn.closest('.mb-3') || btn.closest('div:has(.flex-wrap)');
                            if (parentCard) {
                                parentCard.style.transition = 'all 0.5s ease';
                                parentCard.style.opacity = '0';
                                setTimeout(() => parentCard.style.display = 'none', 500);
                            }
                        }, 600);
                    });
                });
            });
        });
    </script>
</body>
"""

content = content.replace("</body>", script)

with open('.agents/SPEC/PROTOTYPE.html', 'w') as f:
    f.write(content)

print("Patched HTML successfully.")
