package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.generateVerificationCode

class MemoryStorageApi : StorageApi {
    override val userApi = UserMemoryStorageApi()
    override val contactApi = ContactMemoryStorageApi()
}

val contacts = mutableMapOf<Int, UserContact>()
val users = mutableMapOf<Int, User>()
val sessions = mutableMapOf<String, Pair<BackendSession, Int>>()

class ContactMemoryStorageApi : ContactStorageApi {

    private var currentContactId = 0

    override suspend fun createContact(contactData: ContactData): UnconfirmedUserContact {
        val contact = UnconfirmedUserContact(currentContactId, contactData, generateVerificationCode())
        contacts[contact.id] = contact
        currentContactId += 1
        return contact
    }

    override suspend fun confirmContact(contact: UnconfirmedUserContact): ActiveUserContact {
        val newContact = ActiveUserContact(contact.id, contact.data)
        contacts[contact.id] = newContact
        return newContact
    }
}

class UserMemoryStorageApi : UserStorageApi {

    private var currentUserId = 0

    override suspend fun getUserBySession(session: BackendSession): User? {
        return refreshUser(users[sessions[session.publicKey]?.second])
    }

    override suspend fun getUserById(id: Int): User? {
        return refreshUser(users[id])
    }

    fun refreshUser(user: User?): User? {
        return when (user) {
            is GuestUser -> user
            is BlankUser -> user.copy(contact = contacts.entries.find { it.key == user.contact.id }!!.value)
            is ActiveUser -> user.copy(contacts = user.contacts
                    .map { oldContact -> contacts.entries.find { it.key == oldContact.id }!!.value })
            null -> null
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
        val newUser = ActiveUser(id = user.id, permissions = user.permissions, contacts = listOf(user.contact), data = data)
        users[user.id] = newUser
        return newUser
    }

    override suspend fun updateUser(user: ActiveUser, newData: UserData): ActiveUser {
        val newUser = user.copy(data = newData)
        users[user.id] = newUser
        return newUser
    }
}