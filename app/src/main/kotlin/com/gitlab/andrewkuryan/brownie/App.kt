package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.MemoryStorageApi
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.logic.SrpGenerator
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
import java.security.KeyFactory
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.naming.NoPermissionException

class ClientException(override val message: String?) : Exception()

@Suppress("UNUSED")
fun Application.main() {
    Security.addProvider(BouncyCastleProvider())

    val kf = KeyFactory.getInstance("EC")
    val privateKeySpec = PKCS8EncodedKeySpec(
            Base64.getDecoder().decode(environment.config.property("ktor.security.ecdsa.privateKey").getString())
    )
    val privateKey = kf.generatePrivate(privateKeySpec)

    val srpGenerator = SrpGenerator(
            BigInteger(environment.config.property("ktor.security.srp.N").getString(), 16),
            environment.config.property("ktor.security.srp.NBitLen").getString().toInt(),
            BigInteger(environment.config.property("ktor.security.srp.g").getString(), 16)
    )
    val memoryStorageApi: StorageApi = MemoryStorageApi()
    val customGsonConverter = CustomGsonConverter()

    install(ContentNegotiation) {
        register(ContentType.Application.Json, customGsonConverter)
    }
    install(SignFeature) {
        storageApi = memoryStorageApi
        privateSignKey = privateKey
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

    rootRoutes(memoryStorageApi, srpGenerator, customGsonConverter.gson)
    launchTelegramBot(memoryStorageApi)
}