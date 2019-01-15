package io.fairspace.triton.webdav

import org.apache.commons.io.output.ProxyOutputStream
import java.io.IOException
import java.io.OutputStream

open class ExceptionAwareOutputStream(inner: OutputStream) : ProxyOutputStream(inner) {
    var exception: IOException? = null
        private set

    @Throws(IOException::class)
    override fun handleIOException(e: IOException) {
        exception = e
        super.handleIOException(e)
    }


}