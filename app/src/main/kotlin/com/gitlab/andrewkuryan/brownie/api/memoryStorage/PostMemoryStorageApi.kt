package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.PostStorageApi
import com.gitlab.andrewkuryan.brownie.entity.ActiveUser
import com.gitlab.andrewkuryan.brownie.entity.Post
import com.gitlab.andrewkuryan.brownie.entity.PostBlank
import com.gitlab.andrewkuryan.brownie.entity.Paragraph
import java.util.*

typealias PostId = Int
typealias UserId = Int

internal val posts = mutableMapOf<PostId, Pair<UserId, Post>>()
internal val postBlanks = mutableMapOf<UserId, PostBlank>()

class PostMemoryStorageApi : PostStorageApi {

    private var currentPostId = 0

    override suspend fun getPostById(id: Int): Post? {
        return posts[id]?.second
    }

    override suspend fun initNewPost(author: ActiveUser): PostBlank.Initialized {
        val newPost = PostBlank.Initialized(currentPostId, author.id)
        postBlanks[author.id] = newPost
        currentPostId += 1
        return newPost
    }

    override suspend fun addPostTitle(post: PostBlank.Initialized, title: String): PostBlank.Filling {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = PostBlank.Filling(post.id, post.authorId, title, listOf())
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun addPostParagraph(post: PostBlank.Filling, paragraph: Paragraph): PostBlank.Filling {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = post.copy(paragraphs = post.paragraphs + paragraph)
            postBlanks[authorId] = newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun completePost(post: PostBlank.Filling): Post {
        val authorId = postBlanks.entries.find { it.value.id == post.id }?.key
        if (authorId != null) {
            val newPost = Post(post.id, post.title, post.paragraphs, Date(), post.authorId)
            postBlanks.remove(authorId)
            posts[newPost.id] = authorId to newPost
            return newPost
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun deletePost(post: Post): Post {
        val oldPost = posts[post.id]
        if (oldPost != null) {
            posts.remove(post.id)
            return post
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun deletePost(post: PostBlank): PostBlank {
        val oldPost = postBlanks.entries.find { it.value.id == post.id }
        if (oldPost != null) {
            postBlanks.remove(oldPost.key)
            return post
        } else {
            throw Exception("No such post")
        }
    }

    override suspend fun getUserPostBlank(user: ActiveUser): PostBlank? {
        return postBlanks[user.id]
    }
}