package it.unibo.clustered.seaborn.comm

import it.unibo.clustered.seaborn.comm.Metric.disconnected
import it.unibo.clustered.seaborn.comm.Metric.kiloBitsPerSecond
import java.io.File
import java.util.Locale
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.exp
import kotlin.math.ln

/** Fit exponential function y = a * exp(-b * x). **/
fun fitExponential(dataRates: Map<Distance, DataRate>): (Distance) -> DataRate {
    // Solve:
    // y = a * exp(-b * x)
    // ln(y) = ln(a) - b * x  -> linear regression
    require(dataRates.values.all { it.kiloBitsPerSecond > 0 }) {
        "All data rates must be strictly positive (found: ${dataRates.filterValues { it.kiloBitsPerSecond <= 0 }})"
    }
    val distances = dataRates.keys
    val n = dataRates.size
    val lnY = dataRates.mapValues { (_, dataRate) -> ln(dataRate.kiloBitsPerSecond) }
    val sumX = distances.sumOf { it.meters }
    check(sumX.isFinite()) { "Sum of distances must be finite" }
    val sumLnY = lnY.values.sum()
    check(sumLnY.isFinite()) { "Sum of lnY must be finite" }
    val sumX2 = distances.sumOf { it.meters * it.meters }
    val sumXlnY = lnY.toList().sumOf { it.first.meters * it.second }
    val denominator = n * sumX2 - sumX * sumX
    check(denominator > 0) { "Denominator must be positive" }
    val b = (n * sumXlnY - sumX * sumLnY) / denominator
    check(b.isFinite()) { "B must be finite" }
    val lnA = (sumLnY - b * sumX) / n
    check(lnA.isFinite()) { "lnA must be finite" }
    val a = exp(lnA)
    check(a > 0) { "a must be positive" }
    return { d: Distance -> (a * exp(-b * d.meters)).kiloBitsPerSecond }
}

// fun interpolateLogLinear(data: Map<Distance, DataRate>): (Distance) -> DataRate {
//    val sorted = data.entries.sortedBy { it.key.meters }
//    return { d: Distance ->
//        val x = d.meters
//        val (low, high) = sorted
//            .zipWithNext()
//            .firstOrNull { x in it.first.key.meters..it.second.key.meters }
//            ?: if (x < sorted.first().key.meters) sorted.first() to sorted.first()
//            else sorted.last() to sorted.last()
//
//        val x0 = low.key.meters
//        val x1 = high.key.meters
//        val y0 = ln(low.value.kiloBitsPerSecond)
//        val y1 = ln(high.value.kiloBitsPerSecond)
//
//        val proportion = if (x1 != x0) (x - x0) / (x1 - x0) else 0.0
//        val lnInterpolated = y0 + proportion * (y1 - y0)
//        exp(lnInterpolated).kiloBitsPerSecond
//    }
// }

/** Interpolated LogLinear function for distances and data rates. **/
fun interpolateLogLinear(data: Map<Distance, DataRate>): (Distance) -> DataRate {
    val sorted = data.entries.sortedBy { it.key.meters }
    val xs = sorted.map { it.key.meters }.toDoubleArray()
    val ysLog = sorted.map { ln(it.value.kiloBitsPerSecond) }.toDoubleArray()
    return { d: Distance ->
        val x = d.meters
        val idx = xs.binarySearch(x).let { if (it >= 0) it else -(it + 1) }
        val i0 =
            when {
                idx == 0 -> 0
                idx >= xs.size -> xs.lastIndex
                else -> idx - 1
            }
        val i1 =
            when {
                idx == 0 -> 0
                idx >= xs.size -> xs.lastIndex
                else -> idx
            }
        val x0 = xs[i0]
        val x1 = xs[i1]
        val y0 = ysLog[i0]
        val y1 = ysLog[i1]
        val proportion = if (x1 != x0) (x - x0) / (x1 - x0) else 0.0
        val lnInterpolated = y0 + proportion * (y1 - y0)
        exp(lnInterpolated).kiloBitsPerSecond
    }
}

/**
 * Converts a [Distance] into a [DataRate].
 */
