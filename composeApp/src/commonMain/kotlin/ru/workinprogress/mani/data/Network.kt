package ru.workinprogress.mani.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import ru.workinprogress.feature.auth.AuthResource
import ru.workinprogress.feature.auth.RefreshTokenRequest
import ru.workinprogress.feature.auth.TokensResponse
import ru.workinprogress.feature.auth.data.TokenRepository
import ru.workinprogress.mani.BASE_URL
import ru.workinprogress.mani.REALM
import ru.workinprogress.mani.SECURE


val networkModule = module {
    single<HttpClient> {
        HttpClient {
            install(Resources)
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        println("HTTP Client: $message")
                    }
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Auth) {
                bearer {
                    realm = REALM

                    loadTokens {
                        get<TokenRepository>().getToken()
                    }
                    refreshTokens {
                        val tokenRepository = get<TokenRepository>()
                        val httpClient = get<HttpClient>()

                        val refreshToken = tokenRepository.getToken().refreshToken

                        if (refreshToken == null || refreshToken.isEmpty()) {
                            return@refreshTokens null
                        }

                        val response = httpClient.post(AuthResource.Refresh()) {
                            markAsRefreshTokenRequest()
                            setBody(RefreshTokenRequest(refreshToken.orEmpty()))
                        }

                        if (response.status == HttpStatusCode.Unauthorized) {
                            tokenRepository.set("", "")
                        }

                        val data = response.body<TokensResponse>()

                        tokenRepository.set(
                            accessToken = data.accessToken,
                            refreshToken = data.refreshToken
                        )
                        tokenRepository.getToken()
                    }
                }
            }
            defaultRequest {
                contentType(Json)
                url {
                    protocol = if (SECURE) URLProtocol.HTTPS else URLProtocol.HTTP
                    host = BASE_URL
//                    port = 8080
                }
            }
        }
    }
}
