package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.*
import java.io.File
import java.io.InputStream

interface StorageApi {
    val userApi: UserStorageApi
    val contactApi: ContactStorageApi
    val postApi: PostStorageApi
    val fileApi: FileStorageApi
}

interface UserStorageApi {
    suspend fun getUserBySessionKey(publicKey: String): Pair<User, BackendSession>?
    suspend fun getUserById(id: Int): User?
    suspend fun getUserByLogin(login: String): ActiveUser?
    suspend fun getUserByContact(contact: ActiveUserContact): ActiveUser?
    suspend fun getUserPublicInfo(id: Int): List<UserPublicItem>?

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

    suspend fun getContactByUniqueKey(uniqueKey: ContactUniqueKey): ActiveUserContact?
}

interface PostStorageApi {
    suspend fun initNewPost(author: ActiveUser): PostBlank.Initialized
    suspend fun addPostTitle(post: PostBlank.Initialized, title: String): PostBlank.Filling
    suspend fun addPostParagraph(post: PostBlank.Filling, paragraph: Paragraph): PostBlank.Filling
    suspend fun completePost(post: PostBlank.Filling): Post

    suspend fun deletePost(post: PostBlank): PostBlank
    suspend fun deletePost(post: Post): Post

    suspend fun getPostById(id: Int): Post?
    suspend fun getUserPostBlank(user: ActiveUser): PostBlank?
}

interface FileStorageApi {
    suspend fun saveFile(inputStream: InputStream, format: StorageFileFormat): StorageFile
    suspend fun getFileById(id: Int): StorageFile?
    suspend fun getFileContentById(id: Int): File?
}