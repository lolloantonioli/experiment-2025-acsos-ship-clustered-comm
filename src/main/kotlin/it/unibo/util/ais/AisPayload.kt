package it.unibo.util.ais

import dk.dma.ais.message.AisMessage
import dk.dma.ais.message.IPositionMessage
import dk.dma.ais.message.IVesselPositionMessage
import it.unibo.util.gpx.ParsingUtils
import java.time.Instant

/** Describes the subset of AIS information taken from raw messages relevant for this simulation.
 * @param boatId the unique identifier of the boat.
 * @param timestamp the timestamp related to the receipt of the message.
 * @param longitude the longitude of the boat.
 * @param latitude the latitude of the boat.
 * @param speedOverGround the speed of the boat.
 * @param courseOverGround the direction of the boat.
 */
data class AisPayload(
    val boatId: Int,
    val timestamp: Instant,
    val longitude: Double,
    val latitude: Double,
    val speedOverGround: Double?,
    val courseOverGround: Double?,
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
                // Se messaggio non contiene sog e cog, questi diventano null
                val vesselPositionMessage = aisMessage as? IVesselPositionMessage
                AisPayload(
                    boatId,
                    timestamp,
                    aisMessage.pos.longitudeDouble,
                    aisMessage.pos.latitudeDouble,
                    if (vesselPositionMessage?.isSogValid == true) vesselPositionMessage.sog / 10.0 else null,
                    if (vesselPositionMessage?.isCogValid == true) vesselPositionMessage.cog / 10.0 else null,
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
