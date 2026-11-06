package com.mtpnda.biocue.core.domain

/**
 * Spec 03: the watch's synthetic-data "Test Mode" is a toggle that is OFF by default and exists in debug builds only,
 * so a release build can never feed invented readings into a system that alerts caregivers.
 */
object TestModePolicy {
    fun effective(isDebugBuild: Boolean, requested: Boolean): Boolean = isDebugBuild && requested
}
