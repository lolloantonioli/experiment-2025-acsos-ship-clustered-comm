package it.unibo.util.ais

import dk.dma.ais.message.AisMessage
import java.io.File
import java.time.Instant

/** Utility object to decode AIS raw messages **/
object AisDecoder {
    /** @return the message parsed from a raw [String] to [AisMessage] and maps it to
     * the timestamp of the raw message.
     **/
    fun parsePayload(
        payload: String,
        date: String,
    ): Map<Instant, AisMessage> {
        val aisMessageBuilder = AisCustomMessageParser()
        val payloadDecoded: MutableMap<Instant, AisMessage> = mutableMapOf()
        payload.lines().forEach {
            if (it.startsWith("!DATE-TIME")) {
                // Timestamp - NOT STANDARD AIS NMEA 0183
                val time = it.substringAfter("!DATE-TIME,").trim()
                // println("${date}T${time}Z")
                val currentTimestamp = Instant.parse("${date}T${time}Z")
                if (aisMessageBuilder.isComplete()) {
                    val aisMessage = aisMessageBuilder.build()
                    if (aisMessage != null) payloadDecoded[currentTimestamp] = aisMessage
                }
            } else if (it != "") {
                // Payload
                aisMessageBuilder.parseLine(it)
                // println(it)
            }
        }
        return payloadDecoded
    }

    /** Parses all the raw AIS lines contained in a [File].
     * @param file the [File] from which parse AIS info.
     **/
    fun parseFile(file: File): Map<Instant, AisMessage> {
        val dateLong = file.name.substringAfterLast("/").substringBefore("-")
        val year = dateLong.take(4)
        val month = dateLong.drop(4).take(2)
        val day = dateLong.takeLast(2)
        val date = "$year-$month-$day"
        return parsePayload(file.readText(Charsets.UTF_8), date)
    }
}