fun interface ConnectionTechnology {
    /**
     * Function that given a distance from the source returns the corresponding data rate,
     * which value depends on the communication technology used.
     * @param distance the [Distance] from the source.
     * @return the corresponding [DataRate] value.
     */
    operator fun invoke(distance: Distance): DataRate

    /**
     * Static factory for [ConnectionTechnology].
     */
    companion object {
        /** Creates a [ConnectionTechnology] by interpolating the missing points. **/
        fun byInterpolation(
            dataRates: Map<Distance, DataRate>,
            maxRange: Distance = dataRates.keys.max(),
        ): ConnectionTechnology =
            object : ConnectionTechnology {
                val interpolated = interpolateLogLinear(dataRates)

                override fun invoke(distance: Distance): DataRate =
                    when {
                        distance > maxRange -> disconnected
                        else -> interpolated(distance)
                    }
            }
    }
}

/** Distance representation.
 * @param meters the distance in meters.
 **/
@JvmInline
value class Distance(
    val meters: Double,
) : Comparable<Distance> {
    /** Converts the distance from meters to kilometers. **/
    val kilometers: Double get() = meters / 1000.0

    override fun toString(): String =
        when {
            meters > 1000 -> "${kilometers.readable}km"
            else -> "${meters.readable}m"
        }

    /** Computes the sum of two [Distance]. **/
    operator fun plus(other: Distance): Distance = Distance(meters + other.meters)

    override fun compareTo(other: Distance): Int = meters.compareTo(other.meters)
}

/** Conversion from [Double] representation of meters to [Distance]. **/
val Double.meters get() = Distance(this)

/** Conversion from [Double] representation of kilometers to [Distance]. **/
val Double.kilometers get() = Distance(this * 1e3)

/** Conversion from [Int] representation of meters to [Distance]. **/
val Int.meters get() = toDouble().meters

/** Conversion from [Int] representation of kilometers to [Distance]. **/
val Int.kilometers get() = toDouble().kilometers

/** Formatted [String] of a [Double]. **/
val Double.readable get() = String.format(Locale.ENGLISH, "%.3f", this)

/** Data rate representation in kbps.
 * @param kiloBitsPerSecond the kilobits of the data rate.
 **/
@JvmInline
value class DataRate(
    val kiloBitsPerSecond: Double,
) : Comparable<DataRate> {
    /** Representation of the data rate into Mbps. **/
    val megaBitsPerSecond: Double get() = kiloBitsPerSecond / 1000.0

    /** Representation of the data rate into Gbps. **/
    val gigaBitsPerSecond: Double get() = megaBitsPerSecond / 1000.0

    /** Computes the time required to transmit 1Kb. **/
    val timeToTransmitOneKb: Double get() = 1.0 / kiloBitsPerSecond

    /** Computes the time required to transmit 1Mb. **/
    val timeToTransmitOneMb get() = timeToTransmitOneKb * 1e3

    override fun toString(): String =
        when {
            kiloBitsPerSecond.isInfinite() -> "loopback"
            gigaBitsPerSecond > 1 -> "${gigaBitsPerSecond.readable}Gbps"
            megaBitsPerSecond > 1 -> "${megaBitsPerSecond.readable}Mbps"
            else -> "${kiloBitsPerSecond.readable}Kbps"
        }

    /** Computes the Sum of two data rates. **/
    operator fun plus(other: DataRate): DataRate = DataRate(kiloBitsPerSecond + other.kiloBitsPerSecond)

    /** Computes the difference between two data rates. The resulting data rate will be at most 0.0, not negative. **/
    operator fun minus(other: DataRate): DataRate =
        DataRate(
            (kiloBitsPerSecond - other.kiloBitsPerSecond)
                .coerceAtLeast(0.0),
        )

    /** Multiplies two data rates. **/
    operator fun times(other: Double): DataRate = DataRate(kiloBitsPerSecond * other)

    /** Divides two data rates. **/
    operator fun div(other: Double): DataRate = DataRate(kiloBitsPerSecond / other)

    override fun compareTo(other: DataRate) = kiloBitsPerSecond.compareTo(other.kiloBitsPerSecond)
}

/** Metric functions used in the simulation for the communication means involved. **/
object Metric {
    /** Used for conversions of bps. **/
    const val ONE_MILLION = 1e6

    /** Used for conversions of bps. **/
    const val ONE_THOUSANDSTH = 1e-3

    /** Disconnected data rate == 0.0. **/
    val disconnected = DataRate(0.0)

