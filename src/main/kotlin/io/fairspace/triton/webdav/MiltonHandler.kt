package io.fairspace.triton.webdav

import io.milton.http.HttpManager
import io.milton.servlet.MiltonServlet
import io.milton.servlet.ServletRequest
import io.milton.servlet.ServletResponse
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException

/**
 * A jetty handler to serve all request using a [milton HttpManager][HttpManager].
 */
class MiltonHandler(private val httpManager: HttpManager) : AbstractHandler() {

    @Throws(IOException::class, ServletException::class)
    override fun handle(target: String, baseRequest: Request, request: HttpServletRequest,
                        response: HttpServletResponse) {
        if (!target.startsWith("/files")) {
            return
        }
        val miltonRequest = ServletRequest(request, null)
        val miltonResponse = ServletResponse(response)

        try {
            MiltonServlet.setThreadlocals(request, response)
            httpManager.process(miltonRequest, miltonResponse)
        } finally {
            MiltonServlet.clearThreadlocals()
            response.outputStream.flush()
            response.flushBuffer()
        }
    }
}