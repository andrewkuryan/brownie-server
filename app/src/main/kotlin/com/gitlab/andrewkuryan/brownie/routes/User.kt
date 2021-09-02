package com.gitlab.andrewkuryan.brownie.routes

import com.github.salomonbrys.kotson.jsonObject
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.GuestUser
import com.gitlab.andrewkuryan.brownie.entity.User
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun getUserTypeString(user: User): String {
    return when (user) {
        is GuestUser -> "GuestUser"
    }
}

fun Route.userRoutes(gson: Gson, storageApi: StorageApi) {
    get("/api/user") {
        val user = context.attributes[sessionUserKey]
        call.respond(
            jsonObject(
                "type" to getUserTypeString(user),
                "data" to gson.toJsonTree(user)
            )
        )
    }
}