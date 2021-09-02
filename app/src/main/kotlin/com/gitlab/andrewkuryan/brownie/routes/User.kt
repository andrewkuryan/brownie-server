package com.gitlab.andrewkuryan.brownie.routes

import com.gitlab.andrewkuryan.brownie.api.StorageApi
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.userRoutes(storageApi: StorageApi) {
    get("/api/user") {
        call.respond(context.attributes[sessionUserKey])
    }
}