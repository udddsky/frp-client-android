package com.example.frpclient

import java.io.Serializable

data class FrpConfig(
    val serverAddr: String,
    val serverPort: Int,
    val token: String,
    val localIp: String,
    val localPort: Int,
    val remotePort: Int,
    val protocol: String = "tcp",
    val frpVersion: String = "latest"
) : Serializable
