/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.model.maps.deployments

import it.unibo.alchemist.boundary.gps.loaders.TraceLoader
import it.unibo.alchemist.model.Deployment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.maps.GPSTrace
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * Distributes nodes in the first positions of [GPSTrace].
 * @param traces the [TraceLoader] instance.
 */
class FromGPSTraceCustom(
    private val traces: TraceLoader,
) : Deployment<GeoPosition> {
    /**
     * Creates a [FromGPSTraceCustom] instance.
     * @param path the [String] path where the GPX traces can be load from.
     * @param normalizer
     * @param normalizerArgs
     */
    constructor(path: String, normalizer: String, vararg normalizerArgs: Any) : this(
        TraceLoader(path, false, normalizer, *normalizerArgs),
    )

    override fun stream(): Stream<GeoPosition> =
        StreamSupport
            .stream(traces.spliterator(), false)
            .limit(traces.size().get().toLong())
            .map { obj: GPSTrace -> obj.initialPosition }
}
