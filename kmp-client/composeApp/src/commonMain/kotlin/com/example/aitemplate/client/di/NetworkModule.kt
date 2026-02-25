package com.example.aitemplate.client.di

import com.example.aitemplate.client.data.remote.ChatApi
import com.example.aitemplate.client.data.remote.ConversationApi
import com.example.aitemplate.client.data.remote.MetadataApi
import com.example.aitemplate.client.data.sse.SseClient
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 10_000
            }
        }
    }
    single { SseClient(get()) }
    single { MetadataApi(get()) }
    single { ChatApi(get()) }
    single { ConversationApi(get()) }
}
