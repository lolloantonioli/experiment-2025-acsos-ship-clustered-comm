package it.unibo.util.gpx

import dk.dma.ais.message.AisMessage
import it.unibo.util.ais.AisDecoder
import it.unibo.util.ais.AisPayload
import java.io.File
import java.time.Instant
import java.util.regex.Pattern

/**
 * Utility Script that parses the raw AIS data (not provided for privacy in this repository).
 */
object ParseRawNavigationData {
    /**
     * @param inputFolder the folder containing AIS raw messages.
     * @param day the name of the file used to filter raw messages to parse.
     * @return a list of [File].
     */
    fun dataToParse(
        inputFolder: String,
        day: String,
    ): List<File> =
        File(inputFolder)
            .listFiles { file ->
                Pattern.compile("$day.+").matcher(file.name).matches()
            }.ifEmpty {
                throw IllegalStateException("No suitable file found")
            }.toList()

    /**
     * Parses all the files contained inside inputFolder into [AisPayload] and then stores them in outputFolder as
     * GPX traces.
     * @param inputFolder the path of the input folder with raw data.
     * @param outputFolder the path of the output folder where to put parsed info.
     * @param day the file name used to filter raw files inside inputFolder.
     */
    fun parse(
        inputFolder: String,
        outputFolder: String,
        day: String,
    ) {
        val aisMessages: MutableMap<Instant, AisMessage> = mutableMapOf()
        dataToParse(inputFolder, day).forEach {
            aisMessages.putAll(AisDecoder.parseFile(it))
        }
        val aisPayloads =
            AisPayload
                .from(aisMessages)
                .filterNot { it.boatId == ID_TO_DELETE }
        println("Writing to: $outputFolder")
        val destinationFolder = File(outputFolder)
        destinationFolder.mkdirs()
        GpxFormatter.createGpxFileFromAisData(aisPayloads, destinationFolder)
    }

    /** This boat is located in Russia somehow, probably an error in the data. Deleting it from dataset.**/
    const val ID_TO_DELETE = 235818393

    /** Kotlin script to generate GPX traces from AIS raw files. **/
    @JvmStatic
    fun main(vararg args: String) {
        val inputFolder = args[0]
        val outputFolder = args[1]
        val day = args[2]
        println("Parsing raw data from: $inputFolder")
        ParseRawNavigationData.parse(inputFolder, outputFolder, day)
    }
}
