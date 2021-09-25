package com.gitlab.andrewkuryan.brownie.routes

import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.logic.SrpGenerator
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.routing.*

fun Application.rootRoutes(storageApi: StorageApi, srpGenerator: SrpGenerator, gson: Gson) {
    routing {
        userRoutes(storageApi, srpGenerator, gson)
    }
}