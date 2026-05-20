package it.unibo.utils

import it.unibo.util.ais.AisCustomMessageParser
import it.unibo.util.ais.AisDecoder
import it.unibo.util.ais.AisPayload
import it.unibo.util.gpx.GpxFormatter
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestAisParser {
    val dataToParse =
        File(
            object {}
                .javaClass.classLoader
                .getResource("ais-sample/20250506-130000.nmea.txt")
                ?.toURI()
                ?: throw IllegalArgumentException("Missing sample file in project resources"),
        )

    @Test
    fun testParseFile() {
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        println(aisMessage)
    }

    @Test
    fun testConversionToAisPayload() {
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        println(aisMessage)
        val aisPayloads = AisPayload.Companion.from(aisMessage)
        assertTrue(aisPayloads.isNotEmpty())
        assertTrue(aisPayloads.any { it.speedOverGround != null })
        assertTrue(aisPayloads.any { it.courseOverGround != null })
        println(aisPayloads)
    }

    @Test
    fun testSogAndCogConversion() {
        val customMessageParser = AisCustomMessageParser()
        val example = "!AIVDM,1,1,,A,14eGrSPP00ncMJTO5C6aBwvP2D0?,0*7A"
        customMessageParser.parseLine(example)
        val output = customMessageParser.build()
        val payload = AisPayload.from(
            316013198,
            java.time.Instant.EPOCH,
            requireNotNull(output)
        )
        requireNotNull(payload)
        assertEquals(0.0, payload.speedOverGround)
        assertEquals(237.9, payload.courseOverGround)
    }

    @Test
    fun testDeriveNumberOfBoats() {
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        val aisPayloads = AisPayload.Companion.from(aisMessage)
        val groupedByBoat = aisPayloads.groupBy { it.boatId }
        println(groupedByBoat.count())
        println(groupedByBoat)
    }

    @Test
    fun testGpxGenerationFromAisData() {
        val aisMessage = AisDecoder.parseFile(dataToParse)
        assertTrue(aisMessage.isNotEmpty())
        val xmlPath = Files.createTempDirectory("ais-gpx-test").toFile()
        val aisPayloads = AisPayload.Companion.from(aisMessage)
        val payload =
            aisPayloads.first {
                it.speedOverGround != null && it.courseOverGround != null
            }
        GpxFormatter.createGpxFileFromAisData(listOf(payload), xmlPath)
        val gpx = File(xmlPath, "${payload.boatId}.gpx").readText()
        assertTrue("<sog>${payload.speedOverGround}</sog>" in gpx)
        assertTrue("<cog>${payload.courseOverGround}</cog>" in gpx)
    }

    @Test
    fun testSingleLineParse() {
        val customMessageParser = AisCustomMessageParser()
        val example = "!AIVDM,1,1,,A,14eGrSPP00ncMJTO5C6aBwvP2D0?,0*7A"
        customMessageParser.parseLine(example)
        val output = customMessageParser.build()
        println(output)
        assertEquals(
            "[msgId=1, repeat=0, userId=316013198, cog=2379, navStatus=0, " +
                "pos=(32592666,190245714) = (32592666,-78189742), posAcc=1, raim=1, " +
                "specialManIndicator=0, rot=128, sog=0, spare=0, syncState=0, " +
                "trueHeading=511, utcSec=16, slotTimeout=5, subMessage=15]",
            output.toString(),
        )
    }
}
