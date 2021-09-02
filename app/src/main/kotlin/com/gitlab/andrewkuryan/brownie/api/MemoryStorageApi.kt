package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.BackendSession
import com.gitlab.andrewkuryan.brownie.entity.GuestUser
import com.gitlab.andrewkuryan.brownie.entity.User

class MemoryStorageApi : StorageApi {
    override val userApi = UserMemoryStorageApi()
}

class UserMemoryStorageApi : UserStorageApi {

    private var currentId = 0
    private val users = mutableMapOf<String, Pair<User, BackendSession>>()

    override suspend fun getUserBySession(session: BackendSession): User? {
        return users[session.publicKey]?.first
    }

    override suspend fun createNewGuest(session: BackendSession): GuestUser {
        val user = GuestUser(currentId, listOf())
        currentId += 1
        users[session.publicKey] = user to session
        return user
    }
}