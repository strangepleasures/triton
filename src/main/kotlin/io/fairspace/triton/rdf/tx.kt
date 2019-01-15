package io.fairspace.triton.rdf

import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.sparql.core.GraphView
import org.apache.jena.sparql.core.Transactional
import org.apache.jena.system.Txn.calculateRead
import org.apache.jena.system.Txn.calculateWrite

fun tx(resource: Resource): Transactional {
    val graph = resource.model.graph
    return when (graph) {
        is Transactional -> graph
        is GraphView -> graph.dataset
        else -> throw RuntimeException("???")
    }
}

fun <T> Resource.read(action: () -> T): T = calculateRead(tx(this), action)
fun <T> Resource.write(action: () -> T): T = calculateWrite(tx(this), action)


inline operator fun <reified T> Resource.get(property: Property): T {
    val stmt = getProperty(property) ?: return null as T
    val v = stmt.`object`
    return convert(if (v.isLiteral) v.asLiteral().value else v.asResource())
}

operator fun Resource.set(property: Property, value: String) {
    model.removeAll(this, property, null)
    addProperty(property, value)
}

operator fun Resource.set(property: Property, value: RDFNode) {
    model.removeAll(this, property, null)
    addProperty(property, value)
}

operator fun Resource.set(property: Property, value: Boolean) {
    model.removeAll(this, property, null)
    addLiteral(property, value)
}

operator fun Resource.set(property: Property, value: Int) {
    model.removeAll(this, property, null)
    addLiteral(property, value)
}

operator fun Resource.set(property: Property, value: Long) {
    model.removeAll(this, property, null)
    addLiteral(property, value)
}

inline fun <reified T> convert(x: Any?): T =
        when (T::class) {
            Long::class -> (if (x == null) null else (x as Number).toLong())
            else -> x
        } as T

fun Resource.exists() = this.model.containsResource(this)