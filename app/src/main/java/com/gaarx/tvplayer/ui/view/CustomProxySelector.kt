package com.gaarx.tvplayer.ui.view

import android.util.Log
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI


class CustomProxySelector(private val proxyHost: String, private val proxyPort: Int) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        return listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort)))
    }

    override fun connectFailed(uri: URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {
        Log.e("ProxySelector", "Connection failed: $uri", ioe)
    }
}