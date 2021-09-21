package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.MemoryStorageApi
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.routes.rootRoutes
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileNotFoundException
import java.math.BigInteger
import java.nio.file.NoSuchFileException
import java.security.Security
import javax.naming.NoPermissionException

class ClientException(override val message: String?) : Exception()

val N = BigInteger("1ebf99b3991d1f5eff8d19cbbd3eef5f1254fa977e08bdc097e5b6fd554649e134327b44d1115ffacf5278614ca4452d68692489c0745392f8db33f4ab74e8251a9e5032f4654ef1c17e286bb875e8d172451c439f4cf71277a16a333be25cd803744ee7888b3a9a8319011e82c58188914cac8c5155c71ac919c9390fe1589b7", 16)
const val N_BITLEN = 1024
val g = BigInteger.TWO
val k = BigInteger("439ed3dae05ef86e497bb7d9aa27d6907e517c35c526abbb4b833abcf6cc23e0913435c1cdfa1fafcbc491e4602c61f95e47172a02d8ddb3c6b977d5f00b371", 16)

fun Application.main() {
    Security.addProvider(BouncyCastleProvider())

    val storageApi: StorageApi = MemoryStorageApi()
    val customGsonConverter = CustomGsonConverter()

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
}