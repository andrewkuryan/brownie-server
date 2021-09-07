package com.gitlab.andrewkuryan.brownie

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.GuestUser
import com.gitlab.andrewkuryan.brownie.entity.TelegramContactData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun launchTelegramBot(storageApi: StorageApi) {
    val bot = bot {
        token = "1803769708:AAFXgXBqP2x6W4jRMq8BSSTIQlf_eMyELio"
        dispatch {
            command("start") {
                val userId = message.text?.removePrefix("/start userId-")?.toIntOrNull()
                val tgUserId = message.from?.id
                val tgFirstName = message.from?.firstName
                val tgUsername = message.from?.username
                if (userId != null && tgUserId != null && tgFirstName != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val user = storageApi.userApi.getUserById(userId)
                        if (user != null && user is GuestUser) {
                            val contactData = TelegramContactData(tgUserId, tgFirstName, tgUsername)
                            val contact = storageApi.contactApi.createContact(contactData)
                            storageApi.userApi.addUserContact(user, contact)
                        }
                    }
                }
            }
        }
    }
    bot.startPolling()
}