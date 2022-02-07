package com.gitlab.andrewkuryan.brownie.api

import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.entity.post.*
import com.gitlab.andrewkuryan.brownie.entity.user.*
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
    suspend fun getUserByLogin(login: String): User.Active?
    suspend fun getUserByContact(contact: UserContact.Active): User.Active?
    suspend fun getUserPublicInfo(id: Int): List<UserPublicItem>?

    suspend fun changeSessionOwner(
        session: BackendSession.Temp,
        newUser: User.Active,
        newSession: BackendSession.Active
    ): BackendSession.Active

    suspend fun updateSession(
        oldSession: BackendSession.Guest,
        newSession: BackendSession.Active
    ): BackendSession.Active

    suspend fun updateSession(oldSession: BackendSession.Guest, newSession: BackendSession.Temp): BackendSession.Temp
    suspend fun deleteSession(session: BackendSession): BackendSession

    suspend fun createNewGuest(session: BackendSession): User.Guest

    suspend fun addUserContact(oldUser: User.Guest, contact: UserContact): User.Blank
    suspend fun addUserContact(oldUser: User.Active, contact: UserContact): User.Active

    suspend fun fulfillUser(user: User.Blank, data: UserData): User.Active
    suspend fun updateUser(user: User.Active, newData: UserData): User.Active

    suspend fun deleteUser(user: User): User
}

interface ContactStorageApi {
    suspend fun createContact(contactData: ContactData): UserContact.Unconfirmed
    suspend fun confirmContact(contact: UserContact.Unconfirmed): UserContact.Active
    suspend fun regenerateVerificationCode(contact: UserContact.Unconfirmed): UserContact.Unconfirmed

    suspend fun getContactByUniqueKey(uniqueKey: ContactUniqueKey): UserContact.Active?
}

interface PostStorageApi {
    suspend fun initNewPost(author: User.Active): Post.Initialized
    suspend fun addPostTitle(post: Post.Initialized, title: String): Post.Filling
    suspend fun addPostParagraph(post: Post.Filling, paragraph: Paragraph): Post.Filling
    suspend fun addPostCategory(post: Post.Filling, category: Category): Post.Categorizing
    suspend fun changePostCategory(post: Post.Categorizing, newCategory: Category): Post.Categorizing
    suspend fun addPostTags(post: Post.Categorizing, tags: List<Tag>): Post.Taggable
    suspend fun replacePostTags(post: Post.Taggable, newTags: List<Tag>): Post.Taggable
    suspend fun completePost(post: Post.Taggable): Post.Active

    suspend fun deletePost(post: Post): Post

    suspend fun getPostById(id: Int): Post?
    suspend fun getUserPostBlank(user: User.Active): Post.NotCompleted?
}

interface CategoryStorageApi {
    suspend fun searchCategories(filter: Filter<Category>): List<Category.Meaningful>
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