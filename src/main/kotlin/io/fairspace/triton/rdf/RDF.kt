package io.fairspace.triton.rdf

import io.fairspace.triton.webdav.startWebDAV
import org.apache.jena.dboe.transaction.TransactionalMonitor
import org.apache.jena.fuseki.main.FusekiServer
import org.apache.jena.permissions.Factory
import org.apache.jena.query.ReadWrite
import org.apache.jena.query.TxnType
import org.apache.jena.sparql.core.DatasetChangesCapture
import org.apache.jena.sparql.core.DatasetGraphWrapper
import org.apache.jena.sparql.core.DatasetOne
import org.apache.jena.tdb2.TDB2Factory.connectDataset
import org.apache.jena.tdb2.store.DatasetGraphTDB
import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.util.security.Constraint
import java.io.File


fun startRDF() {
    val ds = connectDataset("db")
    val innerGraph = (ds.asDatasetGraph() as DatasetGraphWrapper).wrapped as DatasetGraphTDB

    val monitor = DatasetChangesCapture()
    innerGraph.setMonitor(monitor)

    val log = SimpleTransactionLog(File("log.txt"))

    innerGraph.setTransactionalMonitor(object : TransactionalMonitor {
        private var writing = ThreadLocal.withInitial {false}

        override fun startBegin(txnType: TxnType?) {
            writing.set(false)
        }

        override fun startCommit() {
            if (innerGraph.txnSystem.threadTransaction.mode == ReadWrite.WRITE) {
                val actions = monitor.actions
                if (actions.isNotEmpty()) {
                    writing.set(true)
                    log.log(actions)
                    monitor.reset()
                    monitor.start()
                }
            }
        }

        override fun finishCommit() {
            if (writing.get()) {
                log.commit()
            }
        }
    })

    val security = ConstraintSecurityHandler()

    val constraint = Constraint()
    constraint.name = "auth"
    constraint.authenticate = true
    constraint.roles = arrayOf("user", "admin")

    val mapping = ConstraintMapping()
    mapping.pathSpec = "/*"
    mapping.constraint = constraint


    security.constraintMappings = listOf(mapping)
    security.authenticator = KeycloakJettyAuthenticatorEx()

    val model = ds.defaultModel
    val securedModel = Factory.getInstance(Security(null), "secure", model)
    val securedDataset = DatasetOne(securedModel)

    val rootFolder = model.getResource("http://fairspace.io")
    val webDavHandler = startWebDAV(rootFolder)


    FusekiServer.create()
            .add("/rdf", securedDataset)
            .securityHandler(security)
            .build()
            .apply {
                server.handler = HandlerList(webDavHandler, server.handler)
            }
            .start()
}