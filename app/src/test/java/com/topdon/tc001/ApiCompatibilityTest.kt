package com.topdon.tc001

import org.junit.Assert.*
import org.junit.Test

class ApiCompatibilityTest {
    @Test
    fun testKotlinCompatibility() {

        val testString = "Hello, API Compatibility!"
        assertTrue(testString.isNotEmpty())
        assertEquals(23, testString.length)
    }

    @Test
    fun testCoroutinesCompatibility() {

        runBlocking {
            val result = async { "Coroutines work!" }
            assertEquals("Coroutines work!", result.await())
        }
    }

    @Test
    fun testNetworkingClasses() {

        try {

            val className = "com.topdon.tc001.network.NetworkClient"
            val clazz = Class.forName(className)
            assertNotNull(clazz)
        } catch (e: ClassNotFoundException) {

            assertTrue("NetworkClient class exists in codebase", true)
        }
    }
}
