package com.gitlab.andrewkuryan.brownie.entity.user

import com.gitlab.andrewkuryan.brownie.BackendField
import java.math.BigInteger

sealed class UserPermission {
    sealed class TopLevel : UserPermission()
    sealed class Secondary(val parent: UserPermission) : UserPermission()

    object BrowseOwnPosts : TopLevel()
    object BrowseAllPosts : Secondary(BrowseOwnPosts)

    object CreatePosts : TopLevel()

    companion object {
        val DEFAULT = listOf(BrowseOwnPosts, CreatePosts)
    }
}

sealed class User {
    abstract val id: Int
    abstract val permissions: List<UserPermission>

    data class Guest(
        override val id: Int,
        override val permissions: List<UserPermission>,
    ) : User()

    data class Blank(
        override val id: Int,
        override val permissions: List<UserPermission>,
        val contact: UserContact,
    ) : User()

    data class Active(
        override val id: Int,
        override val permissions: List<UserPermission>,
        val contacts: List<UserContact>,
        val data: UserData,
        val publicItems: List<UserPublicItemType>,
    ) : User()
}

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
    data class Contacts(val value: List<UserContact.Active>) : UserPublicItem()
}

fun User.Active.getPublicInfo() = publicItems.map {
    when (it) {
        UserPublicItemType.ID -> UserPublicItem.ID(id)
        UserPublicItemType.LOGIN -> UserPublicItem.Login(data.login)
        UserPublicItemType.CONTACTS -> UserPublicItem.Contacts(contacts.filterIsInstance<UserContact.Active>())
    }
}