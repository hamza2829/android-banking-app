package dev.pocketdemo

import dev.pocketdemo.data.MockApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException

class MockApiTest {
    @Test fun retrofitReadsSyntheticSnapshotOverRealLoopbackHttp() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"balanceCents":12345,"transactions":[]}"""))
            server.start(java.net.InetAddress.getByName("127.0.0.1"), 0); val snapshot = MockApi.create("http://127.0.0.1:${server.port}/").snapshot()
            assertEquals(12345L, snapshot.balanceCents); assertTrue(snapshot.transactions.isEmpty())
            val request = server.takeRequest(); assertEquals("/snapshot", request.path)
            assertNull(request.getHeader("Authorization"))
        }
    }
    @Test fun httpFailureIsNotPresentedAsSuccess() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503)); server.start(java.net.InetAddress.getByName("127.0.0.1"), 0)
            try { MockApi.create("http://127.0.0.1:${server.port}/").snapshot(); fail("Expected HTTP failure") }
            catch (e: HttpException) { assertEquals(503, e.code()) }
        }
    }
    @Test fun externalEndpointsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { MockApi.create("https://example.com/") }
    }
    @Test fun endpointCredentialsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { MockApi.create("http://demo:password@localhost/") }
    }
}
