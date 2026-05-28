package it.unibo.util.gpx

import it.unibo.util.ais.AisPayload
import java.io.File

/** Utility to generate GPX files from AIS Payload. **/
object GpxFormatter {
    /**
     * @param aisData the list of [AisPayload] that will be converted into GPX traces.
     * @param outputFolder the folder in which GPX traces will be stored.
     */
    fun createGpxFileFromAisData(
        aisData: List<AisPayload>,
        outputFolder: File,
    ) {
        val gpxHeader =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="AIS-to-GPX Converter" xmlns="http://www.topografix.com/GPX/1/1">
            """.trimIndent()

        val gpxFooter = "</gpx>"

        // Group AIS data by boatId
        val groupedData = aisData.groupBy { it.boatId }

        groupedData.toSortedMap().values.forEachIndexed { index, points ->
            val boatId = "anon${(index + 1).toString().padStart(4, '0')}"
            val tracks =
                """
                <trk>
                    <name>$boatId</name>
                    <trkseg>
                        ${points.sortedBy { it.timestamp }.joinToString("\n") { point ->
                    val extensions =
                        point.gpxExtensions().takeIf { it.isNotEmpty() }?.let {
                            """
                            <extensions>
                            $it
                            </extensions>
                            """.trimIndent()
                        }.orEmpty()
                    """
                    <trkpt lat="${point.latitude}" lon="${point.longitude}">
                        <ele>0</ele>
                        <time>${point.timestamp}</time>
                        $extensions
                    </trkpt>
                    """.trimIndent()
                }}
                    </trkseg>
                </trk>
                """.trimIndent()

            // Construct output file gpx
            val outputGpx = "$gpxHeader\n$tracks\n$gpxFooter"

            // Create file for boatID
            val file = File("${outputFolder.path}/$boatId.gpx")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(outputGpx)
        }
    }

    private fun AisPayload.gpxExtensions(): String =
        listOfNotNull(
            speedOverGround?.let { "    <sog>$it</sog>" },
            courseOverGround?.let { "    <cog>$it</cog>" },
        ).joinToString("\n")
}
