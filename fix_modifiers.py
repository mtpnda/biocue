import re

with open('mobile/src/main/java/com/mtpnda/biocue/ui/screens/wearer/WearerDashboardScreen.kt', 'r') as f:
    c = f.read()

c = c.replace("Card(\n                    .fillMaxWidth(),", "Card(\n                    modifier = Modifier.fillMaxWidth(),")
c = c.replace("Box(.weight(1f))", "Box(modifier = Modifier.weight(1f))")

with open('mobile/src/main/java/com/mtpnda/biocue/ui/screens/wearer/WearerDashboardScreen.kt', 'w') as f:
    f.write(c)

print("Fixed modifiers.")
