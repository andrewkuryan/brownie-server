package com.gitlab.andrewkuryan.brownie.entity.post

import com.gitlab.andrewkuryan.brownie.entity.StorageFile
import java.util.*

sealed class Post {
    abstract val id: Int
    abstract val authorId: Int

    sealed class NotCompleted : Post()

    data class Initialized(override val id: Int, override val authorId: Int) : NotCompleted()

    data class Filling(
        override val id: Int,
        override val authorId: Int,
        val title: String,
        val paragraphs: List<Paragraph>,
    ) : NotCompleted()

    data class Categorizing(
        override val id: Int,
        override val authorId: Int,
        val title: String,
        val paragraphs: List<Paragraph>,
        val category: Category,
    ) : NotCompleted()

    data class Taggable(
        override val id: Int,
        override val authorId: Int,
        val title: String,
        val paragraphs: List<Paragraph>,
        val category: Category,
        val tags: List<Tag>,
    ) : NotCompleted()

    data class Active(
        override val id: Int,
        override val authorId: Int,
        val title: String,
        val paragraphs: List<Paragraph>,
        val category: Category,
        val tags: List<Tag>,
        val createdAt: Date,
    ) : Post()
}

sealed class Paragraph {
    data class Text(val items: List<TextItem>) : Paragraph()
    data class Image(val file: StorageFile, val description: String?) : Paragraph()
}

data class TextItem(
    val text: String,
    val formatting: TextFormatting,
)

enum class FontWeight { NORMAL, BOLD }
enum class FontStyle { NORMAL, ITALIC }
enum class TextDecoration { NONE, UNDERLINE }

data class TextFormatting(
    val fontWeight: FontWeight,
    val fontStyle: FontStyle,
    val textDecoration: TextDecoration,
) {
    companion object {
        val NONE = TextFormatting(FontWeight.NORMAL, FontStyle.NORMAL, TextDecoration.NONE)
    }
}