package io.fairspace.triton.webdav

import io.fairspace.triton.rdf.get
import io.fairspace.triton.rdf.set
import io.milton.common.ContentTypeUtils.findAcceptableContentType
import io.milton.common.ContentTypeUtils.findContentTypes
import io.milton.http.FileItem
import io.milton.http.Range
import io.milton.http.exceptions.BadRequestException
import io.milton.http.exceptions.ConflictException
import io.milton.http.exceptions.NotAuthorizedException
import io.milton.http.exceptions.NotFoundException
import io.milton.resource.CollectionResource
import io.milton.resource.FileResource
import io.milton.resource.ReplaceableResource
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.copyFile
import org.apache.commons.io.IOUtils
import org.apache.mina.core.RuntimeIoException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * A [milton FileResource][FileResource] to serve a single file.
 */
class MiltonFileResource(file: org.apache.jena.rdf.model.Resource, resourceFactory: MiltonWebDAVResourceFactory) : MetadataResource(file, resourceFactory), FileResource, ReplaceableResource {

    @Throws(NotAuthorizedException::class, BadRequestException::class, ConflictException::class)
    override fun copyTo(toCollection: CollectionResource, name: String) {
        log.debug { "Copying $file to ${toCollection.name}/$name" }

        toCollection as MiltonFolderResource

        val target = file.model.createResource(toCollection.file.uri + '/' + name)

        write {
            file.model.removeAll(target, null, null)

            file.listProperties().forEach {
                file.model.add(target, it.predicate, it.`object`)
            }

            file.model.listStatements(null, null, file).forEach {
                file.model.add(it.subject, it.predicate, target)
            }
        }
    }

    @Throws(NotAuthorizedException::class, ConflictException::class, BadRequestException::class)
    override fun delete() {
        log.debug { "Deleting $file" }

        write {
            file.model.removeAll(file, null, null).removeAll(null, null, file)
        }
    }

    @Throws(IOException::class, NotAuthorizedException::class, BadRequestException::class, NotFoundException::class)
    override fun sendContent(out: OutputStream, range: Range?, params: Map<String, String>?, contentType: String?) {
        log.debug { "Sending contents for $file" }

        val localPath = read {
            file.getRequiredProperty(FILE_LOCAL_PATH).`object`.asLiteral().string
        }
        copyFile(File(localPath), out)
    }

    override fun getContentType(accepts: String?): String? {
        val mime = findContentTypes(this.name)
        val contentType = findAcceptableContentType(mime, accepts)

        log.debug { "Resolved content-type $contentType for $file" }

        return contentType

    }

    override fun getContentLength(): Long? {
        return read { file[FILE_SIZE] }
    }


    @Throws(ConflictException::class, NotAuthorizedException::class, BadRequestException::class)
    override fun moveTo(rDest: CollectionResource, name: String) {
        log.debug { "Moving $file to ${rDest.name}/$name" }

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
        }
    }

    override fun getCreateDate(): Date = Date(read {
        file.getProperty(FILE_CREATED)?.`object`?.asLiteral()?.long ?: 0
    })

    @Throws(BadRequestException::class, ConflictException::class, NotAuthorizedException::class)
    override fun replaceContent(inputStream: InputStream, length: Long?) {
        log.debug { "Replacing content of $file" }

        val fileRoot = File("files")
        fileRoot.mkdirs()
        val newFile = File(fileRoot, UUID.randomUUID().toString())

        try {
            object : ExceptionAwareOutputStream(FileUtils.openOutputStream(newFile)) {
                @Throws(IOException::class)
                override fun close() {
                    super.close()

                    if (exception == null) {
                        write {
                            file[FILE_LOCAL_PATH] = newFile.absolutePath
                            file[FILE_IS_DIRECTORY] = false
                            file[FILE_SIZE] = newFile.length()
                            file[FILE_LAST_MODIFIED] = newFile.lastModified()
                            file[FILE_IS_READY] = true
                        }
                    } else {
                        newFile.delete()
                    }
                }
            }.use {
                IOUtils.copy(inputStream, it)
            }
        } catch (e: Exception) {
            log.error(e) { "Error creating file $newFile" }
            throw RuntimeIoException(e)
        }
    }

    @Throws(BadRequestException::class, NotAuthorizedException::class, ConflictException::class)
    override fun processForm(parameters: Map<String, String>, files: Map<String, FileItem>): String? {
        return null
    }

}