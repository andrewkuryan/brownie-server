package com.gitlab.andrewkuryan.brownie.telegram.flow.createPost

import arrow.core.Some
import arrow.core.curried
import arrow.core.partially1
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.entities.BotCommand
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.entity.post.*
import com.gitlab.andrewkuryan.brownie.telegram.FlowVertex
import com.gitlab.andrewkuryan.brownie.telegram.TriggerMessage
import com.gitlab.andrewkuryan.brownie.telegram.flowContext
import com.gitlab.andrewkuryan.brownie.telegram.withParams

enum class CreatePostMessage(override val text: String) : TriggerMessage {
    COMPLETE_CONTENT("☑️ Complete Content"),
    SKIP_CATEGORY("⬇️ Keep Unclassified"),
    COMPLETE_CATEGORIES("☑️ Complete Categories"),
    SKIP_TAGS("⬇️ Skip Adding Tags"),
    CANCEL("❌ Cancel"),
}

fun Dispatcher.createPostFlow(storageApi: StorageApi, getBot: () -> Bot) {
    val initPostVertex = FlowVertex.Root("Create Post")
    val enterTitleVertex = FlowVertex.Internal<Post.Initialized> { "\uD83D\uDD24 Enter post title:" }
    val enterParagraphVertex =
        FlowVertex.Internal<Post.Filling> { "\uD83D\uDCDD\uD83C\uDFDE Enter paragraph (text or image):" }
    val addTopLevelCategoryVertex = FlowVertex.Internal<Post.Categorizing> { post ->
        val topLevelCategories = storageApi.categoryApi
            .searchCategories(CategoryFilter.TopLevel(scope = MetadataScope.Author(post.authorId).anyParentFilter()))
            .joinToString(", ") { it.displayName() }
        "\uD83D\uDD20 Add top level category (one of: $topLevelCategories):"
    }
    val addSecondaryCategoryVertex = FlowVertex.Internal<Post.Categorizing> { post ->
        val nextLevelCategories = storageApi.categoryApi
            .searchCategories(
                CategoryFilter.Secondary(
                    scope = MetadataScope.Author(post.authorId).anyParentFilter(),
                    parent = post.category.exactlyFilter()
                )
            )
            .joinToString(", ") { it.displayName() }
        "\uD83D\uDD21 Add subcategory (one of: ${nextLevelCategories}):"
    }
    val hasSecondaryCategoryVertex = FlowVertex.Decision<Post.Categorizing, Boolean> {
        storageApi.categoryApi.searchCategories(
            CategoryFilter.Secondary(
                scope = MetadataScope.Author(it.authorId).anyParentFilter(),
                parent = it.category.exactlyFilter()
            )
        ).isNotEmpty()
    }
    val addTagsVertex = FlowVertex.Internal<Post.Taggable> {
        """📌 Add tags separated by commas (e.g. <tag1>,<tag2>,...):
            |🔍 Use "/q <query>" to search existing tags
        """.trimMargin()
    }
    val cancelPostVertex = FlowVertex.Leaf<Post> { "\uD83D\uDDD1 Post creation canceled" }
    val completePostVertex = FlowVertex.Leaf<Post.Active> { "✅ Post ${it.id} created successfully" }

    flowContext<CreatePostMessage>(getBot) {
        determineVertex {
            withParams(fromId(it), user(storageApi), userPostBlank(storageApi)) { _, _, postBlank ->
                when {
                    postBlank is Post.Initialized -> enterTitleVertex
                    postBlank is Post.Filling -> enterParagraphVertex
                    postBlank is Post.Categorizing && postBlank.category is Category.Unclassified -> addTopLevelCategoryVertex
                    postBlank is Post.Categorizing -> addSecondaryCategoryVertex
                    postBlank is Post.Taggable -> addTagsVertex
                    else -> null
                }
            }.orNull() ?: initPostVertex
        }

        initPostVertex.byCommand(BotCommand("post", "Create new post"))
            .toVertex(enterTitleVertex).withAction(::initPost.partially1(storageApi))

        enterTitleVertex.byAnyText().toVertex(enterParagraphVertex)
            .withAction(::completeTitle.partially1(storageApi))
        enterTitleVertex.byTrigger(CreatePostMessage.CANCEL).toVertex(cancelPostVertex)
            .withAction(::cancelPost.partially1(storageApi))

        enterParagraphVertex.byAnyMessage().toVertex(enterParagraphVertex)
            .withAction(::completeParagraph.curried()(storageApi)(getBot))
        enterParagraphVertex.byTrigger(CreatePostMessage.COMPLETE_CONTENT).toVertex(addTopLevelCategoryVertex)
            .withAction(::completeContent.partially1(storageApi))
        enterParagraphVertex.byTrigger(CreatePostMessage.CANCEL).toVertex(cancelPostVertex)
            .withAction(::cancelPost.partially1(storageApi))

        addTopLevelCategoryVertex.byAnyText().toVertex(hasSecondaryCategoryVertex)
            .withAction(::completeCategory.partially1(storageApi))
        addTopLevelCategoryVertex.byTrigger(CreatePostMessage.SKIP_CATEGORY).toVertex(addTagsVertex)
            .withAction(::completeAllCategories.partially1(storageApi))
        addTopLevelCategoryVertex.byTrigger(CreatePostMessage.CANCEL).toVertex(cancelPostVertex)
            .withAction(::cancelPost.partially1(storageApi))

        hasSecondaryCategoryVertex.byDecision { hasSecondaryCategories -> hasSecondaryCategories }
            .toVertex(addSecondaryCategoryVertex).withAction { Some(it) }
        hasSecondaryCategoryVertex.byDecision { hasSecondaryCategories -> !hasSecondaryCategories }
            .toVertex(addTagsVertex).withAction(::autoCompleteCategories.partially1(storageApi))

        addSecondaryCategoryVertex.byAnyText().toVertex(hasSecondaryCategoryVertex)
            .withAction(::completeCategory.partially1(storageApi))
        addSecondaryCategoryVertex.byTrigger(CreatePostMessage.COMPLETE_CATEGORIES).toVertex(addTagsVertex)
            .withAction(::completeAllCategories.partially1(storageApi))
        addSecondaryCategoryVertex.byTrigger(CreatePostMessage.CANCEL).toVertex(cancelPostVertex)
            .withAction(::cancelPost.partially1(storageApi))

        addTagsVertex.byTrigger(CreatePostMessage.SKIP_TAGS).toVertex(completePostVertex)
            .withAction(::completePost.partially1(storageApi))
        addTagsVertex.byCommand(BotCommand("q", "Search existing tags (use: /q <query>)"))
            .toVertex(addTagsVertex).withAction(::searchTags.curried()(storageApi)(getBot))
        addTagsVertex.byAnyText().toVertex(completePostVertex)
            .withAction(::completeTags.partially1(storageApi))
        addTagsVertex.byTrigger(CreatePostMessage.CANCEL).toVertex(cancelPostVertex)
            .withAction(::cancelPost.partially1(storageApi))
    }
}

private fun Category.displayName() = when (this) {
    is Category.Unclassified -> "Unclassified"
    is Category.TopLevel -> this.data.name
    is Category.Secondary -> this.data.name
}