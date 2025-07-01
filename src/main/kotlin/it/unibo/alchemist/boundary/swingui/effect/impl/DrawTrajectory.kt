package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.swingui.effect.api.Effect
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point

/**
 * Graphical effect to highlight the trajectory performed by ships in the simulated environment.
 *
 * The object is deprecated because Alchemist deprecated its Swing GUI package,
 * however, no alternative is available to date, so this customization inherits deprecation.
 */
@Suppress("DEPRECATION")
class DrawTrajectory : Effect {
    @Transient
    private var positionsMemory: Map<Int, List<Point>> = mapOf()

    @Transient
    private var lastDrawMemory: Map<Int, Int> = emptyMap()

    override fun getColorSummary(): Color = Color.BLACK

    override fun <T : Any?, P : Position2D<P>> apply(
        g: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>,
    ) {
        // Actual position
        val nodePosition: P = environment.getPosition(node)
        val viewPoint: Point = wormhole.getViewPoint(nodePosition)

        drawTrajectory(g, node)

        updateTrajectory(node, environment, viewPoint)
    }

    private fun <P : Position2D<P>> drawTrajectory(
        graphics2D: Graphics2D,
        node: Node<*>,
    ) {
        val positions = positionsMemory[node.id].orEmpty()
        val color = computeColorOrBlack(node)
        positions.takeLast(DEFAULT_SNAPSHOT_LENGTH).forEach {
            graphics2D.color = color
            graphics2D.fillOval(it.x - 2, it.y - 2, 4, 4)
        }
    }

    private fun <P : Position2D<P>, T> updateTrajectory(
        node: Node<T>,
        environment: Environment<T, P>,
        actualPosition: Point,
    ) {
        val positions = positionsMemory[node.id].orEmpty()
        val lastDraw = lastDrawMemory[node.id] ?: 0
        val roundedTime =
            environment.simulation.time
                .toDouble()
                .toInt()
        if (roundedTime >= lastDraw) {
            lastDrawMemory = lastDrawMemory + (node.id to lastDraw + DEFAULT_TIMESPAN)
            val updatedPositions =
                (positions + actualPosition)
            positionsMemory = positionsMemory +
                (node.id to updatedPositions)
        }
    }

    private fun computeColorOrBlack(node: Node<*>): Color =
        node
            .id
            .toFloat()
            .let {
                val hue = (it % MAX_COLOR) * 360f / MAX_COLOR // Convert to hue (0-360 degrees)
                return Color.getHSBColor(hue.toFloat(), 1f, 1f) // Full saturation and brightness
            }

    /** Static utilities for drawing the trajectory. **/
    companion object {
        /**
         * Maximum value for color gradient.
         */
        private const val MAX_COLOR: Double = 255.0

        /**
         * Length of the represented trajectory in the graphical interface.
         */
        private const val DEFAULT_SNAPSHOT_LENGTH: Int = 140

        /**
         * Memory length of past positions.
         */
        private const val DEFAULT_TIMESPAN: Int = 100
    }
}
