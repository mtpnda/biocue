import re

with open('mobile/src/main/java/com/mtpnda/biocue/ui/screens/wearer/WearerDashboardScreen.kt', 'r') as f:
    c = f.read()

c = c.replace("Column(\n                                .fillMaxWidth(),", "Column(\n                                modifier = Modifier.fillMaxWidth(),")
c = c.replace(",\n                            .fillMaxWidth(),", ",\n                            modifier = Modifier.fillMaxWidth(),")
c = c.replace("Card(.fillMaxWidth(),", "Card(modifier = Modifier.fillMaxWidth(),")
c = c.replace(".padding(bottom = 8.dp)", "modifier = Modifier.padding(bottom = 8.dp)")

with open('mobile/src/main/java/com/mtpnda/biocue/ui/screens/wearer/WearerDashboardScreen.kt', 'w') as f:
    f.write(c)

print("Fixed implicit modifiers.")
