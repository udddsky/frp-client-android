package com.example.frpclient

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FrpClientScreen()
                }
            }
        }
    }
}

@Composable
fun FrpClientScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(FrpService.PREFS_NAME, Context.MODE_PRIVATE)

    var serverAddr by rememberSaveable { mutableStateOf("frp.example.com") }
    var serverPort by rememberSaveable { mutableStateOf("7000") }
    var token by rememberSaveable { mutableStateOf("your-token") }
    var localIp by rememberSaveable { mutableStateOf("127.0.0.1") }
    var localPort by rememberSaveable { mutableStateOf("22") }
    var remotePort by rememberSaveable { mutableStateOf("6000") }
    var frpVersion by rememberSaveable { mutableStateOf("0.52.3") }
    var running by remember { mutableStateOf(prefs.getBoolean(FrpService.KEY_RUNNING, false)) }
    var detail by remember { mutableStateOf(prefs.getString(FrpService.KEY_DETAIL, "未运行") ?: "未运行") }

    LaunchedEffect(Unit) {
        while (true) {
            running = prefs.getBoolean(FrpService.KEY_RUNNING, false)
            detail = prefs.getString(FrpService.KEY_DETAIL, "未运行") ?: "未运行"
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("FRP Android Client")
        Text("状态: ${if (running) "运行中" else "已停止"}")
        Text(detail)

        OutlinedTextField(
            value = serverAddr,
            onValueChange = { serverAddr = it },
            label = { Text("服务端地址") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = serverPort,
            onValueChange = { serverPort = it },
            label = { Text("服务端端口") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Token") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = localIp,
            onValueChange = { localIp = it },
            label = { Text("本地 IP") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = localPort,
            onValueChange = { localPort = it },
            label = { Text("本地端口") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = remotePort,
            onValueChange = { remotePort = it },
            label = { Text("远端端口") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = frpVersion,
            onValueChange = { frpVersion = it },
            label = { Text("FRP 版本") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("版本名会优先匹配 assets/frpc-<版本>，例如 assets/frpc-0.52.3；没有时回退到 assets/frpc")

        Button(onClick = {
            val config = FrpConfig(
                serverAddr = serverAddr,
                serverPort = serverPort.toIntOrNull() ?: 7000,
                token = token,
                localIp = localIp,
                localPort = localPort.toIntOrNull() ?: 22,
                remotePort = remotePort.toIntOrNull() ?: 6000,
                frpVersion = frpVersion.trim()
            )
            FrpService.start(context, config)
        }) {
            Text("启动")
        }

        Button(onClick = { FrpService.stop(context) }) {
            Text("停止")
        }
    }
}
