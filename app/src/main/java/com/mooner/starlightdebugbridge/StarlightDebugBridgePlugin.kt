package com.mooner.starlightdebugbridge

import android.graphics.Color
import android.text.InputType
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import com.mooner.starlight.plugincore.language.ButtonConfigObject
import com.mooner.starlight.plugincore.language.ConfigObject
import com.mooner.starlight.plugincore.language.StringConfigObject
import com.mooner.starlight.plugincore.logger.Logger
import com.mooner.starlight.plugincore.plugin.StarlightPlugin
import com.mooner.starlight.plugincore.utils.NetworkUtil
import java.net.NetworkInterface
import java.net.SocketException

class StarlightDebugBridgePlugin: StarlightPlugin() {

    private val server = Server()
    private var isRunning: Boolean = false
    private var password = "1234"
    private var port = 8000
    private var pingPeriod: Long = 1000
    private var timeoutMillis: Long = 1000 * 15

    companion object {
        private const val T = "Milky Way"

        private const val KEY_PASSWORD = "password"
        private const val KEY_PORT = "port"
        private const val KEY_TIMEOUT = "timeoutMillis"
        private const val KEY_PING_PERIOD = "pingPeriod"
        private const val KEY_RESTART = "restartServer"
    }

    override val configObjects: List<ConfigObject> = listOf(
        StringConfigObject(
            id = KEY_PASSWORD,
            name = "접속 비밀번호",
            hint = "1234"
        ),
        StringConfigObject(
            id = KEY_PORT,
            name = "포트",
            hint = "8000",
            inputType = InputType.TYPE_CLASS_NUMBER,
            require = { text ->
                if (text.toIntOrNull() == null) {
                    "올바르지 않은 값입니다"
                } else if (text.toInt() !in 1024..49151) {
                    "포트는 1024~49151 사이의 값만 할당 가능합니다."
                } else null
            }
        ),
        StringConfigObject(
            id = KEY_TIMEOUT,
            name = "연결 시간 초과(ms)",
            hint = "15000",
            inputType = InputType.TYPE_CLASS_NUMBER,
            require = { text ->
                if (text.toIntOrNull() == null || text.toInt() < 0) {
                    "올바르지 않은 값입니다"
                } else null
            }
        ),
        StringConfigObject(
            id = KEY_PING_PERIOD,
            name = "포트",
            hint = "8000",
            inputType = InputType.TYPE_CLASS_NUMBER,
            require = { text ->
                if (text.toIntOrNull() == null) {
                    "올바르지 않은 값입니다"
                } else if (text.toInt() !in 1024..49151) {
                    "포트는 1024~49151 사이의 값만 할당 가능합니다."
                } else null
            }
        ),
        ButtonConfigObject(
            id = KEY_RESTART,
            name = "서버 재시작",
            onClickListener = {
                if (isRunning) {
                    server.stopServer()
                    isRunning = false
                    getPluginConfigs()
                    server.startServer(port, password, pingPeriod, timeoutMillis)
                    isRunning = true
                }
            },
            backgroundColorInt = Color.parseColor("#FBE7C6"),
            loadIcon = {
                val context = it.context
                val imageLoader = ImageLoader.Builder(context)
                    .componentRegistry {
                        add(SvgDecoder(context))
                    }
                    .build()
                Coil.setImageLoader(imageLoader)
                it.load(getAsset("images/reload.svg")) {
                    size(24, 24)
                }
            }
        )
    )

    override val name: String
        get() = T

    override fun onConfigUpdated(updated: Map<String, Any>) {
        for ((id, value) in updated) {
            when(id) {
                "password" -> {
                    this.password = value as String
                    Logger.i(T, "Password for Debug server changed successfully.")
                }
                "port" -> {
                    this.port = (value as String).toInt()
                    Logger.i(T, "Port for Debug server changed successfully.")
                }
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        if (getIpAddress().isBlank()) {
            Logger.i(T, "Waiting for valid network connection...")
            isRunning = false
            return
        }
        refreshConfig()
        try {
            server.startServer(port, password, pingPeriod, timeoutMillis)
            Logger.i(T, "Starting server...")
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.i(T, "Failed to start server: $e")
        }
        Logger.i(T, "Successfully started server on ${getIpAddress()}: $port")
        isRunning = true
    }

    override fun onDisable() {
        super.onDisable()
        server.stopServer()
        isRunning = false
    }

    override fun onNetworkStateChanged(state: Int) {
        super.onNetworkStateChanged(state)
        Logger.d(T, "Network state changed: $state")
        if (state in arrayOf(NetworkUtil.NETWORK_STATUS_ETHERNET, NetworkUtil.NETWORK_STATUS_WIFI)) {
            if (!isRunning) {
                Logger.d(T, "Resuming stopped DebugBridge server..")
                refreshConfig()
                server.startServer(port, password, pingPeriod, timeoutMillis)
                isRunning = true
                Logger.i(T, "Resumed DebugBridge server on ${getIpAddress()}: $port")
            }
        } else {
            if (isRunning) {
                Logger.d(T, "Pausing DebugBridge server..")
                server.stopServer()
                isRunning = false
                Logger.i(T, "Paused DebugBridge server")
            }
        }
    }

    private fun refreshConfig() {
        val configs = getPluginConfigs()
        password = (configs["password"] as String?)?: "1234"
        port = (configs["port"] as String?)?.toInt()?: 8000
        pingPeriod = (configs["pingPeriod"] as String?)?.toLong()?: 1000
        timeoutMillis = (configs["timeoutMillis"] as String?)?.toLong()?: 15000
    }

    private fun getIpAddress(): String {
        var ip = ""
        try {
            val enumNetworkInterfaces = NetworkInterface
                .getNetworkInterfaces()
            while (enumNetworkInterfaces.hasMoreElements()) {
                val networkInterface = enumNetworkInterfaces
                    .nextElement()
                val enumInetAddress = networkInterface
                    .inetAddresses
                while (enumInetAddress.hasMoreElements()) {
                    val inetAddress = enumInetAddress.nextElement()
                    if (inetAddress.isSiteLocalAddress) {
                        ip += inetAddress.hostAddress
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
            ip += """
            Something Wrong! $e
            
            """.trimIndent()
        }
        return ip
    }
}