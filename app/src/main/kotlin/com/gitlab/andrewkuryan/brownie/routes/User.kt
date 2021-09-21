package com.gitlab.andrewkuryan.brownie.routes

import com.github.salomonbrys.kotson.fromJson
import com.gitlab.andrewkuryan.brownie.*
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.api.dumpDB
import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.logic.SrpGenerator
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import java.math.BigInteger
import javax.naming.NoPermissionException

data class VerifyContactBody(val verificationCode: String)

data class FulfillUserBody(
        val login: String,
        val salt: String,
        val verifierHex: String
)

data class LoginInitBody(val login: String, val AHex: String)
data class LoginInitResponse(val salt: String, val BHex: String)

data class LoginVerifyBody(val login: String, val AHex: String, val BHex: String, val MHex: String)
data class LoginVerifyResponse(val RHex: String, val user: User)

fun Route.userRoutes(storageApi: StorageApi, srpGenerator: SrpGenerator, gson: Gson) {
    get("/api/user") {
        val user = context.attributes[sessionUserKey]
        call.respond(user)
    }

    post("/api/user/contact/{id}/verify") {
        val user = context.attributes[sessionUserKey]
        val code = gson.fromJson<VerifyContactBody>(context.attributes[receivedBodyKey])
        val contactId = call.parameters["id"]?.toInt() ?: -1
        val contact = when {
            user is BlankUser && user.contact is UnconfirmedUserContact && user.contact.id == contactId ->
                user.contact
            user is ActiveUser && user.contacts.find { it is UnconfirmedUserContact && it.id == contactId } != null ->
                user.contacts.filterIsInstance<UnconfirmedUserContact>().find { it.id == contactId }!!
            else -> throw ClientException("Unverified contact not found")
        }
        if (code.verificationCode == contact.verificationCode) {
            val newContact = storageApi.contactApi.confirmContact(contact)
            call.respond<UserContact>(newContact)
        } else {
            throw ClientException("Wrong verification code: ${code.verificationCode}")
        }
    }

    put("/api/user/fulfill") {
        val user = context.attributes[sessionUserKey]
        val session = context.attributes[sessionKey]
        val body = gson.fromJson<FulfillUserBody>(context.attributes[receivedBodyKey])
        if (user is BlankUser && session is GuestSession) {
            val verifier = BigInteger(body.verifierHex, 16)
            val data = UserData(body.login, UserCredentials(body.salt, verifier))
            val newUser = storageApi.userApi.fulfillUser(user, data)
            val newSession = ActiveSession(session.publicKey, session.browserName, session.osName)
            storageApi.userApi.updateSession(session, newSession)
            call.respond<User>(newUser)
            dumpDB()
        } else {
            throw ClientException("User cannot be fulfilled")
        }
    }

    post("/api/user/login/init") {
        val guestUser = context.attributes[sessionUserKey]
        val guestSession = context.attributes[sessionKey]
        if (guestUser is GuestUser && guestSession is GuestSession) {
            val body = gson.fromJson<LoginInitBody>(context.attributes[receivedBodyKey])
            val user = storageApi.userApi.getUserByLogin(body.login) ?: throw ClientException("User not fount")
            val (KHex, B) = srpGenerator.computeKHexB(BigInteger(body.AHex, 16), user.data.credentials.verifier)
            val newSession = TempSession(guestSession.publicKey, guestSession.browserName, guestSession.osName, KHex)
            storageApi.userApi.updateSession(guestSession, newSession)
            call.respond(LoginInitResponse(user.data.credentials.salt, B.toString(16)))
            dumpDB()
        } else {
            throw ClientException("Session is already in use")
        }
    }

    post("/api/user/login/verify") {
        val guestUser = context.attributes[sessionUserKey]
        val tempSession = context.attributes[sessionKey]
        if (guestUser is GuestUser && tempSession is TempSession) {
            val body = gson.fromJson<LoginVerifyBody>(context.attributes[receivedBodyKey])
            val user = storageApi.userApi.getUserByLogin(body.login) ?: throw ClientException("User not fount")
            val expectedM = srpGenerator
                    .computeMHex(body.login, user.data.credentials.salt, body.AHex, body.BHex, tempSession.KHex)
            if (expectedM == body.MHex) {
                val newSession = ActiveSession(tempSession.publicKey, tempSession.browserName, tempSession.osName)
                storageApi.userApi.changeSessionOwner(tempSession, user, newSession)
                storageApi.userApi.deleteUser(guestUser)
                call.respond(LoginVerifyResponse(srpGenerator.computeRHex(body.AHex, expectedM, tempSession.KHex), user))
                dumpDB()
            } else {
                throw NoPermissionException("Session values does not match")
            }
        } else {
            throw ClientException("Session is already in use")
        }
    }
}