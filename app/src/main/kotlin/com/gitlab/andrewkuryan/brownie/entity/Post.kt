package com.gitlab.andrewkuryan.brownie.entity

import java.util.*

sealed class PostBlank {
    abstract val id: Int
    abstract val authorId: Int

    data class Initialized(override val id: Int, override val authorId: Int) : PostBlank()
    data class Filling(
        override val id: Int,
        override val authorId: Int,
        val title: String,
        val paragraphs: List<Paragraph>
    ) : PostBlank()
}

data class Post(
    val id: Int,
    val title: String,
    val paragraphs: List<Paragraph>,
    val createdAt: Date,
    val authorId: Int,
)

sealed class Paragraph {
    data class Text(val items: List<TextItem>) : Paragraph()
    data class Image(val file: StorageFile, val description: String?) : Paragraph()
}

data class TextItem(
    val text: String,
    val formatting: TextFormatting,
)

enum class FontWeight {
    NORMAL, BOLD
}

enum class FontStyle {
    NORMAL, ITALIC
}

enum class TextDecoration {
    NONE, UNDERLINE
}

data class TextFormatting(
    val fontWeight: FontWeight,
    val fontStyle: FontStyle,
    val textDecoration: TextDecoration,
) {
    companion object {
        val NONE = TextFormatting(
            fontWeight = FontWeight.NORMAL,
            fontStyle = FontStyle.NORMAL,
            textDecoration = TextDecoration.NONE
        )
    }
}