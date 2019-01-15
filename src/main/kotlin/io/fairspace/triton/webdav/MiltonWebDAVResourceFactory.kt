package io.fairspace.triton.webdav

import io.fairspace.triton.rdf.*
import io.milton.cache.LocalCacheManager
import io.milton.http.LockManager
import io.milton.http.ResourceFactory
import io.milton.http.SecurityManager
import io.milton.http.exceptions.BadRequestException
import io.milton.http.exceptions.NotAuthorizedException
import io.milton.http.fs.SimpleLockManager
import io.milton.http.fs.SimpleSecurityManager

/**
 * A resource factory for the [MiltonHandler]. Besides creating [MiltonFileResource]s and [ ]s, this class also holds a [SecurityManager] for authentication and an [ ].
 */
class MiltonWebDAVResourceFactory(val baseResource: org.apache.jena.rdf.model.Resource, credentials: Map<String, String>?) : ResourceFactory {
    val securityManager: SecurityManager?
    val lockManager: LockManager

    init {
        val cacheManager = LocalCacheManager()
        this.lockManager = SimpleLockManager(cacheManager)

        securityManager = if (credentials != null && !credentials.isEmpty()) {
            SimpleSecurityManager("", credentials)
        } else {
            null
        }

        val filesRoot = baseResource.model.createResource(baseResource.uri + "/files")
        filesRoot.write {
            if (!filesRoot.exists()) {
                filesRoot[FILE_IS_DIRECTORY] = true
            }
        }
    }

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun getResource(host: String, path: String): MetadataResource? =
            getResource(baseResource.model.createResource(baseResource.uri + path.removeSuffix("/")))

   fun getResource(file: org.apache.jena.rdf.model.Resource): MetadataResource? =
           baseResource.read {
               if (!file.exists()) {
                   null
               } else if (file[FILE_IS_DIRECTORY]) {
                   MiltonFolderResource(file, this)
               } else {
                   MiltonFileResource(file, this)
               }
           }
}