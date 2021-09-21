package com.gitlab.andrewkuryan.brownie.entity

sealed class BackendSession {
    abstract val publicKey: String
}

data class GuestSession(
        override val publicKey: String,
        val browserName: String,
        val osName: String,
) : BackendSession()

data class InitialSession(
        override val publicKey: String,
        val browserName: String,
        val osName: String,
) : BackendSession()

data class TempSession(
        override val publicKey: String,
        val browserName: String,
        val osName: String,
        val KHex: String,
) : BackendSession()

data class ActiveSession(
        override val publicKey: String,
        val browserName: String,
        val osName: String,
        val KHex: String,
) : BackendSession()