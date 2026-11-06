package com.mtpnda.biocue.shared

/**
 * Kept so existing callers and the parity-fixture generator keep working. The logic now lives in
 * core (`com.mtpnda.biocue.core.domain.ResolveLogic`) so the core module can use it.
 */
object ResolveLogic {
    fun processResolution(currentLabel: String, newLabelOrContext: String): Pair<String, Boolean> =
        com.mtpnda.biocue.core.domain.ResolveLogic.processResolution(currentLabel, newLabelOrContext)
}
