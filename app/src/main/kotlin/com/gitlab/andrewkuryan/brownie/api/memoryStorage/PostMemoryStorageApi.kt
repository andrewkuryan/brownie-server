package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.PostStorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import java.util.*

typealias PostId = Int
typealias UserId = Int

internal val posts = mutableMapOf<PostId, Pair<UserId, ActivePost>>()
internal val postBlanks = mutableMapOf<UserId, NotCompletedPost>()

class PostMemoryStorageApi : PostStorageApi {

    private var currentPostId = 0

    override suspend fun getPostById(id: Int): Post? {
        return posts[id]?.second
    }

    override suspend fun initNewPost(author: ActiveUser): InitializedPost {
        val newPost = InitializedPost(currentPostId, author.id)
        postBlanks[author.id] = newPost
        currentPostId += 1
        return newPost
    }

    override suspend fun addPostTitle(post: InitializedPost, title: String): FillingPost {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = FillingPost(post.id, post.authorId, title, listOf())
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun addPostParagraph(post: FillingPost, paragraph: Paragraph): FillingPost {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = post.copy(paragraphs = post.paragraphs + paragraph)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun addPostCategory(post: FillingPost, category: Category): CategorizingPost {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = CategorizingPost(post.id, post.authorId, post.title, post.paragraphs, category)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun changePostCategory(post: CategorizingPost, newCategory: Category): CategorizingPost {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = post.copy(category = newCategory)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun addPostTags(post: CategorizingPost, tags: List<Tag>): TaggablePost {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = TaggablePost(post.id, post.authorId, post.title, post.paragraphs, post.category, tags)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun replacePostTags(post: TaggablePost, newTags: List<Tag>): TaggablePost {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = post.copy(tags = newTags)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun completePost(post: TaggablePost): ActivePost {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost =
                ActivePost(post.id, post.authorId, post.title, post.paragraphs, post.category, post.tags, Date())
            postBlanks.remove(authorId)
            posts[newPost.id] = authorId to newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun deletePost(post: Post): Post {
        val oldPost = posts[post.id]
        return if (oldPost != null) {
            posts.remove(post.id)
            post
        } else {
            val oldPostBlank = postBlanks[post.authorId]
            if (oldPostBlank != null) {
                postBlanks.remove(post.authorId)
                oldPostBlank
            } else {
                throw Exception("No such post")
            }
        }
    }

    override suspend fun getUserPostBlank(user: ActiveUser): NotCompletedPost? {
        return postBlanks[user.id]
    }
}