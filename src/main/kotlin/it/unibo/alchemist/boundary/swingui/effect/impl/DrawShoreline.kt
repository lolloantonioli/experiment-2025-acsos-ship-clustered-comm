package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.maps.maps.environments.NavigationEnvironment
import java.awt.Color
import java.awt.Graphics2D

/**
 * Graphical representation in a bidimensional canvas of the coastal borders (shoreline) stored in GeoJSON file.
 * The shoreline is represented with a blue line in the graphical interface.
 *
 * The object is deprecated because Alchemist deprecated its Swing GUI package,
 * however, no alternative is available to date, so this customization inherits deprecation.
 */
@Suppress("DEPRECATION")
class DrawShoreline : AbstractDrawOnce() {
    override fun getColorSummary(): Color = Color.BLUE

    override fun <T : Any?, P : Position2D<P>> draw(
        graphics2D: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>,
    ) {
        DrawGeoJSONInformation.draw(
            (environment as NavigationEnvironment<T>).getGeoJsonObjectsForShoreline(),
            colorSummary,
            graphics2D,
            wormhole as Wormhole2D<GeoPosition>,
        )
    }
}
