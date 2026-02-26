package com.example.aitemplate.client.di

import com.example.aitemplate.client.data.remote.AuthApi
import com.example.aitemplate.client.data.remote.ChatApi
import com.example.aitemplate.client.data.remote.ConversationApi
import com.example.aitemplate.client.data.remote.MetadataApi
import com.example.aitemplate.client.data.sse.SseClient
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            // JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            
            // Default request configuration
            install(DefaultRequest) {
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
            
            // Timeout configuration
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 10_000
            }
            
            // Handle 401 errors
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status == HttpStatusCode.Unauthorized) {
                        // Clear auth and let UI handle redirect
                        val settings = Settings()
                        settings.remove("access_token")
                        settings.remove("refresh_token")
                    }
                }
            }
        }
    }
    single { SseClient(get()) }
    single { MetadataApi(get()) }
    single { ChatApi(get()) }
    single { ConversationApi(get()) }
    single { AuthApi(get()) }
}
