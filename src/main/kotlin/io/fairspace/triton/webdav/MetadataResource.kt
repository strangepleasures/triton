package io.fairspace.triton.webdav

import io.fairspace.triton.rdf.get
import io.fairspace.triton.rdf.tx
import io.milton.http.*
import io.milton.http.exceptions.BadRequestException
import io.milton.http.exceptions.LockedException
import io.milton.http.exceptions.NotAuthorizedException
import io.milton.http.exceptions.PreConditionFailedException
import io.milton.resource.*
import mu.KotlinLogging
import org.apache.jena.system.Txn.calculateRead
import org.apache.jena.system.Txn.calculateWrite
import java.net.URLDecoder
import java.util.*

abstract class MetadataResource(
        val file: org.apache.jena.rdf.model.Resource,
        protected val resourceFactory: MiltonWebDAVResourceFactory) : Resource, PropFindableResource, LockableResource, GetableResource, DeletableResource, CopyableResource, MoveableResource {
    protected val log = KotlinLogging.logger {}
    private val txn = tx(file)

    protected fun <T> read(action: () -> T): T = calculateRead(txn, action)
    protected fun <T> write(action: () -> T): T = calculateWrite(txn, action)

    override fun getUniqueId(): String {
        return file.uri
    }

    override fun getName(): String = URLDecoder.decode(file.uri.substringAfterLast("/"))


    override fun getCreateDate(): Date = Date(read { file.get<Long?>(FILE_CREATED) ?: 0 })

    override fun authenticate(user: String, password: String): Any {
        log.debug{"Authenticating user $user for resource $file"}

        return if (resourceFactory.securityManager != null) {
            resourceFactory.securityManager.authenticate(user, password)
        } else user
    }

    override fun authorise(request: Request, method: Request.Method, auth: Auth?): Boolean {
        if (auth != null) {
            log.debug { "Authorizing user ${auth.user} for resource $file" }
        }

        return resourceFactory.securityManager == null || resourceFactory.securityManager
                .authorise(request, method, auth, this)
    }

    override fun getRealm(): String {
        return uniqueId
    }

    override fun getModifiedDate(): Date = Date(read { file.get<Long?>(FILE_LAST_MODIFIED) ?: 0 })

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun checkRedirect(request: Request): String? {
        // no redirect
        return null
    }

    @Throws(NotAuthorizedException::class, PreConditionFailedException::class, LockedException::class)
    override fun lock(timeout: LockTimeout, lockInfo: LockInfo): LockResult? {
        log.debug { "Locking $file" }
        return resourceFactory.lockManager.lock(timeout, lockInfo, this)
    }

    @Throws(NotAuthorizedException::class, PreConditionFailedException::class)
    override fun refreshLock(token: String): LockResult? {
        log.debug { "Refreshing lock for $file" }
        return resourceFactory.lockManager.refresh(token, this)
    }

    @Throws(NotAuthorizedException::class, PreConditionFailedException::class)
    override fun unlock(tokenId: String) {
        log.debug { "Unlocking $file" }
        resourceFactory.lockManager.unlock(tokenId, this)
    }

    override fun getCurrentLock(): LockToken? {
        return resourceFactory.lockManager.getCurrentToken(this)
    }

    override fun getMaxAgeSeconds(auth: Auth?): Long? {
        return null
    }

}