package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.UserStorageApi
import com.gitlab.andrewkuryan.brownie.entity.user.*

internal val sessions = mutableMapOf<String, Pair<BackendSession, Int>>()
internal val users = mutableMapOf<Int, User>()

class UserMemoryStorageApi : UserStorageApi {

    var currentUserId = 0

    override suspend fun changeSessionOwner(
        session: BackendSession.Temp,
        newUser: User.Active,
        newSession: BackendSession.Active
    ): BackendSession.Active {
        if (sessions[session.publicKey] == null) {
            throw Exception("No such session")
        } else {
            sessions[session.publicKey] = newSession to newUser.id
        }
        return newSession
    }

    override suspend fun updateSession(
        oldSession: BackendSession.Guest,
        newSession: BackendSession.Active
    ): BackendSession.Active {
        if (sessions[oldSession.publicKey] == null) {
            throw Exception("No such session")
        } else {
            sessions[newSession.publicKey] = newSession to sessions.getValue(oldSession.publicKey).second
        }
        return newSession
    }

    override suspend fun updateSession(
        oldSession: BackendSession.Guest,
        newSession: BackendSession.Temp
    ): BackendSession.Temp {
        if (sessions[oldSession.publicKey] == null) {
            throw Exception("No such session")
        } else {
            sessions[newSession.publicKey] = newSession to sessions.getValue(oldSession.publicKey).second
        }
        return newSession
    }

    override suspend fun deleteSession(session: BackendSession): BackendSession {
        sessions.remove(session.publicKey)
        return session
    }

    override suspend fun getUserBySessionKey(publicKey: String): Pair<User, BackendSession>? {
        val session = sessions[publicKey]
        return if (session == null) {
            null
        } else {
            refreshUser(users.getValue(session.second)) to session.first
        }
    }

    override suspend fun getUserById(id: Int): User? {
        return refreshUser(users[id])
    }

    override suspend fun getUserByLogin(login: String): User.Active? {
        return refreshUser(users.values.filterIsInstance<User.Active>().find { it.data.login == login })
    }

    override suspend fun getUserByContact(contact: UserContact.Active): User.Active? {
        return users.values.filterIsInstance<User.Active>().map { refreshUser(it) }
            .find { it.contacts.any { c -> c.id == contact.id } }
    }

    private inline fun <reified T : User?> refreshUser(user: T): T {
        return when (user) {
            is User.Guest -> user
            is User.Blank -> user.copy(contact = contacts.entries.find { it.key == user.contact.id }!!.value) as T
            is User.Active -> user.copy(contacts = user.contacts
                .map { oldContact -> contacts.entries.find { it.key == oldContact.id }!!.value }) as T
            else -> user
        }
    }

    override suspend fun createNewGuest(session: BackendSession): User.Guest {
        val user = User.Guest(currentUserId, UserPermission.DEFAULT)
        users[currentUserId] = user
        sessions[session.publicKey] = session to user.id
        currentUserId += 1
        return user
    }

    override suspend fun addUserContact(oldUser: User.Guest, contact: UserContact): User.Blank {
        val newUser = User.Blank(oldUser.id, oldUser.permissions, contact)
        users[oldUser.id] = newUser
        return newUser
    }

    override suspend fun addUserContact(oldUser: User.Active, contact: UserContact): User.Active {
        val newUser = oldUser.copy(contacts = oldUser.contacts + contact)
        users[oldUser.id] = newUser
        return newUser
    }

    override suspend fun fulfillUser(user: User.Blank, data: UserData): User.Active {
        val newUser =
            User.Active(
                id = user.id,
                permissions = user.permissions,
                contacts = listOf(user.contact),
                data = data,
                publicItems = listOf(UserPublicItemType.ID, UserPublicItemType.LOGIN)
            )
        users[user.id] = newUser
        return newUser
    }

    override suspend fun updateUser(user: User.Active, newData: UserData): User.Active {
        val newUser = user.copy(data = newData)
        users[user.id] = newUser
        return newUser
    }

    override suspend fun deleteUser(user: User): User {
        users.remove(user.id)
        return user
    }

    override suspend fun getUserPublicInfo(id: Int): List<UserPublicItem>? {
        val user = users[id]
        return if (user != null && user is User.Active) {
            user.getPublicInfo()
        } else {
            null
        }
    }
}