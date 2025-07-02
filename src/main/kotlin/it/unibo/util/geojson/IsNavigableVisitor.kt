package it.unibo.util.geojson

import io.data2viz.geojson.jackson.Feature
import io.data2viz.geojson.jackson.FeatureCollection
import io.data2viz.geojson.jackson.GeoJsonObjectVisitor
import io.data2viz.geojson.jackson.GeometryCollection
import io.data2viz.geojson.jackson.LineString
import io.data2viz.geojson.jackson.LngLatAlt
import io.data2viz.geojson.jackson.MultiLineString
import io.data2viz.geojson.jackson.MultiPoint
import io.data2viz.geojson.jackson.MultiPolygon
import io.data2viz.geojson.jackson.Point
import io.data2viz.geojson.jackson.Polygon

/**
 * Visitor of a GeoJSON object that determines whether the target position is navigable or not.
 * @param targetPosition the target position in.
 */
class IsNavigableVisitor(
    private val targetPosition: LngLatAlt,
) : GeoJsonObjectVisitor<Boolean> {
    override fun visit(geoJsonObject: Feature): Boolean =
        if (geoJsonObject.geometry != null) {
            geoJsonObject.geometry!!.accept(this)
        } else {
            false
        }

    override fun visit(geoJsonObject: FeatureCollection): Boolean =
        geoJsonObject
            .getFeatures()
            .map {
                it.accept(this)
            }.all { it }

    override fun visit(geoJsonObject: GeometryCollection): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: LineString): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: MultiLineString): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: MultiPoint): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: MultiPolygon): Boolean =
        geoJsonObject
            .asListOfPoligon()
            .map {
                it.accept(this)
            }.all { it }

    override fun visit(geoJsonObject: Point): Boolean = TODO("Not yet implemented")

    override fun visit(geoJsonObject: Polygon): Boolean = !targetPosition.insideOf(geoJsonObject)
}
