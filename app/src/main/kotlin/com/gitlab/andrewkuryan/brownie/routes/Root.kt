package com.gitlab.andrewkuryan.brownie.routes

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.BackendSession
import com.gitlab.andrewkuryan.brownie.entity.User
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

fun Application.rootRoutes(storageApi: StorageApi) {
    routing {
        intercept(ApplicationCallPipeline.Call) {
            if (call.request.uri.startsWith("/api")) {
                val rawPublicKey = call.request.headers["X-PublicKey"] ?: ""
                val signature = call.request.headers["X-Signature"] ?: ""
                val browserName = call.request.headers["X-BrowserName"] ?: ""
                val osName = call.request.headers["X-OsName"] ?: ""
                val body = call.receive<String>()

                val signMessageObject = jsonObject(
                    "url" to URLDecoder.decode(call.request.uri, StandardCharsets.UTF_8),
                    "browserName" to browserName,
                    "osName" to osName,
                    "method" to call.request.httpMethod.value,
                ).apply {
                    if (body.isNotEmpty()) {
                        add("body", body.toJson())
                    }
                }

                if (!checkSignature(rawPublicKey, signMessageObject.toString(), signature)) {
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

        userRoutes(storageApi)
    }
}