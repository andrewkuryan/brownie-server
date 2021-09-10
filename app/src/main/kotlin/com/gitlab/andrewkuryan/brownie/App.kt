package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.MemoryStorageApi
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.routes.rootRoutes
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException
import java.security.Security
import javax.naming.NoPermissionException

class ClientException(override val message: String?) : Exception()

fun main() {
    Security.addProvider(BouncyCastleProvider())

    val storageApi: StorageApi = MemoryStorageApi()
    val customGsonConverter = CustomGsonConverter()

    embeddedServer(CIO, 3388) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, customGsonConverter)
        }
        install(StatusPages) {
            status(HttpStatusCode.NotFound) {
                try {
                    call.respondFile(
                            File("web/${call.request.uri.split("/").last()}")
                    )
                } catch (exc: Exception) {
                    when (exc) {
                        is FileNotFoundException, is NoSuchFileException ->
                            call.respondFile(File("web/index.html"))
                        else -> throw exc
                    }
                }
            }
            exception<Throwable> { cause ->
                cause.printStackTrace()
                when (cause) {
                    is NoPermissionException -> call.respond(HttpStatusCode.Forbidden)
                    is ClientException -> call.respond(HttpStatusCode.BadRequest)
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }

        rootRoutes(storageApi, customGsonConverter.gson)
        launchTelegramBot(storageApi)
    }.start(wait = true)
}