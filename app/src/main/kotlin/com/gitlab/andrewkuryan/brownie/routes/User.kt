package com.gitlab.andrewkuryan.brownie.routes

import com.gitlab.andrewkuryan.brownie.*
import com.gitlab.andrewkuryan.brownie.api.StorageApi
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

fun Route.userRoutes(
        storageApi: StorageApi,
        srpGenerator: SrpGenerator,
        emailService: EmailService,
        telegramApi: TelegramApi,
        gson: Gson
) {
    get("/api/user") {
        val user = getAuthorizedUser()
        call.respond(user)
    }

    post("/api/user/contact/email") {
        val user = getAuthorizedUser()
        val body = receiveVerified<EmailContactData>(gson)
        if (user is GuestUser) {
            val contact = storageApi.contactApi.createContact(body)
            storageApi.userApi.addUserContact(user, contact)
            emailService.sendVerificationEmail(body, contact.verificationCode)
            call.respond<UserContact>(contact)
        } else {
            throw ClientException("Can't add contact to this user")
        }
    }

    post("/api/user/contact/resend-code") {
        val user = getAuthorizedUser()
        if (user is BlankUser && user.contact is UnconfirmedUserContact) {
            val newContact = storageApi.contactApi.regenerateVerificationCode(user.contact)
            when (newContact.data) {
                is EmailContactData -> emailService.sendVerificationEmail(newContact.data, newContact.verificationCode)
                is TelegramContactData -> telegramApi.sendVerificationCode(newContact.data, newContact.verificationCode)
            }
            call.respond<UserContact>(newContact)
        } else {
            throw ClientException("User don't have unconfirmed contact")
        }
    }

    post("/api/user/contact/{id}/verify") {
        val user = getAuthorizedUser()
        val code = receiveVerified<VerifyContactBody>(gson)
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
        val user = getAuthorizedUser()
        val session = getSession()
        val body = receiveVerified<FulfillUserBody>(gson)
        if (user is BlankUser && session is GuestSession) {
            val verifier = BigInteger(body.verifierHex, 16)
            val data = UserData(body.login, UserCredentials(body.salt, verifier))
            val newUser = storageApi.userApi.fulfillUser(user, data)
            val newSession = ActiveSession(session.publicKey, session.browserName, session.osName)
            storageApi.userApi.updateSession(session, newSession)
            call.respond<User>(newUser)
        } else {
            throw ClientException("User cannot be fulfilled")
        }
    }

    post("/api/user/login/init") {
        val guestUser = getAuthorizedUser()
        val guestSession = getSession()
        if (guestUser is GuestUser && guestSession is GuestSession) {
            val body = receiveVerified<LoginInitBody>(gson)
            val user = storageApi.userApi.getUserByLogin(body.login) ?: throw ClientException("User not fount")
            val (KHex, B) = srpGenerator.computeKHexB(BigInteger(body.AHex, 16), user.data.credentials.verifier)
            val newSession = TempSession(guestSession.publicKey, guestSession.browserName, guestSession.osName, KHex)
            storageApi.userApi.updateSession(guestSession, newSession)
            call.respond(LoginInitResponse(user.data.credentials.salt, B.toString(16)))
        } else {
            throw ClientException("Session is already in use")
        }
    }

    post("/api/user/login/verify") {
        val guestUser = getAuthorizedUser()
        val tempSession = getSession()
        if (guestUser is GuestUser && tempSession is TempSession) {
            val body = receiveVerified<LoginVerifyBody>(gson)
            val user = storageApi.userApi.getUserByLogin(body.login) ?: throw ClientException("User not fount")
            val expectedM = srpGenerator
                    .computeMHex(body.login, user.data.credentials.salt, body.AHex, body.BHex, tempSession.KHex)
            if (expectedM == body.MHex) {
                val newSession = ActiveSession(tempSession.publicKey, tempSession.browserName, tempSession.osName)
                storageApi.userApi.changeSessionOwner(tempSession, user, newSession)
                storageApi.userApi.deleteUser(guestUser)
                call.respond(
                        LoginVerifyResponse(
                                srpGenerator.computeRHex(body.AHex, expectedM, tempSession.KHex),
                                user
                        )
                )
            } else {
                throw NoPermissionException("Session values does not match")
            }
        } else {
            throw ClientException("Session is already in use")
        }
    }
}