package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.*
import java.io.File
import java.io.InputStream

interface StorageApi {
    val userApi: UserStorageApi
    val contactApi: ContactStorageApi
    val postApi: PostStorageApi
    val categoryApi: CategoryStorageApi
    val tagApi: TagStorageApi
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
    suspend fun initNewPost(author: ActiveUser): InitializedPost
    suspend fun addPostTitle(post: InitializedPost, title: String): FillingPost
    suspend fun addPostParagraph(post: FillingPost, paragraph: Paragraph): FillingPost
    suspend fun addPostCategory(post: FillingPost, category: Category): CategorizingPost
    suspend fun changePostCategory(post: CategorizingPost, newCategory: Category): CategorizingPost
    suspend fun addPostTags(post: CategorizingPost, tags: List<Tag>): TaggablePost
    suspend fun replacePostTags(post: TaggablePost, newTags: List<Tag>): TaggablePost
    suspend fun completePost(post: TaggablePost): ActivePost

    suspend fun deletePost(post: Post): Post

    suspend fun getPostById(id: Int): Post?
    suspend fun getUserPostBlank(user: ActiveUser): NotCompletedPost?
}

interface CategoryStorageApi {
    suspend fun searchCategories(filter: Filter<Category>): List<Category>
}

interface TagStorageApi {
    suspend fun searchTags(filter: Filter<Tag>): List<Tag>
    suspend fun getTagByName(name: String): Tag?
}

interface FileStorageApi {
    suspend fun saveFile(inputStream: InputStream, format: StorageFileFormat): StorageFile
    suspend fun getFileById(id: Int): StorageFile?
    suspend fun getFileContentById(id: Int): File?
}