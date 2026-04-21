package it.unibo.clustered.seaborn.comm

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.clustered.seaborn.comm.Metric.aprs
import it.unibo.clustered.seaborn.comm.Metric.bitsPerSecond
import it.unibo.clustered.seaborn.comm.Metric.disconnected
import it.unibo.clustered.seaborn.comm.Metric.gigaBitsPerSecond
import it.unibo.clustered.seaborn.comm.Metric.loopBack
import it.unibo.clustered.seaborn.comm.Metric.lora
import it.unibo.clustered.seaborn.comm.Metric.megaBitsPerSecond
import it.unibo.clustered.seaborn.comm.Metric.midband5G
import it.unibo.clustered.seaborn.comm.Metric.wifi
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.exchange
import it.unibo.collektive.aggregate.api.mapNeighborhood
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.stdlib.collapse.any
import it.unibo.collektive.stdlib.collapse.fold
import it.unibo.collektive.stdlib.collapse.maxBy
import it.unibo.collektive.stdlib.collapse.minBy
import it.unibo.collektive.stdlib.collapse.valueOfMaxBy
import it.unibo.collektive.stdlib.consensus.Candidacy
import it.unibo.collektive.stdlib.consensus.boundedElection
import it.unibo.collektive.stdlib.doubles.FieldedDoubles.plus
import it.unibo.collektive.stdlib.spreading.distanceTo
import it.unibo.util.toDouble
import kotlin.Double.Companion.POSITIVE_INFINITY

private typealias RelayInfo<ID> = Pair<ID, Double>

private fun <ID> RelayInfo<ID>.relayId(): ID = first

private val RelayInfo<*>.distanceToLeader: Double get() = second

/** Inject the information inside Alchemist's simulation node, for future retrieval.
 * @param T the value to inject
 * @param environment the alchemist environment
 * @param name the name of the molecule on which inject the value.
 **/
fun <T> T.inject(
    environment: CollektiveDevice<*>,
    name: String,
) = also {
    environment[name] = this
    environment["export-$name"] = this.toDouble()
}

