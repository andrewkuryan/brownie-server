package com.gitlab.andrewkuryan.brownie.routes

import com.github.salomonbrys.kotson.fromJson
import com.gitlab.andrewkuryan.brownie.ClientException
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

data class VerifyContactBody(val verificationCode: String)
data class FulfillUserBody(val login: String?, val passwordHash: String, val confirmedPasswordHash: String)

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
        val body = gson.fromJson<FulfillUserBody>(context.attributes[receivedBodyKey])
        if (body.passwordHash != body.confirmedPasswordHash) {
            throw ClientException("Passwords do not match")
        }
        val data = UserData(body.login, body.passwordHash)
        if (user is BlankUser) {
            val newUser = storageApi.userApi.fulfillUser(user, data)
            call.respond<User>(newUser)
        } else {
            throw ClientException("User cannot be fulfilled")
        }
    }
}