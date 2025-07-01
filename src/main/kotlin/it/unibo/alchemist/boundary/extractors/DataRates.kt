package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.exportfilters.CommonFilters
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.util.baseline1DR
import it.unibo.util.baseline2DataRates
import it.unibo.util.baseline2Parent
import it.unibo.util.baseline3DataRates
import it.unibo.util.baseline3Parent
import it.unibo.util.fiveG
import it.unibo.util.iAmLeader
import it.unibo.util.leader
import it.unibo.util.relay
import it.unibo.util.station
import it.unibo.util.toBoolean
import it.unibo.util.toDouble
import it.unibo.util.toInt

/**
 * Computes the data rate for each algorithm considered in this simulation, namely:
 * current state-of-the-art Communication,
 * Distance-Based Multi-Relay Communication,
 * Data Rate-based Multi-Relay Communication,
 * Collective Summarization Clusters.
 *
 * The data rate information is then stored in simulation nodes.
 * @param mode the type of algorithm for which the data rate is computed.
 */
class DataRates(
    val mode: Mode,
) : AbstractAggregatingDoubleExporter(
        CommonFilters.ONLYFINITE.filteringPolicy,
        listOf("mean"),
        3,
    ) {
    private var lastRound = -1L
    private var previous = emptyMap<Any, Double>()

    /** Gathers the reduction factor for CSC [Mode]. **/
    fun <T> getRatios(
        environment: Environment<T, *>,
        round: Long,
    ): Map<Node<T>, Double> =
        if (lastRound == round) {
            @Suppress("UNCHECKED_CAST")
            previous as Map<Node<T>, Double>
        } else {
            ReductionFactor.clusterRatios(environment).also {
                lastRound = round
                @Suppress("UNCHECKED_CAST")
                previous = it as Map<Any, Double>
            }
        }

    /** Creates a data rate extractor from the string representations of a [Mode].
     * @param mode the [String] representation of the [Mode].
     **/
    constructor(mode: String) : this(Mode.fromString(mode))

    override val columnName: String = mode.toString()

    override fun <T> getData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<Node<T>, Double> =
        environment.nodes.associateWith { subject ->
            if (subject.getConcentration(station).toBoolean() || subject.getConcentration(fiveG).toBoolean()) {
                return@associateWith Double.NaN
            }
            when (mode) {
                Mode.B1_DR -> subject.getConcentration(baseline1DR).toDouble()
                Mode.DIST_MR -> nonAggregatingDataRate(environment, subject, baseline2Parent, baseline2DataRates)
                Mode.DR_MR -> nonAggregatingDataRate(environment, subject, baseline3Parent, baseline3DataRates)
                Mode.CSC -> {
                    val reductionFactors = getRatios(environment, step)
                    val leader = environment.getNodeByID(subject.getConcentration(leader).toInt())
                    clusteringDataRate(environment, reductionFactors, leader, 3000.0)
                }
            }
        }

    /** Tail recursive function that computes data rate in non-aggregating algorithms: all of them excepts CSC. **/
    tailrec fun <T> nonAggregatingDataRate(
        environment: Environment<T, *>,
        subject: Node<T>,
        getLeader: Molecule,
        dataRate: Molecule,
        visited: Set<Int> = emptySet(),
    ): Double =
        when {
            subject.getConcentration(station).toBoolean() -> 3000.0
            subject.getConcentration(dataRate).toDouble() >= 3000 -> {
                val next = subject.getConcentration(leader).toInt()
                when {
                    next in visited -> 0.0
                    else ->
                        nonAggregatingDataRate(
                            environment,
                            environment.getNodeByID(next),
                            getLeader,
                            dataRate,
                            visited + next,
                        )
                }
            }
            else -> 0.0
        }

    /** Function that computes the data rate depending on the selected [Mode]. **/
    fun <T> clusteringDataRate(
        environment: Environment<T, *>,
        reductionFactors: Map<Node<T>, Double>,
        subject: Node<T>,
        dataRate: Double,
        visited: Set<Int> = emptySet(),
    ): Double =
        when {
            subject.getConcentration(station).toBoolean() -> dataRate
            subject.id in visited -> 0.0
            subject.getConcentration(iAmLeader).toBoolean() -> {
                val reducedDataRate = dataRate * reductionFactors.getValue(subject)
                val relay = environment.getNodeByID(subject.getConcentration(relay).toInt())
                when {
                    reducedDataRate == 0.0 -> 0.0
                    relay == subject -> 0.0
                    else ->
                        clusteringDataRate<T>(
                            environment,
                            reductionFactors,
                            relay,
                            reducedDataRate,
                            visited + relay.id,
                        )
                }
            }
            else -> {
                val myLeader = subject.getConcentration(leader).toInt()
                when {
                    myLeader == subject.id -> 0.0
                    else ->
                        clusteringDataRate(
                            environment,
                            reductionFactors,
                            environment.getNodeByID(myLeader),
                            dataRate,
                            visited + myLeader,
                        )
                }
            }
        }
}

/**
 * Enumeration of the algorithms compared in the simulation.
 */
enum class Mode {
    /** Current state-of-the-art Communication. **/
    B1_DR,

    /** Distance-Based Multi-Relay Communication (Dist-MR). **/
    DIST_MR,

    /** Data Rate-based Multi-Relay Communication (DR-MR). **/
    DR_MR,

    /** Collective Summarization Clusters (CSC). **/
    CSC,

    ;

    companion object {
        /**
         * Converts a [String] in a [Mode] enumeration, when possible.
         * @param value the [String]
         * @return a [Mode] value
         * @throws [IllegalArgumentException] if from the [String] is not possible to determine the [Mode].
         */
        fun fromString(value: String): Mode =
            when (value) {
                "b1-dr" -> B1_DR
                "b2-dr" -> DIST_MR
                "b3-dr" -> DR_MR
                "bc-dr" -> CSC
                else -> throw IllegalArgumentException("Unknown mode: $value")
            }
    }
}
