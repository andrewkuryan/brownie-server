package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.*

interface StorageApi {
    val userApi: UserStorageApi
    val contactApi: ContactStorageApi
}

interface UserStorageApi {
    suspend fun getUserBySession(session: BackendSession): User?
    suspend fun getUserById(id: Int): User?

    suspend fun createNewGuest(session: BackendSession): GuestUser

    suspend fun addUserContact(oldUser: GuestUser, contact: UserContact): BlankUser
    suspend fun addUserContact(oldUser: ActiveUser, contact: UserContact): ActiveUser

    suspend fun fulfillUser(user: BlankUser, data: UserData): ActiveUser
    suspend fun updateUser(user: ActiveUser, newData: UserData): ActiveUser
}

interface ContactStorageApi {
    suspend fun createContact(contactData: ContactData): UnconfirmedUserContact
    suspend fun confirmContact(contact: UnconfirmedUserContact): ActiveUserContact
}