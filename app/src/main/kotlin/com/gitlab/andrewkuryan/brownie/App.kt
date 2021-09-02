package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.MemoryStorageApi
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.routes.rootRoutes
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
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

fun main() {
    Security.addProvider(BouncyCastleProvider())

    val storageApi: StorageApi = MemoryStorageApi()
    val gson = Gson()

    embeddedServer(CIO, 3388) {
        install(DoubleReceive)
        install(ContentNegotiation) {
            gson()
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
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }

        rootRoutes(gson, storageApi)
    }.start(wait = true)
}