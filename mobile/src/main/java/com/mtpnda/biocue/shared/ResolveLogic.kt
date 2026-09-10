package com.mtpnda.biocue.shared

import com.mtpnda.biocue.core.domain.model.EventTypes

object ResolveLogic {
    fun processResolution(
        currentLabel: String,
        newLabelOrContext: String
    ): Pair<String, Boolean> {
        // Stage 1: Initial emotion labeling
        if (currentLabel == EventTypes.LABEL_NONE) {
            if (newLabelOrContext == EventTypes.LABEL_OVERWHELMED) {
                // It's a meltdown, move to Stage 2 (context)
                return Pair(newLabelOrContext, false)
            } else {
                // Not a meltdown, or unsure, we are done
                return Pair(newLabelOrContext, true)
            }
        }
        
        // Stage 2: Context labeling (or if it was manually triggered as OVERWHELMED)
        return Pair("$currentLabel - $newLabelOrContext", true)
    }
}