/** Aggregate algorithm executed on each node. **/
fun Aggregate<Int>.entrypoint(environment: CollektiveDevice<*>): Any? {
    // Streaming data rate
    val payloadSize = environment.get<Double>("payloadSize")
    val streamingBitRate = payloadSize.megaBitsPerSecond

    val groundStation: Boolean = environment.isDefined("station")
    val distances: Field<Int, Distance> =
        with(environment) { distances() }
            .map { it.value.meters }
            .inject(environment, "distance")
    val is5gAntenna: Field<Int, Boolean> = neighboring(environment.isDefined("5gAntenna"))
    val thisIsA5gAntenna = is5gAntenna.local.value
    val dataRates: Field<Int, DataRate> = computeDataRates(environment, distances)
    // Once data rates have been established, 5g towers are cut off the computation,
    // they just relay the communication
    if (!thisIsA5gAntenna) {
        dataRates.all.maxBy { it.value }.inject(environment, "max-data-rate") ?: disconnected
        val timeToTransmit = dataRates.map { it.value.timeToTransmitOneMb }.inject(environment, "metric")

        // Baseline 1: direct communication with the station only
        val stationsNearby: Field<Int, Boolean> = neighboring(groundStation)
        val baseline1 =
            stationsNearby.alignedMap(dataRates) { _, isStation, dataRate ->
                dataRate.takeIf { isStation } ?: disconnected
            }
        val baseline1MaxData = baseline1.all
            // Calcolo il field con il valore max e ne prendo solo il valore e non l'id
            .valueOfMaxBy { it.value }
            .takeIf { it >= 3.megaBitsPerSecond } ?: disconnected
        baseline1MaxData.max3Mbit().inject(environment, "baseline1-data-rate")

        // Baseline 2: min distance data rate
        val baseline2DistanceToStation =
            distanceTo(
                groundStation,
                bottom = 0.meters,
                top = POSITIVE_INFINITY.meters,
                metric = distances,
                accumulateDistance = Distance::plus,
            ).inject(environment, "baseline2-distanceToStation")
        val neighborDistances = neighboring(baseline2DistanceToStation)
        // se uso alignedMap non posso usare method reference
        val distanceThroughRelay = distances.alignedMapValues(neighborDistances, Distance::plus)
        val baseline2Parent =
            distanceThroughRelay.all
                .fold(localId to POSITIVE_INFINITY.meters) { current, entry ->
                    val distance = entry.value
                    when {
                        current.second > distance -> entry.id to distance
                        else -> current
                    }
                }.first
                .inject(environment, "baseline2-parent")
        dataRates[baseline2Parent].inject(environment, "baseline2-parent-data-rate")
        computeNonCooperativeDataRate(
            "baseline2",
            streamingBitRate,
            environment,
            baseline2Parent,
            stationsNearby,
            dataRates,
        )

        // Baseline 3: min time to station
        val baseline3TimeToStation =
            distanceTo(
                groundStation,
                metric = timeToTransmit
            ).inject(environment, "baseline3-timeToStation")

        val baseline3neighborTimes = neighboring(baseline3TimeToStation)
        val baseline3TimeToStationThroughRelays =
            timeToTransmit.alignedMapValues(
                baseline3neighborTimes,
                Double::plus
            )
        val baseline3Parent =
            baseline3TimeToStationThroughRelays.all
                .fold(localId to POSITIVE_INFINITY) { current, entry ->
                    val time = entry.value
                    when {
                        current.second > time -> entry.id to time
                        else -> current
                    }
                }.first
                .inject(environment, "baseline3-parent")
        dataRates[baseline3Parent].inject(environment, "baseline3-parent-data-rate")
        computeNonCooperativeDataRate(
            "baseline3",
            streamingBitRate,
            environment,
            baseline3Parent,
            stationsNearby,
            dataRates,
        )

        // Clustered
        val clusteredTimeToStation: Double =
            distanceTo(
                groundStation,
                metric = timeToTransmit
            ).inject(environment, "clustered-timeToStation")

        // Clustered
        val myLeader: Int =
            boundedElection(
                strength = -clusteredTimeToStation,
                bound = streamingBitRate.timeToTransmitOneMb,
                metric = timeToTransmit,
                selectBest = { c1, c2 ->
                    maxOf(
                        c1,
                        c2,
                        compareBy<Candidacy<Int, Double, Double>> { it.strength }.thenBy { it.candidate },
                    )
                },
            ).inject(environment, "myLeader")
        val imLeader = myLeader == localId
        imLeader.inject(environment, "imLeader")
        val distanceToLeader =
            alignedOn(myLeader) {
                distanceTo(
                    imLeader,
                    metric = timeToTransmit
                ).inject(environment, "distanceToLeader")
            }
        val idOfIntraClusterRelay = neighboring(distanceToLeader)
            .alignedMapValues(timeToTransmit, Double::plus)
            .alignedMapValues(neighboring(myLeader)) { distance, leader ->
                when (leader) {
                    myLeader -> distance
                    else -> POSITIVE_INFINITY
                }
            }.all
            // it.value equivale al second del Pair cioè a distanceToLeader
            .minBy { it.value }.id
            .inject(environment, "intra-cluster-relay")
        val intraClusterDataRate =
            dataRates[idOfIntraClusterRelay]
                .inject(environment, "intra-cluster-relay-data-rate")
        (intraClusterDataRate.takeUnless { imLeader } ?: Double.NaN)
            .inject(environment, "intra-cluster-relay-data-rate-not-leader")

        val timesToStationAround =
            neighboring(clusteredTimeToStation)
                .inject(environment, "timesToStationAround")
        val localTimeToStation = timesToStationAround.local.value
        val potentialRelays = neighboring(myLeader)
            .alignedMapValues(timesToStationAround) { leader, distance ->
                leader != myLeader && distance < localTimeToStation
            }.inject(environment, "potentialRelays")
        val myRelay =
            potentialRelays
                .alignedMapValues(timesToStationAround + timeToTransmit) { canRelay, distance ->
                    when {
                        canRelay -> distance
                        else -> POSITIVE_INFINITY
                    }
                }.all
                .minBy { it.value }.id
                .inject(environment, "myRelay")
        neighboring(myRelay).all.any { it.value == localId }.inject(environment, "imRelay")
        val upstreamToRelay = dataRates[myRelay]
        val iHaveARelay = imLeader && myRelay != localId
        environment["leader-to-relay-data-rate"] =
            when {
                iHaveARelay -> upstreamToRelay.kiloBitsPerSecond
                else -> Double.NaN
            }
        return timeToTransmit[myRelay]
    }

    return Unit
}

