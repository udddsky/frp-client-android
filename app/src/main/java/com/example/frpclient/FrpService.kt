package com.example.frpclient

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable

class FrpService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var process: Process? = null
    private var outputJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = intent?.getSerializableExtra(EXTRA_CONFIG) as? FrpConfig
        if (config == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(1, buildNotification("正在启动 FRP"))
        startProcess(config)
        return START_STICKY
    }

    override fun onDestroy() {
        outputJob?.cancel()
        process?.destroy()
        super.onDestroy()
    }

    private fun startProcess(config: FrpConfig) {
        val workingDir = filesDir
        val configFile = File(workingDir, "frpc.toml")
        val logFile = File(workingDir, "frpc.log")
        val executable = prepareExecutable(config)

        if (executable == null) {
            writeState(false, "未找到 frpc 二进制，请将对应版本文件放入 app/assets/，例如 frpc-0.52.3 或 frpc")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        configFile.writeText(buildConfigString(config))
        logFile.writeText("")
        writeState(true, "正在启动")

        val processBuilder = ProcessBuilder(executable.absolutePath, "-c", configFile.absolutePath)
        processBuilder.directory(workingDir)
        process = processBuilder.start()

        outputJob = serviceScope.launch {
            process?.inputStream?.bufferedReader()?.useLines { lines ->
                lines.forEach { line ->
                    logFile.appendText("$line\n")
                    writeState(true, line)
                }
            }
            val exitCode = process?.waitFor() ?: -1
            writeState(false, "FRP 已退出，退出码: $exitCode")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildConfigString(config: FrpConfig): String {
        return """
            [common]
            server_addr = "${config.serverAddr}"
            server_port = ${config.serverPort}
            token = "${config.token}"
            protocol = "${config.protocol}"

            [ssh]
            type = "tcp"
            local_ip = "${config.localIp}"
            local_port = ${config.localPort}
            remote_port = ${config.remotePort}
        """.trimIndent()
    }

    private fun buildNotification(content: String): android.app.Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FRP Client")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FRP Client",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun prepareExecutable(config: FrpConfig): File? {
        val dir = File(filesDir, "bin")
        dir.mkdirs()

        val version = config.frpVersion.trim()
        val preferredNames = mutableListOf<String>()
        if (version.isNotEmpty()) {
            preferredNames.add("frpc-$version")
            preferredNames.add("frpc_$version")
        }
        preferredNames.add("frpc")

        preferredNames.forEach { fileName ->
            val target = File(dir, fileName)
            if (target.exists()) {
                return target
            }
        }

        val assetNames = mutableListOf<String>()
        if (version.isNotEmpty()) {
            assetNames.add("frpc-$version")
            assetNames.add("frpc_$version")
        }
        assetNames.add("frpc")

        assetNames.forEach { assetName ->
            val target = File(dir, assetName)
            try {
                val inputStream = assets.open(assetName)
                target.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                target.setExecutable(true)
                return target
            } catch (_: Exception) {
                // 继续尝试下一个候选文件
            }
        }

        return null
    }

    private fun writeState(isRunning: Boolean, detail: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_RUNNING, isRunning)
            .putString(KEY_DETAIL, detail)
            .apply()
    }

    companion object {
        const val EXTRA_CONFIG = "extra_config"
        const val PREFS_NAME = "frp_state"
        const val KEY_RUNNING = "running"
        const val KEY_DETAIL = "detail"
        const val CHANNEL_ID = "frp_service"

        fun start(context: Context, config: FrpConfig) {
            val intent = Intent(context, FrpService::class.java).apply {
                putExtra(EXTRA_CONFIG, config as Serializable)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FrpService::class.java))
        }
    }
}
