package com.gitlab.andrewkuryan.brownie.routes

import com.gitlab.andrewkuryan.brownie.EmailService
import com.gitlab.andrewkuryan.brownie.TelegramApi
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.logic.SrpGenerator
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.routing.*

fun Application.rootRoutes(
        storageApi: StorageApi,
        srpGenerator: SrpGenerator,
        emailService: EmailService,
        telegramApi: TelegramApi,
        gson: Gson
) {
    routing {
        userRoutes(storageApi, srpGenerator, emailService, telegramApi, gson)
    }
}