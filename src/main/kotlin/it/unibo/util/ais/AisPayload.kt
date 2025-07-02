package it.unibo.util.ais

import dk.dma.ais.message.AisMessage
import dk.dma.ais.message.IPositionMessage
import it.unibo.util.gpx.ParsingUtils
import java.time.Instant

/** Describes the subset of AIS information taken from raw messages relevant for this simulation.
 * @param boatId the unique identifier of the boat.
 * @param timestamp the timestamp related to the receipt of the message.
 * @param longitude the longitude of the boat.
 * @param latitude the latitude of the boat.
 */
data class AisPayload(
    val boatId: Int,
    val timestamp: Instant,
    val longitude: Double,
    val latitude: Double,
) {
    /**
     * Static factory for [AisPayload].
     */
    companion object {
        /**
         * Creates an [AisPayload] object from an [AisMessage].
         * @param boatId the unique identifier of the boat.
         * @param timestamp the timestamp related to the receipt of the message.
         * @param aisMessage the [AisMessage] from which information is taken.
         * @return an [AisPayload] where possible, or null
         */
        fun from(
            boatId: Int,
            timestamp: Instant,
            aisMessage: AisMessage,
        ): AisPayload? =
            if (
                aisMessage is IPositionMessage &&
                ParsingUtils.validateLongitude(aisMessage.pos.longitudeDouble) &&
                ParsingUtils.validateLatitude(aisMessage.pos.latitudeDouble)
            ) {
                AisPayload(
                    boatId,
                    timestamp,
                    aisMessage.pos.longitudeDouble,
                    aisMessage.pos.latitudeDouble,
                )
            } else {
                null
            }

        /**
         * Creates an [AisPayload] object from Map of [AisMessage] and corresponding timestamps.
         * @param map a [Map] of [Instant] and related [AisMessage].
         * @return a [List] of [AisPayload]
         */
        fun from(map: Map<Instant, AisMessage>): List<AisPayload> =
            map
                .map {
                    from(it.value.userId, it.key, it.value)
                }.filterNotNull()
    }
}
