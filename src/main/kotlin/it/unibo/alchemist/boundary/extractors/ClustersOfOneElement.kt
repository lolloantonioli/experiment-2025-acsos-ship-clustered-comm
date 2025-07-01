package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule

/**
 * Computes the number of clusters composed by only one vessel in the simulation.
 */
class ClustersOfOneElement(
    override val columnNames: List<String> = listOf("clustersComposedOfOneElement"),
) : AbstractDoubleExporter() {
    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> =
        mapOf(
            columnNames.first() to
                environment.nodes
                    .map {
                        it.contents[myLeader]
                    }.groupingBy { it }
                    .eachCount()
                    .filter { it.value == 1 }
                    .count()
                    .toDouble(),
        )

    companion object {
        /**
         * Gathers the leader of the node.
         * This information is stored in each node in the simulation as "myLeader".
         */
        val myLeader = SimpleMolecule("myLeader")
    }
}
