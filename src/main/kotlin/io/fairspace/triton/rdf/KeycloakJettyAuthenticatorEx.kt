package io.fairspace.triton.rdf

import org.eclipse.jetty.server.Request
import org.keycloak.adapters.jetty.KeycloakJettyAuthenticator
import javax.servlet.ServletRequest

class KeycloakJettyAuthenticatorEx : KeycloakJettyAuthenticator() {
    companion object {
        private val requestHolder = ThreadLocal<Request>()
        val currentRequest: Request
            get() = requestHolder.get()
    }

    override fun resolveRequest(req: ServletRequest?): Request =
            super.resolveRequest(req).also { requestHolder.set(it) }

}