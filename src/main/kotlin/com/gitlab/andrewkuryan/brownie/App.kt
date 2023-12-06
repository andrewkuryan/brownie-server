package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.api.memoryStorage.MemoryStorageApi
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
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.Security
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.naming.NoPermissionException

class ClientException(override val message: String?) : Exception()

fun Application.getConfig(path: String): String {
    println(path.uppercase().replace(".", "_"))
    return environment.config.propertyOrNull(path)?.getString() ?: System.getenv()
        .getOrDefault(path.uppercase().replace(".", "_"), "")
}

@Suppress("UNUSED")
fun Application.main() {
    Security.addProvider(BouncyCastleProvider())

    val kf = KeyFactory.getInstance("EC")
    val privateKeySpec = PKCS8EncodedKeySpec(
        Base64.getDecoder().decode(getConfig("ktor.security.ecdsa.privateKey"))
    )
    val privateKey = kf.generatePrivate(privateKeySpec)

    val srpGenerator = SrpGenerator(
        N = BigInteger(getConfig("ktor.security.srp.N"), 16),
        NBitLen = getConfig("ktor.security.srp.NBitLen").toInt(),
        g = BigInteger(getConfig("ktor.security.srp.g"), 16)
    )
    val memoryStorageApi: StorageApi = MemoryStorageApi()
    val customGsonConverter = CustomGsonConverter()
    val emailService = EmailService(
        smtpServer = getConfig("ktor.smtp.server"),
        smtpServerPort = getConfig("ktor.smtp.port").toInt(),
        smtpServerUsername = getConfig("ktor.smtp.username"),
        smtpServerPassword = getConfig("ktor.smtp.password"),
        senderName = getConfig("ktor.smtp.senderName"),
        senderEmail = getConfig("ktor.smtp.senderEmail"),
        templatesRoot = Paths.get("mail")
    )
    val telegramApi = TelegramApi(memoryStorageApi, getConfig("ktor.telegram.botToken"))

    install(CORS) {
        anyHost()
        method(HttpMethod.Put)
        header(HttpHeaders.ContentType)
        allowHeadersPrefixed("X-")
        header("X-PublicKey")
        header("X-Signature")
        header("X-BrowserName")
        header("X-OsName")
    }
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
                is NoPermissionException -> call.respond(HttpStatusCode.Forbidden, cause.message ?: "No Permission")
                is ClientException -> call.respond(HttpStatusCode.BadRequest, cause.message ?: "Bad Request")
                else -> call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Interval Server Error")
            }
        }
    }

    rootRoutes(memoryStorageApi, srpGenerator, emailService, telegramApi, customGsonConverter.gson)
}