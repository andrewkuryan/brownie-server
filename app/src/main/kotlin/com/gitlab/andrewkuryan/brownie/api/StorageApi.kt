package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.BackendSession
import com.gitlab.andrewkuryan.brownie.entity.GuestUser
import com.gitlab.andrewkuryan.brownie.entity.User

interface StorageApi {
    val userApi: UserStorageApi
}

interface UserStorageApi {
    suspend fun getUserBySession(session: BackendSession): User?
    suspend fun createNewGuest(session: BackendSession): GuestUser
}