package io.fairspace.triton.rdf

import org.apache.jena.atlas.lib.Pair
import org.apache.jena.sparql.core.*

interface TransactionLog {
    fun log(quads: List<Pair<QuadAction, Quad>>)

    fun commit()

    fun error()
}
