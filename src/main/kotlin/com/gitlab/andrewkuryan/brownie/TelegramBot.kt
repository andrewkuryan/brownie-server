package com.gitlab.andrewkuryan.brownie

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.entities.*
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.user.ContactData
import com.gitlab.andrewkuryan.brownie.entity.user.User
import com.gitlab.andrewkuryan.brownie.telegram.flow.createPost.createPostFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TelegramApi(storageApi: StorageApi, botToken: String) {

    private val getBot = { bot }

    private val bot: Bot = bot {
        token = botToken
        dispatch {
            command("start") {
                val userId = message.text?.removePrefix("/start userId-")?.toIntOrNull()
                val tgUserId = message.from?.id
                val tgFirstName = message.from?.firstName
                val tgUsername = message.from?.username
                if (userId != null && tgUserId != null && tgFirstName != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val user = storageApi.userApi.getUserById(userId)
                        if (user != null && user is User.Guest) {
                            val contactData = ContactData.Telegram(tgUserId, tgFirstName, tgUsername)
                            val contact = storageApi.contactApi.createContact(contactData)
                            storageApi.userApi.addUserContact(user, contact)
                            sendVerificationCode(contactData, contact.verificationCode)
                        }
                    }
                }
            }

            createPostFlow(storageApi, getBot)
        }
    }

    init {
        bot.startPolling()
    }

    fun sendVerificationCode(contactData: ContactData.Telegram, verificationCode: String) {
        bot.sendMessage(
            ChatId.fromId(contactData.telegramId),
            "Your verification code:\n${verificationCode}"
        )
    }
}