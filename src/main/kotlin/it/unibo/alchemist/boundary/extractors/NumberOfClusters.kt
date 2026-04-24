package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule

/**
 * Computes the number of clusters in the simulation.
 */
class NumberOfClusters(
    override val columnNames: List<String> = listOf("n_clusters"),
) : AbstractDoubleExtractor()  {
    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> =
        mapOf(
            columnNames.first() to
                environment.nodes
                    .map { it.contents[Utils.myLeader] }
                    .toSet()
                    .count()
                    .toDouble(),
        )

}
