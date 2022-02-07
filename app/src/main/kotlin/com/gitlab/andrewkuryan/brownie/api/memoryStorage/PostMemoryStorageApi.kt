package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.PostStorageApi
import com.gitlab.andrewkuryan.brownie.entity.post.*
import com.gitlab.andrewkuryan.brownie.entity.user.User
import java.util.*

typealias PostId = Int
typealias UserId = Int

internal val posts = mutableMapOf<PostId, Pair<UserId, Post.Active>>()
internal val postBlanks = mutableMapOf<UserId, Post.NotCompleted>()

class PostMemoryStorageApi : PostStorageApi {

    private var currentPostId = 0

    override suspend fun getPostById(id: Int): Post? {
        return posts[id]?.second
    }

    override suspend fun initNewPost(author: User.Active): Post.Initialized {
        val newPost = Post.Initialized(currentPostId, author.id)
        postBlanks[author.id] = newPost
        currentPostId += 1
        return newPost
    }

    override suspend fun addPostTitle(post: Post.Initialized, title: String): Post.Filling {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = Post.Filling(post.id, post.authorId, title, listOf())
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun addPostParagraph(post: Post.Filling, paragraph: Paragraph): Post.Filling {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = post.copy(paragraphs = post.paragraphs + paragraph)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun addPostCategory(post: Post.Filling, category: Category): Post.Categorizing {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = Post.Categorizing(post.id, post.authorId, post.title, post.paragraphs, category)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun changePostCategory(post: Post.Categorizing, newCategory: Category): Post.Categorizing {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = post.copy(category = newCategory)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun addPostTags(post: Post.Categorizing, tags: List<Tag>): Post.Taggable {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = Post.Taggable(post.id, post.authorId, post.title, post.paragraphs, post.category, tags)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun replacePostTags(post: Post.Taggable, newTags: List<Tag>): Post.Taggable {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = post.copy(tags = newTags)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun completePost(post: Post.Taggable): Post.Active {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost =
                Post.Active(post.id, post.authorId, post.title, post.paragraphs, post.category, post.tags, Date())
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

    override suspend fun getUserPostBlank(user: User.Active): Post.NotCompleted? {
        return postBlanks[user.id]
    }
}