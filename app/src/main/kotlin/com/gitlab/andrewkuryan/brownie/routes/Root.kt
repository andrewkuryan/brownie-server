package com.gitlab.andrewkuryan.brownie.routes

import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.BackendSession
import com.gitlab.andrewkuryan.brownie.entity.GuestSession
import com.gitlab.andrewkuryan.brownie.entity.User
import com.gitlab.andrewkuryan.brownie.logic.SrpGenerator
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.naming.NoPermissionException

val sessionUserKey = AttributeKey<User>("SessionUser")
val sessionKey = AttributeKey<BackendSession>("BackendSession")
val receivedBodyKey = AttributeKey<String>("ReceivedBody")

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

fun Application.rootRoutes(storageApi: StorageApi, srpGenerator: SrpGenerator, gson: Gson) {
    routing {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.uri.startsWith("/api")) {
                val rawPublicKey = call.request.headers["X-PublicKey"] ?: ""
                val signature = call.request.headers["X-Signature"] ?: ""
                val browserName = call.request.headers["X-BrowserName"] ?: ""
                val osName = call.request.headers["X-OsName"] ?: ""
                val body = call.receive<String>()

                val signMessageObject = """
                    |{"url":"${URLDecoder.decode(call.request.uri, StandardCharsets.UTF_8)}",
                    |"browserName":"$browserName",
                    |"osName":"$osName",
                    |"method":"${call.request.httpMethod.value}"
                    |${if (body.isNotEmpty()) ",\"body\":${body}" else ""}}""".trimMargin()
                        .trim()
                        .replace("\n", "")

                if (!checkSignature(rawPublicKey, signMessageObject, signature)) {
                    throw NoPermissionException("Signature does not match")
                }

                val userWithSession = storageApi.userApi.getUserBySessionKey(rawPublicKey)
                if (userWithSession == null) {
                    val backendSession = GuestSession(rawPublicKey, browserName, osName)
                    val newUser = storageApi.userApi.createNewGuest(backendSession)
                    context.attributes.put(sessionUserKey, newUser)
                    context.attributes.put(sessionKey, backendSession)
                } else {
                    context.attributes.put(sessionUserKey, userWithSession.first)
                    context.attributes.put(sessionKey, userWithSession.second)
                }
                context.attributes.put(receivedBodyKey, body)
            }
        }

        userRoutes(storageApi, srpGenerator, gson)
    }
}