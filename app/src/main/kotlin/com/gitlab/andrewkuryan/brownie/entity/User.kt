package com.gitlab.andrewkuryan.brownie.entity

import com.gitlab.andrewkuryan.brownie.BackendField

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
) : User()

data class BlankUser(
        override val id: Int,
        override val permissions: List<Permission>,
        val contact: UserContact,
) : User()

data class ActiveUser(
        override val id: Int,
        override val permissions: List<Permission>,
        val contacts: List<UserContact>,
        val data: UserData,
) : User()

data class UserData(
        val login: String?,
        @BackendField val passwordHash: String,
)