package com.gitlab.andrewkuryan.brownie.entity

data class BackendSession(
    val publicKey: String,
    val browserName: String,
    val osName: String,
)