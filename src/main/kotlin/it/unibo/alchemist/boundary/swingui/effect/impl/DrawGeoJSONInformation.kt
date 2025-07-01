package it.unibo.alchemist.boundary.swingui.effect.impl

import io.data2viz.geojson.JacksonGeoJsonObject
import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.GeoPosition
import it.unibo.util.geojson.DrawPoligonVisitor
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D

/**
 * Utility object to handle GeoJson information and represent them in a bi-dimensional graphical interface.
 */
object DrawGeoJSONInformation {
    /**
     * Utility function that draws in a Swing bi-dimensional canvas
     * the layers specified in a GeoJson file.
     * @param layersToRepresent the [List] of [JacksonGeoJsonObject] to represent
     * @param color the [Color] which will be used in the canvas
     * @param graphics2D the bi-dimensional canvas on which represent GeoJson layers
     * @param wormhole a [Wormhole2D]
     */
    fun draw(
        layersToRepresent: List<JacksonGeoJsonObject>,
        color: Color,
        graphics2D: Graphics2D,
        wormhole: Wormhole2D<GeoPosition>,
    ) {
        graphics2D.color = color
        graphics2D.stroke = BasicStroke(2f)
        layersToRepresent.forEach { it.accept(DrawPoligonVisitor(graphics2D, wormhole)) }
    }
}
