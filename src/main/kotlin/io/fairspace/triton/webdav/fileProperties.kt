package io.fairspace.triton.webdav

import org.apache.jena.rdf.model.ResourceFactory.createProperty

val FILE_SIZE = createProperty("http://fairspace.io/ontology/File#size")
val FILE_IS_DIRECTORY = createProperty("http://fairspace.io/ontology/File#isDirectory")
val FILE_CONTAINS = createProperty("http://fairspace.io/ontology/File#contains")
val FILE_LOCAL_PATH = createProperty("http://fairspace.io/ontology/File#localPath")
val FILE_LAST_MODIFIED = createProperty("http://fairspace.io/ontology/File#lastModified")
val FILE_CREATED = createProperty("http://fairspace.io/ontology/File#created")
val FILE_IS_READY = createProperty("http://fairspace.io/ontology/File#isReady")
val FILE_CONTENT_TYPE = createProperty("http://fairspace.io/ontology/File#contentType")