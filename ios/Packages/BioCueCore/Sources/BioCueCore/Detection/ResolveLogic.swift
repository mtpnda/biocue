import Foundation

/// Port of Kotlin `ResolveLogic`: the two-stage review flow.
///  Stage 1  an unlabelled event gets an emotion. OVERWHELMED continues to stage 2; anything else is done.
///  Stage 2  a context ("Loud Noises", …) is appended to the label and the event is resolved.
public enum ResolveLogic {
    public static func processResolution(currentLabel: String, newLabelOrContext: String) -> (label: String, resolved: Bool) {
        if currentLabel == EventTypes.labelNone {
            if newLabelOrContext == EventTypes.labelOverwhelmed {
                return (newLabelOrContext, false)
            } else {
                return (newLabelOrContext, true)
            }
        }
        return ("\(currentLabel) - \(newLabelOrContext)", true)
    }
}
