package dev.pocketdemo.data

import dev.pocketdemo.domain.Transaction
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class MockSnapshot(val balanceCents: Long, val transactions: List<Transaction>)
interface MockBankApi { @GET("snapshot") suspend fun snapshot(): MockSnapshot }
object MockApi {
    // No arbitrary endpoint configuration, real host, token, or app startup request.
    fun create(baseUrl: String): MockBankApi {
        val url = baseUrl.toHttpUrl()
        require(url.host in setOf("127.0.0.1", "localhost")) { "Only loopback mock servers are supported." }
        require(url.username.isEmpty() && url.password.isEmpty())
        return Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).build()
            .create(MockBankApi::class.java)
    }
    suspend fun importIntoEmptyDatabase(api: MockBankApi, repository: LocalBankRepository) {
        val snapshot = api.snapshot()
        repository.seedIfEmpty(snapshot.balanceCents, snapshot.transactions)
    }
}