    /** Data rate towards the same device. **/
    val loopBack = DataRate(POSITIVE_INFINITY)

    /** Conversion from [Double] Kbps into [DataRate]. **/
    val Double.kiloBitsPerSecond get() = DataRate(this)

    /** Conversion from [Double] Mbps into [DataRate]. **/
    val Double.megaBitsPerSecond get() = DataRate(this * 1e3)

    /** Conversion from [Double] Gbps into [DataRate]. **/
    val Double.gigaBitsPerSecond get() = DataRate(this * ONE_MILLION)

    /** Conversion from [Int] bps into [DataRate]. **/
    val Int.bitsPerSecond get() = DataRate(this * ONE_THOUSANDSTH)

    /** Conversion from [Int] Kbps into [DataRate]. **/
    val Int.kiloBitsPerSecond get() = toDouble().kiloBitsPerSecond

    /** Conversion from [Int] Mbps into [DataRate]. **/
    val Int.megaBitsPerSecond get() = toDouble().megaBitsPerSecond

    /** Conversion from [Int] Gbps into [DataRate]. **/
    val Int.gigaBitsPerSecond get() = toDouble().gigaBitsPerSecond

    /**
     * Connection Technology function for LoRAWAN.
     */
    val lora =
        ConnectionTechnology.byInterpolation(
            mapOf(
                10.meters to 50.kiloBitsPerSecond,
                15.kilometers to 300.bitsPerSecond,
                16.kilometers to 1.bitsPerSecond,
            ),
        )

    /**
     * Connection Technology function for Wi-Fi.
     */
    val wifi =
        ConnectionTechnology.byInterpolation(
            mapOf(
                10.meters to 750.megaBitsPerSecond,
                20.meters to 600.megaBitsPerSecond,
                30.meters to 450.megaBitsPerSecond,
                40.meters to 350.megaBitsPerSecond,
                50.meters to 250.megaBitsPerSecond,
                60.meters to 180.megaBitsPerSecond,
                70.meters to 120.megaBitsPerSecond,
                80.meters to 80.megaBitsPerSecond,
                90.meters to 40.megaBitsPerSecond,
                120.meters to 1.bitsPerSecond,
            ),
        )

    /**
     * Connection Technology function for 5G.
     */
    val midband5G =
        ConnectionTechnology.byInterpolation(
            mapOf(
                50.meters to 1.gigaBitsPerSecond,
                500.meters to 800.megaBitsPerSecond,
                1.kilometers to 600.megaBitsPerSecond,
                1.5.kilometers to 400.megaBitsPerSecond,
                2.kilometers to 250.megaBitsPerSecond,
                2.5.kilometers to 200.megaBitsPerSecond,
                3.kilometers to 150.megaBitsPerSecond,
                3.5.kilometers to 110.megaBitsPerSecond,
                4.kilometers to 50.megaBitsPerSecond,
                5.kilometers to 5.megaBitsPerSecond,
            ),
        )

    /**
     * Connection Technology function for ARPS.
     */
    val aprs =
        ConnectionTechnology.byInterpolation(
            mapOf(
                1.kilometers to 9600.bitsPerSecond,
                5.kilometers to 9000.bitsPerSecond,
                10.kilometers to 8500.bitsPerSecond,
                20.kilometers to 7000.bitsPerSecond,
                30.kilometers to 4000.bitsPerSecond,
                40.kilometers to 2000.bitsPerSecond,
                50.kilometers to 1000.bitsPerSecond,
            ),
        )

    /** Upper bound for creating csv interpolation file. **/
    const val UPPER_BOUND = 60000

    /** Utility function that exports the Communication technologies into a csv file to generate charts out of it. **/
    fun exportMetricInCsv(fileName: String = "data/metric_data.csv") {
        val file = File(fileName)
        file.printWriter().use { out ->
            out.println("x,y_wifi,y_aprs,y_lora,y_midband5g")
            for (i in 1 until UPPER_BOUND) {
                out.println(
                    listOf(
                        i.meters.meters,
                        wifi.invoke(i.meters).megaBitsPerSecond,
                        aprs.invoke(i.meters).megaBitsPerSecond,
                        lora.invoke(i.meters).megaBitsPerSecond,
                        midband5G.invoke(i.meters).megaBitsPerSecond,
                    ).joinToString(","),
                )
            }
            println("CSV exported successfully.")
        }
    }
}
