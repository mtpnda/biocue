with open('mobile/src/main/java/com/mtpnda/biocue/ui/Previews.kt', 'r') as f:
    content = f.read()

bad_caregiver = """onOpenProfile = {},
            pairingCode = "123456",
            timeRemainingSeconds = 600,
            connectedCaregivers = emptyList(),
            onGeneratePairingCode = {},
            onRemoveCaregiver = {}
        )
    }
}"""

good_caregiver = """onOpenProfile = {}
        )
    }
}"""

content = content.replace(bad_caregiver, good_caregiver)

with open('mobile/src/main/java/com/mtpnda/biocue/ui/Previews.kt', 'w') as f:
    f.write(content)
