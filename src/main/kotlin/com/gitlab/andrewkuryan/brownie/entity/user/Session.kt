package com.gitlab.andrewkuryan.brownie.entity.user

sealed class BackendSession {
    abstract val publicKey: String

    data class Guest(
        override val publicKey: String,
        val browserName: String,
        val osName: String,
    ) : BackendSession()

    data class Temp(
        override val publicKey: String,
        val browserName: String,
        val osName: String,
        val KHex: String,
    ) : BackendSession()

    data class Active(
        override val publicKey: String,
        val browserName: String,
        val osName: String,
    ) : BackendSession()
}