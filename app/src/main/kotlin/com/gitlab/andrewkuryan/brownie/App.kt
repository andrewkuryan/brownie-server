package com.gitlab.andrewkuryan.brownie

import io.ktor.features.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.gson.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException

fun main() {
    embeddedServer(CIO, 3388) {
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
        }

        routing {

        }
    }.start(wait = true)
}
