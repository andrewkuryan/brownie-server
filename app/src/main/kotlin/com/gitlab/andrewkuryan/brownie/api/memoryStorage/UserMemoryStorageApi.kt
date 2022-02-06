package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.UserStorageApi
import com.gitlab.andrewkuryan.brownie.entity.*

internal val sessions = mutableMapOf<String, Pair<BackendSession, Int>>()
internal val users = mutableMapOf<Int, User>()

class UserMemoryStorageApi : UserStorageApi {

    var currentUserId = 0

    override suspend fun changeSessionOwner(
        session: TempSession,
        newUser: ActiveUser,
        newSession: ActiveSession
    ): ActiveSession {
        if (sessions[session.publicKey] == null) {
            throw Exception("No such session")
        } else {
            sessions[session.publicKey] = newSession to newUser.id
        }
        return newSession
    }

    override suspend fun updateSession(oldSession: GuestSession, newSession: ActiveSession): ActiveSession {
        if (sessions[oldSession.publicKey] == null) {
            throw Exception("No such session")
        } else {
            sessions[newSession.publicKey] = newSession to sessions.getValue(oldSession.publicKey).second
        }
        return newSession
    }

    override suspend fun updateSession(oldSession: GuestSession, newSession: TempSession): TempSession {
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

    override suspend fun getUserByLogin(login: String): ActiveUser? {
        return refreshUser(users.values.filterIsInstance<ActiveUser>().find { it.data.login == login })
    }

    override suspend fun getUserByContact(contact: ActiveUserContact): ActiveUser? {
        return users.values.filterIsInstance<ActiveUser>().map { refreshUser(it) }
            .find { it.contacts.any { c -> c.id == contact.id } }
    }

    private inline fun <reified T : User?> refreshUser(user: T): T {
        return when (user) {
            is GuestUser -> user
            is BlankUser -> user.copy(contact = contacts.entries.find { it.key == user.contact.id }!!.value) as T
            is ActiveUser -> user.copy(contacts = user.contacts
                .map { oldContact -> contacts.entries.find { it.key == oldContact.id }!!.value }) as T
            else -> user
        }
    }

    override suspend fun createNewGuest(session: BackendSession): GuestUser {
        val user = GuestUser(currentUserId, listOf())
        users[currentUserId] = user
        sessions[session.publicKey] = session to user.id
        currentUserId += 1
        return user
    }

    override suspend fun addUserContact(oldUser: GuestUser, contact: UserContact): BlankUser {
        val newUser = BlankUser(oldUser.id, oldUser.permissions, contact)
        users[oldUser.id] = newUser
        return newUser
    }

    override suspend fun addUserContact(oldUser: ActiveUser, contact: UserContact): ActiveUser {
        val newUser = oldUser.copy(contacts = oldUser.contacts + contact)
        users[oldUser.id] = newUser
        return newUser
    }

    override suspend fun fulfillUser(user: BlankUser, data: UserData): ActiveUser {
        val newUser =
            ActiveUser(
                id = user.id,
                permissions = user.permissions,
                contacts = listOf(user.contact),
                data = data,
                publicItems = listOf(UserPublicItemType.ID, UserPublicItemType.LOGIN)
            )
        users[user.id] = newUser
        return newUser
    }

    override suspend fun updateUser(user: ActiveUser, newData: UserData): ActiveUser {
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
        return if (user != null && user is ActiveUser) {
            user.getPublicInfo()
        } else {
            null
        }
    }
}