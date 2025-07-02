package it.unibo.util.gpx

/** Utility functions to validate Latitude and Longitude values. **/
object ParsingUtils {
    /** Maximum value for latitude. **/
    const val LATITUDE_MAX_VALUE = 90.0

    /** Maximum value for longitude. **/
    const val LONGITUDE_MAX_VALUE = 180.0

    /**
     * @param latitude the [Double] representing a latitude.
     * @return true if the latitude is in the correct boundaries, otherwise false.
     */
    fun validateLatitude(latitude: Double): Boolean =
        !(latitude.isNaN() || latitude < -LATITUDE_MAX_VALUE || latitude > LATITUDE_MAX_VALUE)

    /**
     * @param longitude the [Double] representing a longitude.
     * @return true if the longitude is in the correct boundaries, otherwise false.
     */
    fun validateLongitude(longitude: Double): Boolean =
        !(longitude.isNaN() || longitude < -LONGITUDE_MAX_VALUE || longitude > LONGITUDE_MAX_VALUE)
}
