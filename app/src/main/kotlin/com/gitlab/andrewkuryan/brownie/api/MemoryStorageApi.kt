package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.generateVerificationCode

class MemoryStorageApi : StorageApi {
    override val userApi = UserMemoryStorageApi()
    override val contactApi = ContactMemoryStorageApi()
}

class ContactMemoryStorageApi : ContactStorageApi {

    private var currentContactId = 0
    private val contacts = mutableMapOf<Int, UserContact>()

    override suspend fun createContact(contactData: ContactData): UnconfirmedUserContact {
        val contact = UnconfirmedUserContact(currentContactId, contactData, generateVerificationCode())
        contacts[contact.id] = contact
        currentContactId += 1
        return contact
    }

    override suspend fun confirmContact(contact: UnconfirmedUserContact): ActiveUserContact {
        contacts[contact.id] = contact
        return ActiveUserContact(contact.id, contact.data)
    }
}

class UserMemoryStorageApi : UserStorageApi {

    private var currentUserId = 0
    private val users = mutableMapOf<String, Pair<User, BackendSession>>()

    override suspend fun getUserBySession(session: BackendSession): User? {
        return users[session.publicKey]?.first
    }

    override suspend fun getUserById(id: Int): User? {
        return users.values.find { it.first.id == id }?.first
    }

    override suspend fun createNewGuest(session: BackendSession): GuestUser {
        val user = GuestUser(currentUserId, listOf())
        currentUserId += 1
        users[session.publicKey] = user to session
        return user
    }

    override suspend fun addUserContact(oldUser: GuestUser, contact: UserContact): BlankUser {
        val key = users.values.find { it.first.id == oldUser.id }?.second?.publicKey
        if (key != null) {
            val newUser = BlankUser(oldUser.id, oldUser.permissions, contact)
            users[key] = newUser to users.getValue(key).second
            return newUser
        } else {
            throw Exception("No such user: ${oldUser.id}")
        }
    }

    override suspend fun addUserContact(oldUser: ActiveUser, contact: UserContact): ActiveUser {
        val key = users.values.find { it.first.id == oldUser.id }?.second?.publicKey
        if (key != null) {
            val newUser = oldUser.copy(contacts = oldUser.contacts + contact)
            users[key] = newUser to users.getValue(key).second
            return newUser
        } else {
            throw Exception("No such user: ${oldUser.id}")
        }
    }

    override suspend fun fulfillUser(user: BlankUser, data: UserData): ActiveUser {
        val key = users.values.find { it.first.id == user.id }?.second?.publicKey
        if (key != null) {
            val newUser = ActiveUser(id = user.id, permissions = user.permissions, contacts = listOf(user.contact), data = data)
            users[key] = newUser to users.getValue(key).second
            return newUser
        } else {
            throw Exception("No such user: ${user.id}")
        }
    }

    override suspend fun updateUser(user: ActiveUser, newData: UserData): ActiveUser {
        val key = users.values.find { it.first.id == user.id }?.second?.publicKey
        if (key != null) {
            val newUser = user.copy(data = newData)
            users[key] = newUser to users.getValue(key).second
            return newUser
        } else {
            throw Exception("No such user: ${user.id}")
        }
    }
}