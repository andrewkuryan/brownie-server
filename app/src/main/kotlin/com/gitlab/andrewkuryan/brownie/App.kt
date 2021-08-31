package com.gitlab.andrewkuryan.brownie

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.NoSuchFileException
import java.security.KeyFactory
import java.security.Security
import java.security.Signature
import java.security.spec.EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

fun main() {
    Security.addProvider(BouncyCastleProvider())

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
            get("/test-sign") {
                val message = call.parameters["message"] ?: ""
                val signature = call.request.headers["signature"] ?: ""
                val rawPublicKey = call.request.headers["publicKey"] ?: ""
                println(message)
                println(signature)
                println(rawPublicKey)

                val kf = KeyFactory.getInstance("EC")
                val publicKeySpec: EncodedKeySpec = X509EncodedKeySpec(Base64.getDecoder().decode(rawPublicKey))
                val publicKey = kf.generatePublic(publicKeySpec)

                val ecdsaVerify = Signature
                    .getInstance("SHA512withPLAIN-ECDSA", BouncyCastleProvider.PROVIDER_NAME)
                ecdsaVerify.initVerify(publicKey)
                ecdsaVerify.update(message.toByteArray())
                val result = ecdsaVerify.verify(Base64.getDecoder().decode(signature))
                println(result)
            }
        }
    }.start(wait = true)
}