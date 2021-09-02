package com.gitlab.andrewkuryan.brownie.entity

enum class Permission {
    READ_UPDATES,
}

sealed class User {
    abstract val id: Int
    abstract val permissions: List<Permission>
}

data class GuestUser(
    override val id: Int,
    override val permissions: List<Permission>,
): User()