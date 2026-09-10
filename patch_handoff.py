import re

with open('.agents/SPEC/PROTOTYPE.html', 'r') as f:
    html_content = f.read()

with open('.agents/SPEC/04_USER_INTERFACE.md', 'r') as f:
    spec_content = f.read()

handoff_content = f"""# BioCue V3 - Claude Design Handoff

Hello! Please use this document as context for Claude Design to ideate "V2" of the BioCue application.

## 1. Context & Constraints
BioCue is an Android/Wear OS application. 
- The **Wear OS App (Wearer)** is used by a neurodivergent individual. It must be "zero-anxiety" (no raw numbers, no complex animations, extremely simple).
- The **Android Phone App (Caregiver)** is a dashboard used by a caregiver to monitor the wearer, review past stress events, and label patterns to train a machine learning model.
- The **Android Phone App (Wearer Companion)** is a companion app for the wearer's phone, primarily for ambient support (like playing calming audio) and holding developer stubs.

## 2. Current HTML Prototype (1:1 Baseline)
Below is the exact `PROTOTYPE.html` file that perfectly mimics the current Android Jetpack Compose code. Use this as your structural and styling baseline.

```html
{html_content}
```

## 3. UI Specification (from .agents/SPEC/04_USER_INTERFACE.md)
```markdown
{spec_content}
```
"""

with open('/Users/valdiearsanur/.gemini/antigravity/brain/b4b615bf-942b-409f-84fc-fdf1d7ed20f8/claude_design_handoff.md', 'w') as f:
    f.write(handoff_content)

print("Updated handoff artifact.")
