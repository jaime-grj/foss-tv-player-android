package com.gaarx.tvplayer

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TVPlayerAppTest {

    private lateinit var app: TVPlayerApp

    @Before
    fun setUp() {
        app = TVPlayerApp()
    }

    @Test
    fun onCreate_initializesThreeTenAbp() {
        val app = TVPlayerApp()
        app.onCreate()
    }
}
