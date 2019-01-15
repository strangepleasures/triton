package io.fairspace.triton.webdav

import io.fairspace.triton.rdf.*
import io.milton.http.Range
import io.milton.http.exceptions.BadRequestException
import io.milton.http.exceptions.ConflictException
import io.milton.http.exceptions.NotAuthorizedException
import io.milton.http.exceptions.NotFoundException
import io.milton.resource.CollectionResource
import io.milton.resource.DeletableResource
import io.milton.resource.FolderResource
import io.milton.resource.Resource
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.mina.core.RuntimeIoException
import org.slf4j.LoggerFactory
import java.io.*
import java.lang.System.currentTimeMillis
import java.net.URI
import java.util.UUID.randomUUID

/**
 * A [milton FolderResource][FolderResource] to serve the contents of a single folder.
 */
class MiltonFolderResource(file: org.apache.jena.rdf.model.Resource, resourceFactory: MiltonWebDAVResourceFactory)
    : MetadataResource(file, resourceFactory), FolderResource {

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun child(childName: String): Resource? {
        LOGGER.debug("Getting child {} in {}", childName, this.file)

        return resourceFactory.getResource(file.model.createResource(file.uri + '/' + childName))
    }

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun getChildren(): List<MetadataResource> {
        LOGGER.debug("Getting children in {}", this.file)

        return read {
            file.listProperties(FILE_CONTAINS).asSequence().map {
                resourceFactory.getResource(it.`object`.asResource())!!
            }.sortedBy(Resource::getName).toList()
        }
    }

    @Throws(NotAuthorizedException::class, BadRequestException::class, ConflictException::class)
    override fun copyTo(rDest: CollectionResource, name: String) {
        log.debug { "Copying folder $file to ${rDest.name}/$name" }

        rDest as MiltonFolderResource

        val target = file.model.createResource(rDest.file.uri + '/' + name)

        write {
            file.model.removeAll(target, null, null)

            file.listProperties().forEach {
                file.model.add(target, it.predicate, it.`object`)
            }

            file.model.listStatements(null, null, file).forEach {
                file.model.add(it.subject, it.predicate, target)
            }

            rDest.file.addProperty(FILE_CONTAINS, target)

            val targetFolder = MiltonFolderResource(target, resourceFactory)

            targetFolder.children.forEach {
                it.copyTo(targetFolder, it.name)
            }
        }
    }

    @Throws(NotAuthorizedException::class, ConflictException::class, BadRequestException::class)
    override fun delete() {
        LOGGER.debug("Deleting {}", this.file)

        write {
            children.forEach(DeletableResource::delete)
            file.model.removeAll(file, null, null).removeAll(null, null, file)
        }
    }

    @Throws(IOException::class, NotAuthorizedException::class, BadRequestException::class, NotFoundException::class)
    override fun sendContent(out: OutputStream, range: Range?, params: Map<String, String>?, contentType: String?) {
        LOGGER.debug("Sending content for folder {} and contenttype {}", this.file, contentType)
        val relativePath = URI(file.uri).path

        PrintWriter(out).use {
            it.println("<html><head><title>Folder listing for $relativePath</title></head>")
            it.println("<body>")
            it.println("<h1>Folder listing for $relativePath</h1>")
            it.println("<ul>")
            for (f in children) {
                it.println("<li><a href=\"" +  URI(file.uri).path + '/' + f.name + "\">" + f.name + "</a></li>")
            }
            it.println("</ul></body></html>")
        }
    }

    override fun getContentType(accepts: String?): String? {
        return null
    }

    override fun getContentLength(): Long? {
        return null
    }

    @Throws(NotAuthorizedException::class, ConflictException::class, BadRequestException::class)
    override fun createCollection(newName: String): CollectionResource? {

        return write {
            val coll = file.model.createResource( "${file.uri}/$newName")
            if (file.model.containsResource(coll)) {
                //  LOGGER.warn("Could not create subfolder {}", newName)
                null
            } else {
                file.addProperty(FILE_CONTAINS, coll)
                coll[FILE_IS_DIRECTORY] = true
                coll[FILE_CREATED] = currentTimeMillis()
                coll[FILE_LAST_MODIFIED] = currentTimeMillis()
                MiltonFolderResource(coll, resourceFactory)
            }
        }
    }

    @Throws(ConflictException::class, NotAuthorizedException::class, BadRequestException::class)
    override fun moveTo(rDest: CollectionResource, name: String) {
        log.debug { "Moving folder $file to ${rDest.name}/$name" }

        rDest as MiltonFolderResource

        val target = file.model.createResource(rDest.file.uri + '/' + name)

        write {
            file.model.removeAll(target, null, null)

            file.listProperties().forEach {
                file.model.add(target, it.predicate, it.`object`)
            }

            file.model.listStatements(null, null, file).forEach {
                file.model.add(it.subject, it.predicate, target)
            }

            file.model.removeAll(file, null, null)
                    .removeAll(null, null, file)
                    .removeAll(null, FILE_CONTAINS, target)

            rDest.file.addProperty(FILE_CONTAINS, target)

            val targetFolder = MiltonFolderResource(target, resourceFactory)

            targetFolder.children.forEach {
                it.moveTo(targetFolder, it.name)
            }
        }
    }

    @Throws(IOException::class, ConflictException::class, NotAuthorizedException::class, BadRequestException::class)
    override fun createNew(newName: String, inputStream: InputStream, length: Long?, contentType: String?): Resource {
        val fileRoot = File("files")
        fileRoot.mkdirs()
        val newFile = File(fileRoot, randomUUID().toString())
        val resource = file.model.createResource(file.uri + '/' + newName)
        try {
            object : ExceptionAwareOutputStream(FileUtils.openOutputStream(newFile)) {
                @Throws(IOException::class)
                override fun close() {
                    super.close()

                    if (exception == null) {
                        write {
                            resource[FILE_LOCAL_PATH] = newFile.absolutePath
                            resource[FILE_IS_DIRECTORY] = false
                            resource[FILE_SIZE] = newFile.length()
                            resource[FILE_CREATED] = newFile.lastModified()
                            resource[FILE_LAST_MODIFIED] = newFile.lastModified()
                            resource[FILE_IS_READY] = true
                            file.addProperty(FILE_CONTAINS, resource)
                        }
                    } else {
                        newFile.delete()
                    }
                }
            }.use {
                IOUtils.copy(inputStream, it)
                return MiltonFileResource(resource, resourceFactory)
            }
        } catch (e: Exception) {
            LOGGER.error("Error creating file {}", newFile, e)
            throw RuntimeIoException(e)
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MiltonFolderResource::class.java)
    }
}