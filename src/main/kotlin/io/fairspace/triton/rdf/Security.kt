package io.fairspace.triton.rdf

import org.apache.jena.graph.Node
import org.apache.jena.graph.Triple
import org.apache.jena.permissions.SecurityEvaluator


class Security(private val fixedPrincipal: String?) : SecurityEvaluator {
    override fun getPrincipal(): Any? {
        return fixedPrincipal ?: KeycloakJettyAuthenticatorEx.currentRequest.userPrincipal
    }

    override fun evaluateAny(principal: Any?, actions: MutableSet<SecurityEvaluator.Action>?, graphIRI: Node?): Boolean {
        return true
    }

    override fun evaluateAny(principal: Any?, actions: MutableSet<SecurityEvaluator.Action>?, graphIRI: Node?, triple: Triple?): Boolean {
        return true
    }

    override fun evaluate(principal: Any?, action: SecurityEvaluator.Action?, graphIRI: Node?): Boolean {
        return true
    }

    override fun evaluate(principal: Any?, action: SecurityEvaluator.Action?, graphIRI: Node?, triple: Triple?): Boolean {
        return true
    }

    override fun evaluate(principal: Any?, actions: MutableSet<SecurityEvaluator.Action>?, graphIRI: Node?): Boolean {
        return true
    }

    override fun evaluate(principal: Any?, actions: MutableSet<SecurityEvaluator.Action>?, graphIRI: Node?, triple: Triple?): Boolean {
        return true
    }

    override fun evaluateUpdate(principal: Any?, graphIRI: Node?, from: Triple?, to: Triple?): Boolean {
        return true
    }

    override fun isPrincipalAuthenticated(principal: Any?): Boolean {
        return true
    }
}