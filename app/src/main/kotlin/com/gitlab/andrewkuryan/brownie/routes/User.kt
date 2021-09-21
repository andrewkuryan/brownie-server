package com.gitlab.andrewkuryan.brownie.routes

import com.github.salomonbrys.kotson.fromJson
import com.gitlab.andrewkuryan.brownie.*
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.api.dumpDB
import com.gitlab.andrewkuryan.brownie.entity.*
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.commons.codec.digest.DigestUtils
import java.math.BigInteger
import java.security.SecureRandom
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

fun Route.userRoutes(storageApi: StorageApi, gson: Gson) {
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
            val newSession = InitialSession(session.publicKey, session.browserName, session.osName)
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
            val (KHex, B) = computeKHexB(BigInteger(body.AHex, 16), user.data.credentials.verifier)
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
            val expectedM = computeM(body.login, user.data.credentials.salt, body.AHex, body.BHex, tempSession.KHex)
            if (expectedM == body.MHex) {
                val newSession = ActiveSession(tempSession.publicKey, tempSession.browserName, tempSession.osName, tempSession.KHex)
                storageApi.userApi.changeSessionOwner(tempSession, user, newSession)
                storageApi.userApi.deleteUser(guestUser)
                call.respond(LoginVerifyResponse(computeR(body.AHex, expectedM, tempSession.KHex), user))
                dumpDB()
            } else {
                throw NoPermissionException("Session values does not match")
            }
        } else {
            throw ClientException("Session is already in use")
        }
    }
}

fun computeKHexB(A: BigInteger, verifier: BigInteger): Pair<String, BigInteger> {
    val b = BigInteger(512, SecureRandom())
    val B = k * verifier + g.modPow(b, N)
    val u = computeU(A, B)
    val S = (A * verifier.modPow(u, N)).modPow(b, N)
    val KHex = DigestUtils.sha512Hex(S.toString(16))
    return KHex to B
}

fun computeM(username: String, saltHex: String, A: String, B: String, KHex: String): String {
    val NHex = BigInteger(DigestUtils.sha512Hex(N.toString(16)), 16)
    val gHex = BigInteger(DigestUtils.sha512Hex(g.toString(16)), 16)
    return DigestUtils.sha512Hex(
            (NHex xor gHex).toString(16) +
                    DigestUtils.sha512Hex(username) +
                    saltHex + A + B + KHex
    )
}

fun computeR(A: String, MHex: String, KHex: String): String {
    return DigestUtils.sha512Hex(A + MHex + KHex)
}

fun computeU(A: BigInteger, B: BigInteger): BigInteger {
    var hashIn = ""
    val aHex = A.toString(16)
    val bHex = B.toString(16)
    val nLen = 2 * ((N_BITLEN + 7) shr 3)
    hashIn += nzero(nLen - aHex.length) + aHex
    hashIn += nzero(nLen - bHex.length) + bHex
    val uHash = DigestUtils.sha512Hex(hashIn)
    val uTmp = BigInteger(uHash, 16)
    return if (uTmp < N) {
        uTmp
    } else {
        uTmp % (N - BigInteger.ONE)
    }
}

fun nzero(n: Int): String {
    if (n < 1) {
        return ""
    }
    val t = nzero(n shr 1)
    return if ((n and 1) == 0) {
        t + t
    } else {
        t + t + "0"
    }
}