/**
 * Computes the data rate of the node towards the station without optimizing transmission.
 */
fun Aggregate<Int>.computeNonCooperativeDataRate(
    experimentName: String,
    streamingBitRate: DataRate,
    environment: CollektiveDevice<*>,
    parent: Int,
    stationsNearby: Field<Int, Boolean>,
    dataRates: Field<Int, DataRate>,
) {
    val children = neighboring(parent).all
        .fold(mutableSetOf<Int>()) { accumulator, entry ->
            val neighborParentId = entry.value
            accumulator.also { if (neighborParentId == localId) it += entry.id }
        }.inject(environment, "$experimentName-children")
    val childrenCount = children.size.inject(environment, "$experimentName-children-count")
    val parentDataRate: DataRate =
        when {
            parent == localId -> 0.bitsPerSecond
            else -> dataRates[parent]
        }.inject(environment, "$experimentName-parent-clean-data-rate")
    exchange(parentDataRate) { xcDataRates ->
        val actualUpload: DataRate =
            dataRates.alignedMap(xcDataRates) { id, idealDR, actualDR ->
                when {
                    stationsNearby[id] -> idealDR
                    else -> actualDR
                }
            }[parent]
        val sharedUpload: DataRate = (actualUpload - streamingBitRate) / childrenCount.toDouble()
        mapNeighborhood { id ->
            when (id) {
                localId -> actualUpload
                in children -> sharedUpload
                else -> 0.bitsPerSecond
            }
        }
    }.local.value.inject(environment, "$experimentName-data-rate")
}

/** Computes data rate for the node towards its neighbors. **/
fun Aggregate<Int>.computeDataRates(
    environment: CollektiveDevice<*>,
    distances: Field<Int, Distance>,
): Field<Int, DataRate> {
    fun <T> T.inject(name: String) = also { environment[name] = this }
    val is5gAntenna: Field<Int, Boolean> = neighboring(environment.isDefined("5gAntenna"))
    val thisIsA5gAntenna = is5gAntenna.local.value
    val dataRate5g =
        distances
            .alignedMap(is5gAntenna) { id, distance, neighborIs5g ->
                when {
                    id == localId -> loopBack
                    neighborIs5g && thisIsA5gAntenna -> 10.gigaBitsPerSecond // 5g towers with fiber backhaul
                    neighborIs5g || thisIsA5gAntenna -> midband5G(distance)
                    else -> disconnected
                }
            }.inject("5g data rates")
            .all.fold(disconnected) { acc, entry -> maxOf(acc, entry.value) }
            .inject("5g data rate")

    val has5gAntenna =
        evolve(
            when (val probability = environment.get<Any?>("5gProbability")) {
                is Number -> environment.randomGenerator.nextDouble() < probability.toDouble()
                else -> false
            },
        ) { it }.inject("has5gAntenna")
    val antennasAround = neighboring(has5gAntenna).all.any { it.value } || has5gAntenna
    return distances
        .map { entry ->
            val id = entry.id
            val distance = entry.value
            when {
                id == localId -> loopBack
                else -> {
                    val classicDataRates = maxOf(lora(distance), wifi(distance), aprs(distance), dataRate5g)
                    when {
                        antennasAround -> {
                            val shipToShip5G = maxOf(dataRate5g, midband5G(distance))
                            maxOf(shipToShip5G, classicDataRates)
                        }
                        else -> classicDataRates
                    }
                }
            }
        }.inject("dataRates")
}

/** Applies an upper-bound of 3Mbps to this [DataRate].
 * @return at most 3Mbps.
 **/
fun DataRate.max3Mbit(): Double = kiloBitsPerSecond.coerceAtMost(3.megaBitsPerSecond.kiloBitsPerSecond)
