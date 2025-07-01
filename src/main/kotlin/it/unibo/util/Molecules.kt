package it.unibo.util

import it.unibo.alchemist.model.molecules.SimpleMolecule

/** Value representing whether the node is a 5G antenna or not. **/
val fiveG = SimpleMolecule("5gAntenna")

/** Value representing whether the node is a shore station or not. **/
val station = SimpleMolecule("station")

/** Id of the CSC leader of this node. **/
val leader = SimpleMolecule("myLeader")

/** Value stating whether the node is a CSC leader or not. **/
val iAmLeader = SimpleMolecule("imLeader")

/** Value stating whether the node is a CSC relay or not. **/
val iAmRelay = SimpleMolecule("imRelay")

/** Id of the node towards which information is forwarded in CSC. **/
val relay = SimpleMolecule("myRelay")

/** Id of the relay node inside of the cluster in CSC. **/
val intraClusterRelay = SimpleMolecule("intra-cluster-relay")

/** Intra-Cluster data rate in Collective Summarization Clusters (CSC). **/
val intraClusterDR = SimpleMolecule("export-intra-cluster-relay-data-rate-not-leader")

/** Leader to relay data rate in Collective Summarization Clusters (CSC). **/
val interClusterDR = SimpleMolecule("leader-to-relay-data-rate")

/** Data rate of baseline 1: state-of-the-art vessels communication. **/
val baseline1DR = SimpleMolecule("baseline1-data-rate")

/** Id of the node towards which information is forwarded in Dist-MR. **/
val baseline2Parent = SimpleMolecule("baseline2-parent")

/** Data rate of baseline 2: Distance-Based Multi-Relay Communication (Dist-MR). **/
val baseline2DataRates = SimpleMolecule("baseline2-data-rate")

/** Id of the node towards which the information is forwarded in DR-MR. **/
val baseline3Parent = SimpleMolecule("baseline3-parent")

/** Data rate of baseline 3: Data Rate-based Multi-Relay Communication (DR-MR). **/
val baseline3DataRates = SimpleMolecule("baseline3-data-rate")
