package com.mooner.starlightdebugbridge

import android.os.Build
import com.mooner.starlight.plugincore.logger.Logger
import com.mooner.starlightdebugbridge.models.SocketData
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class Server {

    companion object {
        private const val T = "Milky Way"

        private const val TYPE_PASSWORD = "password"

        private const val RESPONSE_WRONG_PASSWORD = "INVALID_PASSWORD"
        private const val RESPONSE_ALREADY_CONNECTED = "ALREADY_CONNECTED"
        private const val RESPONSE_OK = "OK"
    }

    private var isConnected = false
    private var _server: NettyApplicationEngine? = null
    private val server: NettyApplicationEngine
        get() = _server!!

    private val serializer: ThreadLocal<Json> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ThreadLocal.withInitial {
            Json {
                isLenient = true
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        }
    } else {
        object : ThreadLocal<Json>() {
            override fun initialValue(): Json? {
                return Json {
                    isLenient = true
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }
            }
        }
    }
    private val json: Json
        get() = serializer.get()!!

    fun startServer(port: Int, password: String, pingPeriod: Long, timeout: Long) {
        _server = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriodMillis = pingPeriod
                timeoutMillis = timeout
            }

            routing {
                webSocket("/connect") {
                    if (isConnected) {
                        close(reason = CloseReason(
                            CloseReason.Codes.CANNOT_ACCEPT,
                            RESPONSE_ALREADY_CONNECTED
                        ))
                        return@webSocket
                    }
                    var isAuthenticated = false
                    isConnected = true

                    try {
                        Logger.i(T, "Receiving connection from ${call.request.origin.remoteHost}")
                        for (frame in incoming) {
                            frame as? Frame.Text ?: continue
                            val received = frame.readText()

                            if (!isAuthenticated) {
                                val data: SocketData = json.decodeFromString(received)
                                if (data.type == TYPE_PASSWORD && data.content == password) {
                                    isAuthenticated = true
                                    send(RESPONSE_OK)
                                    Logger.i(T, "${call.request.origin.remoteHost} connected")
                                } else {
                                    close(reason = CloseReason(
                                        CloseReason.Codes.CANNOT_ACCEPT,
                                        RESPONSE_WRONG_PASSWORD
                                    ))
                                    break
                                }
                            } else {
                                send("received: $received")
                                Logger.i(T, "Received socket data: $received")
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e(T, e.localizedMessage!!)
                    } finally {
                        isConnected = false
                        isAuthenticated = false
                        Logger.i(T, "Client disconnected")
                    }
                }
            }
        }
        server.start()
    }

    fun stopServer() {
        if (_server != null) {
            server.stop(1000, 5000)
        }
    }
}