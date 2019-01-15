package io.fairspace.triton.rdf

import org.apache.jena.atlas.lib.Pair
import org.apache.jena.sparql.core.Quad
import org.apache.jena.sparql.core.QuadAction
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.System.currentTimeMillis

class SimpleTransactionLog(file: File): TransactionLog {
    private val writer = BufferedWriter(FileWriter(file, true))

    override fun log(quads: List<Pair<QuadAction, Quad>>) {
        quads.forEach { writer.appendln(it.toString()) }
        writer.flush()
    }

    override fun commit() {
        writer.append("COMMITTED timestamp=").append(currentTimeMillis().toString()).appendln().appendln()
        writer.flush()
    }

    override fun error() {
        writer.appendln("ERROR timestamp=").append(currentTimeMillis().toString()).appendln().appendln()
        writer.flush()
    }
}