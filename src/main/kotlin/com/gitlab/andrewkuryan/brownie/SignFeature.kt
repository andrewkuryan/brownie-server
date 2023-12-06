package com.gitlab.andrewkuryan.brownie

import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.user.BackendSession
import com.gitlab.andrewkuryan.brownie.entity.user.User
import com.gitlab.andrewkuryan.brownie.routes.fileChecksumKey
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.content.TextContent
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.naming.NoPermissionException

val authorizedUserKey = AttributeKey<User>("SessionUser")
val sessionKey = AttributeKey<BackendSession>("BackendSession")
val verifiedBodyKey = AttributeKey<String>("ReceivedBody")

fun PipelineContext<Unit, ApplicationCall>.getAuthorizedUser(): User {
    return context.attributes[authorizedUserKey]
}

fun PipelineContext<Unit, ApplicationCall>.getSession(): BackendSession {
    return context.attributes[sessionKey]
}

inline fun <reified T> PipelineContext<Unit, ApplicationCall>.receiveVerified(gson: Gson): T {
    return gson.fromJson(context.attributes[verifiedBodyKey], T::class.java)
}

fun checkSignature(publicKey: String, signMessage: String, signature: String): Boolean {
    val kf = KeyFactory.getInstance("EC")
    val publicKeySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKey))
    val pk = kf.generatePublic(publicKeySpec)

    val ecdsaVerify = Signature
        .getInstance("SHA512withPLAIN-ECDSA", BouncyCastleProvider.PROVIDER_NAME)
    ecdsaVerify.initVerify(pk)
    ecdsaVerify.update(signMessage.toByteArray())
    return ecdsaVerify.verify(Base64.getDecoder().decode(signature))
}

fun createSignature(privateKey: PrivateKey, signMessage: String): String {
    val ecdsaSign = Signature
        .getInstance("SHA512withPLAIN-ECDSA", BouncyCastleProvider.PROVIDER_NAME)
    ecdsaSign.initSign(privateKey)
    ecdsaSign.update(signMessage.toByteArray(charset = Charsets.UTF_8))
    return Base64.getEncoder().encodeToString(ecdsaSign.sign())
}

class SignFeature(configuration: Configuration) {
    val storageApi: StorageApi = configuration.storageApi ?: throw Exception("StorageApi not specified")
    val privateSignKey: PrivateKey = configuration.privateSignKey
        ?: throw Exception("Private key for sign not specified")

    class Configuration {
        var storageApi: StorageApi? = null
        var privateSignKey: PrivateKey? = null
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, SignFeature> {
        override val key: AttributeKey<SignFeature> = AttributeKey("ECDSASign")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): SignFeature {
            val configuration = Configuration().apply(configure)
            val feature = SignFeature(configuration)

            pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) { subject ->
                if (context.response is RoutingApplicationResponse) {
                    val baseUrl = "${call.request.local.scheme}://${call.request.local.host}:${call.request.local.port}"
                    println("baseUrl: $baseUrl")
                    val signMessageObject = """
                    |{"url":"$baseUrl${URLDecoder.decode(call.request.uri, StandardCharsets.UTF_8)}",
                    |"method":"${call.request.httpMethod.value}"
                    |${
                        when (subject) {
                            is TextContent -> ",\"body\":${subject.text}"
                            is LocalFileContent -> ",\"checksum\":\"${call.attributes[fileChecksumKey]}\""
                            else -> ""
                        }
                    }}"""
                        .trimMargin()
                        .trim()
                        .replace("\n", "")

                    val signature = createSignature(feature.privateSignKey, signMessageObject)

                    context.response.header("Access-Control-Expose-Headers","X-Signature")
                    context.response.header("X-Signature", signature)
                }
            }
            pipeline.intercept(ApplicationCallPipeline.Call) {
                if (call.request.uri.startsWith("/api")) {
                    val baseUrl = "${call.request.local.scheme}://${call.request.local.host}:${call.request.local.port}"
                    println("baseUrl: $baseUrl")
                    val rawPublicKey = call.request.headers["X-PublicKey"] ?: ""
                    val signature = call.request.headers["X-Signature"] ?: ""
                    val browserName = call.request.headers["X-BrowserName"] ?: ""
                    val osName = call.request.headers["X-OsName"] ?: ""
                    val body = call.receive<String>()

                    val signMessageObject = """
                    |{"url":"$baseUrl${URLDecoder.decode(call.request.uri, StandardCharsets.UTF_8)}",
                    |"browserName":"$browserName",
                    |"osName":"$osName",
                    |"method":"${call.request.httpMethod.value}"
                    |${if (body.isNotEmpty()) ",\"body\":${body}" else ""}}""".trimMargin()
                        .trim()
                        .replace("\n", "")

                    if (!checkSignature(rawPublicKey, signMessageObject, signature)) {
                        throw NoPermissionException("Signature does not match")
                    }

                    val userWithSession = feature.storageApi.userApi.getUserBySessionKey(rawPublicKey)
                    if (userWithSession == null) {
                        val backendSession = BackendSession.Guest(rawPublicKey, browserName, osName)
                        val newUser = feature.storageApi.userApi.createNewGuest(backendSession)
                        context.attributes.put(authorizedUserKey, newUser)
                        context.attributes.put(sessionKey, backendSession)
                    } else {
                        context.attributes.put(authorizedUserKey, userWithSession.first)
                        context.attributes.put(sessionKey, userWithSession.second)
                    }
                    context.attributes.put(verifiedBodyKey, body)
                }
            }

            return feature
        }
    }
}