package it.unibo.util.geojson

import io.data2viz.geojson.jackson.LngLatAlt
import io.data2viz.geojson.jackson.MultiPolygon
import io.data2viz.geojson.jackson.Polygon
import it.unibo.alchemist.model.maps.positions.LatLongPosition
import java.awt.geom.Point2D
import kotlin.math.cos
import kotlin.math.sin

/** Earth Radius constant value. **/
const val EARTH_RADIUS = 6371000L

/** Conversion from [LngLatAlt] to [Point2D] cartesian positions. **/
fun LngLatAlt.toCartesian() =
    Point2D.Double(
        EARTH_RADIUS * cos(this.latitude) * cos(longitude),
        EARTH_RADIUS * cos(this.latitude) * sin(longitude),
    )

/** Converts a [MultiPolygon] into a [List] of [Polygon]. **/
fun MultiPolygon.asListOfPoligon(): List<Polygon> {
    if (this.coordinates.size > 1) {
        error("More than one element in this array has no sense for this experiment")
    }
    return this.coordinates.first().map {
        Polygon(it)
    }
}

/** Converts a [LatLongPosition] into a [LngLatAlt]. **/
fun LatLongPosition.toLngLatAlt() = LngLatAlt(longitude, latitude)

/** Converts a [LngLatAlt] into a [LatLongPosition]. **/
fun LngLatAlt.toLatLongPosition() = LatLongPosition(latitude, longitude)

/** @return true if this [LngLatAlt] is inside of the provided [Polygon].
 * @param polygon the [Polygon].
 */
fun LngLatAlt.insideOf(polygon: Polygon): Boolean {
    if (polygon.coordinates.size > 1) {
        error("More than one element in this array has no sense for this experiment")
    }
    val pol = polygon.coordinates.first()

    if (pol.contains(this)) {
        // Point of Polygon consider to be inside
        return true
    }

    var i = 0
    var j = pol.size - 1
    var result = false

    while (i < pol.size) {
        if (pol[i].latitude > this.latitude != pol[j].latitude > this.latitude &&
            this.longitude < (pol[j].longitude - pol[i].longitude) * (this.latitude - pol[i].latitude) /
            (pol[j].latitude - pol[i].latitude) + pol[i].longitude
        ) {
            result = !result
        }
        j = i++
    }
    return result
}
