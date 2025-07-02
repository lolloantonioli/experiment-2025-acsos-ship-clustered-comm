package it.unibo.alchemist.model.maps.maps.environments

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.data2viz.geojson.JacksonGeoJsonObject
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Incarnation
import it.unibo.alchemist.model.maps.MapEnvironment
import it.unibo.alchemist.model.maps.environments.OSMEnvironment
import it.unibo.alchemist.model.maps.positions.LatLongPosition
import it.unibo.alchemist.model.maps.routingservices.GraphHopperOptions
import it.unibo.alchemist.model.maps.routingservices.GraphHopperRoutingService
import it.unibo.util.geojson.IsNavigableVisitor
import it.unibo.util.geojson.toLngLatAlt
import java.io.File

/**
 * Features of the Alchemist Simulator to hold navigation information from GeoJSON files.
 * @param incarnation
 * @param shorelineFiles a [List] of GeoJSON files containing shoreline description.
 * @param routesFiles a [List] of GeoJSON files containing the navigation routes description.
 */
class NavigationEnvironment<T>(
    incarnation: Incarnation<T, GeoPosition>,
    shorelineFiles: List<String>,
    routesFiles: List<String>,
) : MapEnvironment<T, GraphHopperOptions, GraphHopperRoutingService> by OSMEnvironment(incarnation) {
    /**
     * Creates an instance of [NavigationEnvironment].
     * @param incarnation
     * @param shorelineFiles a [List] of GeoJSON files containing shoreline description.
     */
    constructor(incarnation: Incarnation<T, GeoPosition>, shorelineFiles: List<String>) :
        this(incarnation, shorelineFiles, emptyList())

    // Loading
    private val geoJsonObjectsForShoreline: List<JacksonGeoJsonObject> = shorelineFiles.map { deserializeGeoJSON(it) }
    private val geoJsonObjectsForRoutes: List<JacksonGeoJsonObject> = routesFiles.map { deserializeGeoJSON(it) }

    private fun deserializeGeoJSON(path: String): JacksonGeoJsonObject {
        val file =
            this::class.java.classLoader
                .getResource(path)
                ?.toURI()
                ?.let { File(it) }
        require(file != null) { "No resource $path exist" }

        val fileExtension = file.path.split(".")[1]
        if (fileExtension != "geojson") {
            error("$fileExtension is not recognized format for this environment: 'geojson' is needed.")
        }

        // Deserialize
        val customMapper =
            ObjectMapper()
                .registerKotlinModule()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        return customMapper.readValue(file, JacksonGeoJsonObject::class.java)
    }

    /**
     * @param position the [GeoPosition] of the boat in the simulation environment.
     * @return true if the position is not inside the polygon of the shore land, otherwise false.
     */
    fun isPositionNavigable(position: GeoPosition): Boolean =
        when (position) {
            is LatLongPosition -> isPositionNavigable(position)
            else -> error("Not yet implemented!")
        }

    /** @return a list of [JacksonGeoJsonObject] parsed from provided files containing routes info. **/
    fun getGeoJsonObjectsForRoutes(): List<JacksonGeoJsonObject> = geoJsonObjectsForRoutes

    /** @return a list of [JacksonGeoJsonObject] parsed from provided files containing shoreline info. **/
    fun getGeoJsonObjectsForShoreline(): List<JacksonGeoJsonObject> = geoJsonObjectsForShoreline

    /**
     * @param position the [LatLongPosition] of the boat in the simulation environment.
     * @return true if the position is not inside the polygon of the shore land, otherwise false.
     */
    fun isPositionNavigable(position: LatLongPosition): Boolean =
        geoJsonObjectsForShoreline.all {
            it.accept(IsNavigableVisitor(position.toLngLatAlt()))
        }

    override fun makePosition(vararg coordinates: Number): GeoPosition {
        require(coordinates.size == 2) {
            "${javaClass.simpleName} only supports bi-dimensional coordinates (latitude, longitude)"
        }
        return LatLongPosition(coordinates[0].toDouble(), coordinates[1].toDouble())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NavigationEnvironment<*>
        if (geoJsonObjectsForShoreline != other.geoJsonObjectsForShoreline) return false
        if (geoJsonObjectsForRoutes != other.geoJsonObjectsForRoutes) return false
        return true
    }

    override fun hashCode(): Int = geoJsonObjectsForShoreline.hashCode() * geoJsonObjectsForRoutes.hashCode()
}
