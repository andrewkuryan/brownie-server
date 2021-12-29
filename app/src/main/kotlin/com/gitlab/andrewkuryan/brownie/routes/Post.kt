package com.gitlab.andrewkuryan.brownie.routes

import com.gitlab.andrewkuryan.brownie.ClientException
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.postRoutes(
    storageApi: StorageApi,
    gson: Gson,
) {
    get("/api/post/{id}") {
        val postId = call.parameters["id"]?.toInt() ?: -1
        val post = storageApi.postApi.getPostById(postId)
        if (post != null) {
            call.respond(post)
        } else {
            throw ClientException("No such post")
        }
    }
}