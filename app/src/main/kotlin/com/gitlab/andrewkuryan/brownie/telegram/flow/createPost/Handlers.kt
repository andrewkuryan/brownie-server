package com.gitlab.andrewkuryan.brownie.telegram.flow.createPost

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.toOption
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.entity.post.*
import com.gitlab.andrewkuryan.brownie.entity.user.ContactUniqueKey
import com.gitlab.andrewkuryan.brownie.entity.user.User
import com.gitlab.andrewkuryan.brownie.telegram.withParams

fun fromId(message: Message): () -> Option<Long> =
    { message.from?.id.toOption() }

fun user(storageApi: StorageApi): suspend (chatId: Long) -> Option<User.Active> =
    { chatId ->
        storageApi.contactApi.getContactByUniqueKey(ContactUniqueKey.Telegram(chatId))
            ?.let { storageApi.userApi.getUserByContact(it) }
            .toOption()
    }

fun userPostBlank(storageApi: StorageApi): suspend (User.Active) -> Option<Post.NotCompleted> =
    { storageApi.postApi.getUserPostBlank(it).toOption() }

inline fun <reified T : Post.NotCompleted> typedUserPostBlank(storageApi: StorageApi): suspend (User.Active) -> Option<T> =
    { user ->
        userPostBlank(storageApi)(user).flatMap {
            when (it) {
                is T -> Some(it)
                else -> None
            }
        }
    }

fun category(storageApi: StorageApi, message: Message): suspend (Post.Categorizing) -> Option<Category> =
    { post ->
        messageText(message)().flatMap { text ->
            val category = storageApi.categoryApi.searchCategories(
                when (post.category) {
                    is Category.Unclassified ->
                        CategoryFilter.TopLevel(
                            name = text.exactlyFilter(),
                            scope = MetadataScope.Author(post.authorId).anyParentFilter(),
                        )
                    is Category.TopLevel, is Category.Secondary ->
                        CategoryFilter.Secondary(
                            name = text.exactlyFilter(),
                            scope = MetadataScope.Author(post.authorId).anyParentFilter(),
                            parent = post.category.exactlyFilter()
                        )
                }
            ).firstOrNull()
            when (category) {
                null -> None
                else -> Some(category)
            }
        }
    }

fun messageText(message: Message): () -> Option<String> =
    { message.text.toOption() }

fun tags(storageApi: StorageApi, message: Message): suspend () -> Option<List<Tag>> =
    {
        message.text?.split(Regex(",+"))
            ?.map { storageApi.tagApi.getTagByName(it.trim()) }
            ?.takeIf { result -> result.all { it != null } }
            ?.filterNotNull()
            .toOption()
    }

suspend fun initPost(storageApi: StorageApi, message: Message) =
    withParams(fromId(message), user(storageApi)) { _, user ->
        storageApi.postApi.initNewPost(user)
    }

suspend fun completeTitle(storageApi: StorageApi, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank<Post.Initialized>(storageApi),
        { messageText(message)() },
    ) { _, _, postBlank, text ->
        storageApi.postApi.addPostTitle(postBlank, text)
    }

suspend fun completeParagraph(storageApi: StorageApi, getBot: () -> Bot, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank<Post.Filling>(storageApi),
    ) { _, _, postBlank ->
        println(message)
        buildParagraph(message, storageApi, getBot)?.let {
            storageApi.postApi.addPostParagraph(postBlank, it)
        }
    }

suspend fun completeContent(storageApi: StorageApi, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank<Post.Filling>(storageApi),
    ) { _, _, postBlank ->
        storageApi.postApi.addPostCategory(postBlank, Category.Unclassified)
    }

suspend fun completeCategory(storageApi: StorageApi, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank(storageApi),
        category(storageApi, message),
    ) { _, _, postBlank, category ->
        storageApi.postApi.changePostCategory(postBlank, category)
    }

suspend fun completeAllCategories(storageApi: StorageApi, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank<Post.Categorizing>(storageApi),
    ) { _, _, postBlank ->
        storageApi.postApi.addPostTags(postBlank, listOf())
    }

suspend fun autoCompleteCategories(storageApi: StorageApi, post: Post.Categorizing) =
    storageApi.postApi.addPostTags(post, listOf()).toOption()

suspend fun searchTags(storageApi: StorageApi, getBot: () -> Bot, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank<Post.Taggable>(storageApi),
        { messageText(message)() },
    ) { _, _, postBlank, text ->
        val tags = storageApi.tagApi.searchTags(
            TagFilter(
                name = RegexpFilter(Regex(".*${text.removePrefix("/q ")}.*", RegexOption.IGNORE_CASE)),
                scope = MetadataScope.Author(postBlank.authorId).anyParentFilter(),
                category = postBlank.category.anyParentFilter()
            )
        ).joinToString(", ") { it.name }
        getBot().sendMessage(
            ChatId.fromId(message.chat.id),
            when {
                tags.isNotEmpty() -> "➡️ $tags"
                else -> "\uD83E\uDD37 No such tags found"
            }
        )
        postBlank
    }

suspend fun completeTags(storageApi: StorageApi, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank<Post.Taggable>(storageApi),
        { tags(storageApi, message)() },
    ) { _, _, postBlank, tags ->
        val result = storageApi.postApi.replacePostTags(postBlank, tags)
        storageApi.postApi.completePost(result)
    }

suspend fun completePost(storageApi: StorageApi, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        typedUserPostBlank<Post.Taggable>(storageApi),
    ) { _, _, postBlank ->
        storageApi.postApi.completePost(postBlank)
    }

suspend fun cancelPost(storageApi: StorageApi, message: Message) =
    withParams(
        fromId(message),
        user(storageApi),
        userPostBlank(storageApi)
    ) { _, _, postBlank ->
        storageApi.postApi.deletePost(postBlank)
    }

suspend fun buildParagraph(message: Message, storageApi: StorageApi, getBot: () -> Bot): Paragraph? = when {
    message.photo != null ->
        getBot().getFile(message.photo!!.maxByOrNull { it.fileSize ?: 0 }!!.fileId).first?.body()?.result?.filePath
            ?.let { imagePath ->
                getBot().downloadFile(imagePath).first?.body()?.byteStream()?.let {
                    Paragraph.Image(
                        file = storageApi.fileApi.saveFile(it, StorageFileFormat.fromFilename(imagePath)),
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

fun List<MessageEntity.Type>.toTextFormatting(): TextFormatting =
    TextFormatting(
        fontWeight = if (any { it == MessageEntity.Type.BOLD }) FontWeight.BOLD else FontWeight.NORMAL,
        fontStyle = if (any { it == MessageEntity.Type.ITALIC }) FontStyle.ITALIC else FontStyle.NORMAL,
        textDecoration = if (any { it == MessageEntity.Type.UNDERLINE }) TextDecoration.UNDERLINE else TextDecoration.NONE,
    )