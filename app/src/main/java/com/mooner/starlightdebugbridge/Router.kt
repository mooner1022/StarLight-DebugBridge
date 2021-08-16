package com.mooner.starlightdebugbridge
/*
import com.mooner.starlight.plugincore.core.Session
import com.mooner.starlight.plugincore.logger.Logger
import com.mooner.starlight.plugincore.project.ProjectConfig
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class Router(
    private val plugin: StarlightDebugBridgePlugin
) {
    private var connectedIP: String? = null
    private var server: NettyApplicationEngine? = null

    fun getConnectedIP(): String? = connectedIP

    companion object {
        private val T = "Milky Way"
    }

    fun startServer(authKey: String) {
        server = embeddedServer(Netty, port = 8080) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                    }
                )
            }
            routing {
                post("/connect") {
                    val params = call.receiveParameters()
                    val key = params["key"]
                    if (key == null) {
                        call.respond(HttpStatusCode.BadRequest,"Required parameter 'key' not provided")
                        return@post
                    }
                    if (authKey != key) {
                        call.respond(HttpStatusCode.Unauthorized, "Key not equal")
                        return@post
                    }
                    connectedIP = call.request.origin.remoteHost
                    Logger.i(T, "Connection attempt from $connectedIP accepted")
                    call.respond(HttpStatusCode.OK)
                }
                post("/disconnect") {
                    if (connectedIP == null) {
                        call.respond(HttpStatusCode.BadRequest, "Requested before connect")
                        return@post
                    }
                    val params = call.receiveParameters()
                    val key = params["key"]
                    if (key == null) {
                        call.respond(HttpStatusCode.BadRequest,"Required parameter 'key' not provided")
                        return@post
                    }
                    if (authKey != key) {
                        call.respond(HttpStatusCode.Unauthorized, "Key not equal")
                        return@post
                    }
                    connectedIP = null
                    Logger.i(T, "Connection destroyed: disconnected")
                    call.respond(HttpStatusCode.OK)
                }
                get("/projects") {
                    if (!sanityCheck(call, listOf())) return@get
                    val projectLoader = Session.projectLoader
                    call.respond(HttpStatusCode.OK, Json.encodeToString(projectLoader.getProjects().map { it.config.name }))
                }
                get("/projects/{projectName}/{requestType}") {
                    if (!sanityCheck(call, listOf())) return@get
                    val projectName = call.parameters["projectName"]
                    if (projectName == null) {
                        call.respond(HttpStatusCode.BadRequest, "Required parameter 'projectName' not provided")
                        return@get
                    }
                    val project = Session.projectLoader.getProject(projectName)
                    if (project == null) {
                        call.respond(HttpStatusCode.BadRequest, "Cannot find project '$projectName'")
                        return@get
                    }
                    when(val requestType = call.parameters["requestType"]) {
                        null -> call.respond(HttpStatusCode.BadRequest, "Required parameter '$requestType' not provided")
                        "code" -> {
                            call.respond(HttpStatusCode.OK, File(project.folder,project.config.mainScript).readText())
                        }
                        "config" -> {
                            call.respond(HttpStatusCode.OK, File(project.folder,"project.json").readText())
                        }
                    }
                }
                put("/projects/{projectName}/{requestType}") {
                    if (!sanityCheck(call, listOf())) return@put
                    val projectName = call.parameters["projectName"]
                    if (projectName == null) {
                        call.respond(HttpStatusCode.BadRequest, "Required parameter 'projectName' not provided")
                        return@put
                    }
                    val project = Session.projectLoader.getProject(projectName)
                    if (project == null) {
                        call.respond(HttpStatusCode.BadRequest, "Cannot find project '$projectName'")
                        return@put
                    }
                    when(val requestType = call.parameters["requestType"]) {
                        null -> call.respond(HttpStatusCode.BadRequest, "Required parameter '$requestType' not provided")
                        "code" -> {
                            File(project.folder,project.config.mainScript).writeText(call.receiveText())
                            call.respond(HttpStatusCode.OK)
                        }
                        "config" -> {
                            try {
                                Json.decodeFromString<ProjectConfig>(call.receiveText())
                            } catch (e: Exception) {
                                call.respond(HttpStatusCode.BadRequest, "Illegal raw string")
                                return@put
                            }
                            File(project.folder,"project.json").writeText(call.receiveText())
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }
        }
        server!!.start()
    }

    fun stopServer() {
        server!!.stop(1000, 2000)
    }

    private suspend fun sanityCheck(call: ApplicationCall, requiredArgs: List<String>): Boolean {
        if (connectedIP == null) {
            call.respond(HttpStatusCode.BadRequest, "Requested before connect")
            return false
        }
        val requestIP = call.request.origin.remoteHost
        if (requestIP != connectedIP) {
            call.respond(HttpStatusCode.Unauthorized, "Request from unknown ip")
            return false
        }
        val params = call.request.queryParameters
        for (req in requiredArgs) {
            if (!params.contains(req)) {
                call.respond(HttpStatusCode.BadRequest, "Required parameter '$req' not provided")
                return false
            }
        }
        return true
    }
}
*/