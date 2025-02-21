/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.resources.*
import kotlinx.serialization.*
import kotlin.test.*

class ResourcesTest {

    @Serializable
    @Resource("path/{id}/{method}")
    class Path(val id: Long, val method: String) {
        @Serializable
        @Resource("child/{path?}")
        data class Child(val parent: Path, val path: String, val query: List<Int>)
    }

    @Test
    fun testBuilders() = testWithEngine(MockEngine) {
        config {
            engine {
                addHandler { request ->
                    val uri = request.url.fullPath
                    val method = request.method.value
                    assertEquals(method, uri.split('/')[3])
                    assertEquals("/path/123/$method/child/value?query=1&query=2&query=3&query=4", uri)
                    respondOk(uri)
                }
            }
            install(Resources)
        }

        test { client ->
            val response1 = client.get(Path.Child(Path(123, "GET"), "value", listOf(1, 2, 3, 4))).bodyAsText()
            val response2 = client.post(Path.Child(Path(123, "POST"), "value", listOf(1, 2, 3, 4))).bodyAsText()
            val response3 = client.put(Path.Child(Path(123, "PUT"), "value", listOf(1, 2, 3, 4))).bodyAsText()
            val response4 = client.delete(Path.Child(Path(123, "DELETE"), "value", listOf(1, 2, 3, 4))).bodyAsText()
            val response5 = client.options(Path.Child(Path(123, "OPTIONS"), "value", listOf(1, 2, 3, 4))).bodyAsText()
            val response6 = client.head(Path.Child(Path(123, "HEAD"), "value", listOf(1, 2, 3, 4))).bodyAsText()
            val response7 = client.request(Path.Child(Path(123, "METHOD"), "value", listOf(1, 2, 3, 4))) {
                method = HttpMethod("METHOD")
            }.bodyAsText()
            assertEquals("/path/123/GET/child/value?query=1&query=2&query=3&query=4", response1)
            assertEquals("/path/123/POST/child/value?query=1&query=2&query=3&query=4", response2)
            assertEquals("/path/123/PUT/child/value?query=1&query=2&query=3&query=4", response3)
            assertEquals("/path/123/DELETE/child/value?query=1&query=2&query=3&query=4", response4)
            assertEquals("/path/123/OPTIONS/child/value?query=1&query=2&query=3&query=4", response5)
            assertEquals("/path/123/HEAD/child/value?query=1&query=2&query=3&query=4", response6)
            assertEquals("/path/123/METHOD/child/value?query=1&query=2&query=3&query=4", response7)

            client.prepareGet(Path.Child(Path(123, "GET"), "value", listOf(1, 2, 3, 4))).body { body: String ->
                assertEquals("/path/123/GET/child/value?query=1&query=2&query=3&query=4", body)
            }
            client.preparePost(Path.Child(Path(123, "POST"), "value", listOf(1, 2, 3, 4))).body { body: String ->
                assertEquals("/path/123/POST/child/value?query=1&query=2&query=3&query=4", body)
            }
            client.preparePut(Path.Child(Path(123, "PUT"), "value", listOf(1, 2, 3, 4))).body { body: String ->
                assertEquals("/path/123/PUT/child/value?query=1&query=2&query=3&query=4", body)
            }
            client.prepareDelete(Path.Child(Path(123, "DELETE"), "value", listOf(1, 2, 3, 4))).body { body: String ->
                assertEquals("/path/123/DELETE/child/value?query=1&query=2&query=3&query=4", body)
            }
            client.prepareOptions(Path.Child(Path(123, "OPTIONS"), "value", listOf(1, 2, 3, 4))).body { body: String ->
                assertEquals("/path/123/OPTIONS/child/value?query=1&query=2&query=3&query=4", body)
            }
            client.prepareHead(Path.Child(Path(123, "HEAD"), "value", listOf(1, 2, 3, 4))).body { body: String ->
                assertEquals("/path/123/HEAD/child/value?query=1&query=2&query=3&query=4", body)
            }
            client.prepareRequest(Path.Child(Path(123, "METHOD"), "value", listOf(1, 2, 3, 4))) {
                method = HttpMethod("METHOD")
            }.body { body: String ->
                assertEquals("/path/123/METHOD/child/value?query=1&query=2&query=3&query=4", body)
            }
        }
    }

    @Test
    fun testBuildersWithUrl() = testWithEngine(MockEngine) {
        config {
            engine {
                addHandler { request ->
                    val uri = request.url.fullPath
                    val method = request.method.value
                    assertEquals(method, uri.split('/')[3])
                    assertEquals("/path/123/$method/child/value?query=1&query=2&query=3&query=4", uri)
                    respondOk(uri)
                }
            }
            install(Resources)
        }

        test { client ->
            val response1 = client.get("/path/123/GET/child/value?query=1&query=2&query=3&query=4").bodyAsText()
            assertEquals("/path/123/GET/child/value?query=1&query=2&query=3&query=4", response1)
        }
    }

    @Serializable
    @Resource("path/{id}/{value?}")
    class PathWithDefault(val id: Boolean = true, val value: String? = null, val query1: Int?, val query2: Int? = 5)

    @Test
    fun testRequestWithDefaults() = testWithEngine(MockEngine) {
        config {
            engine {
                addHandler { request ->
                    val uri = request.url.fullPath
                    assertEquals("/path/true?query2=5", uri)
                    respondOk(uri)
                }
            }
            install(Resources)
        }

        test { client ->
            val response = client.get(PathWithDefault(query1 = null))
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
