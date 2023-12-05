package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.memoryStorage.MemoryStorageApi
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.logic.SrpGenerator
import com.gitlab.andrewkuryan.brownie.routes.rootRoutes
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.nio.file.Paths
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
        N = BigInteger(environment.config.property("ktor.security.srp.N").getString(), 16),
        NBitLen = environment.config.property("ktor.security.srp.NBitLen").getString().toInt(),
        g = BigInteger(environment.config.property("ktor.security.srp.g").getString(), 16)
    )
    val memoryStorageApi: StorageApi = MemoryStorageApi()
    val customGsonConverter = CustomGsonConverter()
    val emailService = EmailService(
        smtpServer = environment.config.property("ktor.smtp.server").getString(),
        smtpServerPort = environment.config.property("ktor.smtp.port").getString().toInt(),
        smtpServerUsername = environment.config.property("ktor.smtp.username").getString(),
        smtpServerPassword = environment.config.property("ktor.smtp.password").getString(),
        senderName = environment.config.property("ktor.smtp.senderName").getString(),
        senderEmail = environment.config.property("ktor.smtp.senderEmail").getString(),
        templatesRoot = Paths.get("mail")
    )
    val telegramApi = TelegramApi(
        memoryStorageApi,
        environment.config.property("ktor.telegram.botToken").getString(),
    )

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