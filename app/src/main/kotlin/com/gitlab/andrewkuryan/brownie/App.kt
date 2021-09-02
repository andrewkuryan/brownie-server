package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.MemoryStorageApi
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.BackendSession
import com.gitlab.andrewkuryan.brownie.entity.User
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.util.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileNotFoundException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.NoSuchFileException
import java.security.KeyFactory
import java.security.Security
import java.security.Signature
import java.security.spec.EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.naming.NoPermissionException

fun checkSignature(publicKey: String, signMessage: String, signature: String): Boolean {
    val kf = KeyFactory.getInstance("EC")
    val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
    val pk = kf.generatePublic(publicKeySpec)

    val ecdsaVerify = Signature
        .getInstance("SHA512withPLAIN-ECDSA", BouncyCastleProvider.PROVIDER_NAME)
    ecdsaVerify.initVerify(pk)
    ecdsaVerify.update(signMessage.toByteArray())
    return ecdsaVerify.verify(Base64.getDecoder().decode(signature))
}

fun main() {
    Security.addProvider(BouncyCastleProvider())

    val storageApi: StorageApi = MemoryStorageApi()

    val sessionUserKey = AttributeKey<User>("SessionUser")

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

        routing {
            intercept(ApplicationCallPipeline.Call) {
                if (call.request.uri.startsWith("/api")) {
                    val rawPublicKey = call.request.headers["X-PublicKey"] ?: ""
                    val signature = call.request.headers["X-Signature"] ?: ""
                    val browserName = call.request.headers["X-BrowserName"] ?: ""
                    val osName = call.request.headers["X-OsName"] ?: ""
                    val body = call.receive<String>()

                    val signMessage = """{
"url":"${URLDecoder.decode(call.request.uri, StandardCharsets.UTF_8)}",
"browserName":"$browserName",
"osName":"$osName",
"method":"${call.request.httpMethod.value}"
${if (body.isNotEmpty()) ",\"body\":$body" else ""}}"""
                        .trimIndent().trim()
                        .replace("\n", "")

                    if (!checkSignature(rawPublicKey, signMessage, signature)) {
                        throw NoPermissionException("Signature does not match")
                    }

                    val backendSession = BackendSession(rawPublicKey, browserName, osName)
                    val user = storageApi.userApi.getUserBySession(backendSession)
                    if (user == null) {
                        val newUser = storageApi.userApi.createNewGuest(backendSession)
                        context.attributes.put(sessionUserKey, newUser)
                    } else {
                        context.attributes.put(sessionUserKey, user)
                    }
                }
            }

            get("/api/test-sign") {
                call.respond("""{ "Result": "${call.request.queryParameters["message"]}" }""")
            }

            post("/api/test-post") {
                val body = call.receive<String>()
                println(context.attributes[sessionUserKey])
                call.respond("""{ "Result": $body }""")
            }
        }
    }.start(wait = true)
}