package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.*

interface StorageApi {
    val userApi: UserStorageApi
    val contactApi: ContactStorageApi
}

interface UserStorageApi {
    suspend fun getUserBySessionKey(publicKey: String): Pair<User, BackendSession>?
    suspend fun getUserById(id: Int): User?
    suspend fun getUserByLogin(login: String): ActiveUser?

    suspend fun changeSessionOwner(session: TempSession, newUser: ActiveUser, newSession: ActiveSession): ActiveSession
    suspend fun updateSession(oldSession: GuestSession, newSession: ActiveSession): ActiveSession
    suspend fun updateSession(oldSession: GuestSession, newSession: TempSession): TempSession
    suspend fun deleteSession(session: BackendSession): BackendSession

    suspend fun createNewGuest(session: BackendSession): GuestUser

    suspend fun addUserContact(oldUser: GuestUser, contact: UserContact): BlankUser
    suspend fun addUserContact(oldUser: ActiveUser, contact: UserContact): ActiveUser

    suspend fun fulfillUser(user: BlankUser, data: UserData): ActiveUser
    suspend fun updateUser(user: ActiveUser, newData: UserData): ActiveUser

    suspend fun deleteUser(user: User): User
}

interface ContactStorageApi {
    suspend fun createContact(contactData: ContactData): UnconfirmedUserContact
    suspend fun confirmContact(contact: UnconfirmedUserContact): ActiveUserContact
    suspend fun regenerateVerificationCode(contact: UnconfirmedUserContact): UnconfirmedUserContact
}