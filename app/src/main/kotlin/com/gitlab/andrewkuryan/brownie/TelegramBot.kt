package com.gitlab.andrewkuryan.brownie

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.*
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TelegramApi(storageApi: StorageApi, botToken: String) {

    enum class ServiceMessage(val text: String) {
        COMPLETE_TITLE("⬇ Complete Title"),
        COMPLETE_PARAGRAPH("⬇ Complete Paragraph"),
        COMPLETE("✅ Complete"),
        CANCEL("❌ Cancel"),
    }

    private val postTitleReplyMarkup = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(KeyboardButton(ServiceMessage.COMPLETE_TITLE.text)),
            listOf(KeyboardButton(ServiceMessage.CANCEL.text))
        ),
        resizeKeyboard = true,
    )
    private val paragraphReplyMarkup = KeyboardReplyMarkup(
        keyboard = listOf(
            listOf(KeyboardButton(ServiceMessage.COMPLETE_PARAGRAPH.text)),
            listOf(
                KeyboardButton(ServiceMessage.CANCEL.text),
                KeyboardButton(ServiceMessage.COMPLETE.text),
            )
        ),
        resizeKeyboard = true,
    )
    private val postEndingReplyMarkup = ReplyKeyboardRemove(removeKeyboard = true)

    private val bot = bot {
        token = botToken
        val previousMessages = mutableMapOf<ChatId, Message>()
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
                            sendVerificationCode(contactData, contact.verificationCode)
                        }
                    }
                }
            }
            command("post") {
                withChatId(message) { chatId ->
                    CoroutineScope(Dispatchers.IO).launch {
                        withUser(message, storageApi) { user ->
                            storageApi.postApi.initNewPost(user)
                            bot.sendMessage(
                                chatId,
                                "Enter post title",
                                replyMarkup = postTitleReplyMarkup,
                            )
                        }
                    }
                }
            }
            message(Filter.Custom { this.text == ServiceMessage.COMPLETE_TITLE.text }) {
                withChatId(message) { chatId ->
                    bot.deleteMessage(chatId, message.messageId)
                    CoroutineScope(Dispatchers.IO).launch {
                        withUser(message, storageApi) { user ->
                            val postBlank = storageApi.postApi.getUserPostBlank(user)
                            val previousText = previousMessages[ChatId.fromId(message.chat.id)]?.text
                            if (postBlank != null && postBlank is PostBlank.Initialized && previousText != null) {
                                storageApi.postApi.addPostTitle(postBlank, previousText)
                                bot.sendMessage(
                                    chatId,
                                    "Enter paragraph (text or image)",
                                    replyMarkup = paragraphReplyMarkup,
                                )
                            }
                        }
                    }
                }
            }
            message(Filter.Custom { this.text == ServiceMessage.COMPLETE_PARAGRAPH.text }) {
                withChatId(message) { chatId ->
                    bot.deleteMessage(chatId, message.messageId)
                    CoroutineScope(Dispatchers.IO).launch {
                        withUser(message, storageApi) { user ->
                            val postBlank = storageApi.postApi.getUserPostBlank(user)
                            val previousMessage = previousMessages[ChatId.fromId(message.chat.id)]
                            if (postBlank != null && postBlank is PostBlank.Filling && previousMessage != null) {
                                buildParagraph(previousMessage, storageApi)?.let { paragraph ->
                                    println(paragraph)
                                    storageApi.postApi.addPostParagraph(postBlank, paragraph)
                                    bot.sendMessage(
                                        chatId,
                                        "Enter paragraph (text or image)",
                                        replyMarkup = paragraphReplyMarkup,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            message(Filter.Custom { this.text == ServiceMessage.CANCEL.text }) {
                withChatId(message) { chatId ->
                    bot.deleteMessage(chatId, message.messageId)
                    CoroutineScope(Dispatchers.IO).launch {
                        withUser(message, storageApi) { user ->
                            withUserPostBlank(user, storageApi) { postBlank ->
                                storageApi.postApi.deletePost(postBlank)
                                bot.sendMessage(
                                    chatId,
                                    "New post creation canceled",
                                    replyMarkup = postEndingReplyMarkup,
                                )
                            }
                        }
                    }
                }
            }
            message(Filter.Custom { this.text == ServiceMessage.COMPLETE.text }) {
                withChatId(message) { chatId ->
                    bot.deleteMessage(chatId, message.messageId)
                    CoroutineScope(Dispatchers.IO).launch {
                        withUser(message, storageApi) { user ->
                            val postBlank = storageApi.postApi.getUserPostBlank(user)
                            if (postBlank != null && postBlank is PostBlank.Filling) {
                                val result = storageApi.postApi.completePost(postBlank)
                                bot.sendMessage(
                                    chatId,
                                    "Post ${result.id} created successfully",
                                    replyMarkup = postEndingReplyMarkup,
                                )
                            }
                        }
                    }
                }
            }
            message {
                if (ServiceMessage.values().all { it.text != message.text }) {
                    previousMessages[ChatId.fromId(message.chat.id)] = message
                }
            }
        }
    }

    init {
        bot.startPolling()
    }

    fun sendVerificationCode(contactData: TelegramContactData, verificationCode: String) {
        bot.sendMessage(
            ChatId.fromId(contactData.telegramId),
            "Your verification code:\n${verificationCode}"
        )
    }

    private fun withChatId(message: Message, body: (ChatId) -> Unit) {
        val tgUserId = message.from?.id
        if (tgUserId != null) {
            body(ChatId.fromId(tgUserId))
        }
    }

    private suspend fun withUser(message: Message, storageApi: StorageApi, body: suspend (ActiveUser) -> Unit) {
        val userContact =
            storageApi.contactApi.getContactByUniqueKey(TelegramContactKey(message.from?.id!!))
        val user = if (userContact != null) storageApi.userApi.getUserByContact(userContact) else null
        if (user != null) {
            body(user)
        }
    }

    private suspend fun withUserPostBlank(user: ActiveUser, storageApi: StorageApi, body: suspend (PostBlank) -> Unit) {
        val postBlank = storageApi.postApi.getUserPostBlank(user)
        if (postBlank != null) {
            body(postBlank)
        }
    }

    private fun List<MessageEntity.Type>.toTextFormatting(): TextFormatting {
        return TextFormatting(
            fontWeight = if (any { it == MessageEntity.Type.BOLD }) FontWeight.BOLD else FontWeight.NORMAL,
            fontStyle = if (any { it == MessageEntity.Type.ITALIC }) FontStyle.ITALIC else FontStyle.NORMAL,
            textDecoration = if (any { it == MessageEntity.Type.UNDERLINE }) TextDecoration.UNDERLINE else TextDecoration.NONE,
        )
    }

    private suspend fun buildParagraph(message: Message, storageApi: StorageApi): Paragraph? = when {
        message.photo != null ->
            bot.getFile(message.photo!!.maxByOrNull { it.fileSize ?: 0 }!!.fileId).first?.body()?.result?.filePath
                ?.let { imagePath ->
                    bot.downloadFile(imagePath).first?.body()?.byteStream()?.let {
                        Paragraph.Image(
                            file = storageApi.fileApi.saveFile(it, StorageFileFormat.fromExtension(imagePath)),
                            description = message.caption
                        )
                    }
                }
        message.text != null -> {
            val textEntities = (message.entities ?: listOf())
                .groupBy { it.offset }
                .mapValues { (_, value) -> value.sortedBy { it.length } }
                .entries.toList()
            val textItems = textEntities.mapIndexed { index, entry ->
                val prevItemOffset = textEntities.getOrNull(index - 1)?.value?.firstOrNull()?.offset ?: 0
                val prevItemLength = textEntities.getOrNull(index - 1)?.value?.lastOrNull()?.length ?: 0
                val prevItem = TextItem(
                    text = message.text!!.substring(prevItemOffset + prevItemLength, entry.value.first().offset),
                    formatting = TextFormatting.NONE,
                )
                listOf(prevItem) + entry.value.mapIndexed { i, entity ->
                    val prevLength = entry.value.getOrNull(i - 1)?.length ?: 0
                    val startIndex = entity.offset + prevLength
                    val entityText = message.text!!.substring(startIndex, startIndex + (entity.length - prevLength))
                    val formatting = entry.value.subList(i, entry.value.size).map { it.type }.toTextFormatting()
                    TextItem(text = entityText, formatting = formatting)
                }
            }.flatten()
            val prevOffset = textEntities.lastOrNull()?.value?.lastOrNull()?.offset ?: 0
            val prevLength = textEntities.lastOrNull()?.value?.lastOrNull()?.length ?: 0
            val lastItem = TextItem(
                text = message.text!!.substring(prevOffset + prevLength),
                formatting = TextFormatting.NONE,
            )
            Paragraph.Text(items = textItems + listOf(lastItem))
        }
        else -> null
    }
}