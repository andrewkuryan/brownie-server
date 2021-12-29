package com.gitlab.andrewkuryan.brownie.entity

import com.gitlab.andrewkuryan.brownie.BackendField
import java.math.BigInteger

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
    val publicItems: List<UserPublicItemType>,
) : User()

data class UserData(
    val login: String,
    @BackendField val credentials: UserCredentials,
)

data class UserCredentials(
    val salt: String,
    val verifier: BigInteger,
)

enum class UserPublicItemType {
    ID, LOGIN, CONTACTS
}

sealed class UserPublicItem {
    data class ID(val value: Int) : UserPublicItem()
    data class Login(val value: String) : UserPublicItem()
    data class Contacts(val value: List<ActiveUserContact>) : UserPublicItem()
}

fun ActiveUser.getPublicInfo() = publicItems.map {
    when (it) {
        UserPublicItemType.ID -> UserPublicItem.ID(id)
        UserPublicItemType.LOGIN -> UserPublicItem.Login(data.login)
        UserPublicItemType.CONTACTS -> UserPublicItem.Contacts(contacts.filterIsInstance<ActiveUserContact>())
    }
}