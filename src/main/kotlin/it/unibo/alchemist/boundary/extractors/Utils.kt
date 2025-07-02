package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.molecules.SimpleMolecule

/**
 * Alchemist Molecules used to gather information from nodes.
 */
object Utils {
    /**
     * Gathers the leader of the node.
     * This information is stored in each node in the simulation as "myLeader".
     */
    val myLeader = SimpleMolecule("myLeader")
}
