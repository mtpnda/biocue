package com.mtpnda.biocue.core.link

/**
 * What the watch puts in its Bluetooth advertisement. Detecting a BioCue watch implies something sensitive about its
 * wearer, so the service id is visible only while the wearer is deliberately pairing. Once bonded, iOS reconnects to
 * the remembered watch by identity and does not need to see the service id.
 */
object AdvertisePolicy {
    fun includeServiceUuid(pairingWindowOpen: Boolean): Boolean = pairingWindowOpen
}
