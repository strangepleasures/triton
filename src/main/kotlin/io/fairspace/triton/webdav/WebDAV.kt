package io.fairspace.triton.webdav

import io.milton.config.HttpManagerBuilder


fun startWebDAV(rootFolder: org.apache.jena.rdf.model.Resource): MiltonHandler {
    val builder = HttpManagerBuilder()
    val userCredentials = mapOf<String, String>()
    builder.resourceFactory = MiltonWebDAVResourceFactory(rootFolder, userCredentials)
    builder.isEnableBasicAuth = !userCredentials.isEmpty()
    val mgr = builder.buildHttpManager()
    return MiltonHandler(mgr)
}