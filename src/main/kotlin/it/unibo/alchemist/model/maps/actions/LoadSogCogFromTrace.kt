package it.unibo.alchemist.model.maps.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import it.unibo.alchemist.model.molecules.SimpleMolecule
import java.io.File
import java.time.Duration
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Loads speed over ground and course over ground from the node's GPX trace.
 */
class LoadSogCogFromTrace<T>(
    private val environment: NavigationEnvironment<T>,
    node: Node<T>,
    private val path: String,
    referenceTimeEpochSeconds: Number,
) : AbstractAction<T>(node) {
    private val startTime = Instant.ofEpochSecond(referenceTimeEpochSeconds.toLong())
    private val directory = javaClass.classLoader.getResource(path)?.let { File(it.toURI()) } ?: File(path)
    private val files = requireNotNull(directory.listFiles()) { "Trace path '$path' is not a directory" }
        .filter { it.extension.equals("gpx", ignoreCase = true) }
        .sortedBy { it.nameWithoutExtension }
    private val points by lazy {
        val nodeIndex = environment.nodes.indexOf(node)
        require(nodeIndex in files.indices) {
            "Cannot match node ${node.id} to a GPX trace in '$path': node index is $nodeIndex, traces are ${files.size}"
        }
        readPoints(files[nodeIndex])
    }

    override fun cloneAction(
        p0: Node<T>?,
        p1: Reaction<T>?,
    ): Action<T> = LoadSogCogFromTrace(environment, p0!!, path, startTime.epochSecond,)

    override fun execute() {
        val time = environment.simulation.time.toDouble()
        val point = points.lastOrNull { it.time <= time } ?: points.firstOrNull()
        point?.sog?.takeUnless(Double::isNaN)?.let { set("sog", it) }
        point?.cog?.takeUnless(Double::isNaN)?.let { set("cog", it) }
    }

    override fun getContext(): Context = Context.LOCAL

    private fun set(
        name: String,
        value: Double,
    ) {
        @Suppress("UNCHECKED_CAST")
        node.setConcentration(SimpleMolecule(name), value as T)
    }

    private fun readPoints(file: File): List<Point> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(file)
        val xmlPoints = document.getElementsByTagNameNS("*", "trkpt")
        return (0 until xmlPoints.length)
            .map { xmlPoints.item(it) as Element }
            .mapNotNull {
                Point(
                    time = it.text("time")
                        ?.let(Instant::parse)
                        ?.let { time -> Duration.between(startTime, time).toMillis() / 1_000.0 }
                        ?: return@mapNotNull null,
                    sog = it.text("sog")?.toDoubleOrNull() ?: Double.NaN,
                    cog = it.text("cog")?.toDoubleOrNull() ?: Double.NaN,
                )
            }
            .filter { it.time >= 0.0 }
            .sortedBy { it.time }
    }

    private fun Element.text(localName: String): String? =
        getElementsByTagNameNS("*", localName)
            .item(0)
            ?.textContent
            ?.trim()

    private data class Point(
        val time: Double,
        val sog: Double,
        val cog: Double,
    )
}
