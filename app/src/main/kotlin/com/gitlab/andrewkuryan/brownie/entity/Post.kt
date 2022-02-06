package com.gitlab.andrewkuryan.brownie.entity

import java.util.*

sealed class Post {
    abstract val id: Int
    abstract val authorId: Int
}

sealed class NotCompletedPost : Post()

data class InitializedPost(
    override val id: Int,
    override val authorId: Int
) : NotCompletedPost()

data class FillingPost(
    override val id: Int,
    override val authorId: Int,
    val title: String,
    val paragraphs: List<Paragraph>,
) : NotCompletedPost()

data class CategorizingPost(
    override val id: Int,
    override val authorId: Int,
    val title: String,
    val paragraphs: List<Paragraph>,
    val category: Category,
) : NotCompletedPost()

data class TaggablePost(
    override val id: Int,
    override val authorId: Int,
    val title: String,
    val paragraphs: List<Paragraph>,
    val category: Category,
    val tags: List<Tag>,
) : NotCompletedPost()

data class ActivePost(
    override val id: Int,
    override val authorId: Int,
    val title: String,
    val paragraphs: List<Paragraph>,
    val category: Category,
    val tags: List<Tag>,
    val createdAt: Date,
) : Post()

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

sealed class MetadataScope {
    object Global : MetadataScope()
    data class Author(val authorId: Int) : MetadataScope()
}

sealed class Category {
    abstract val id: Int
    abstract val data: CategoryData

    object Unclassified : Category() {
        override val id = 0
        override val data = CategoryData.TopLevel("Unclassified", MetadataScope.Global)
    }

    data class TopLevel(override val id: Int, override val data: CategoryData.TopLevel) : Category()
    data class Secondary(override val id: Int, override val data: CategoryData.Secondary) : Category()
}

sealed class CategoryData {
    abstract val name: String
    abstract val scope: MetadataScope

    data class TopLevel(override val name: String, override val scope: MetadataScope) : CategoryData()
    data class Secondary(override val name: String, override val scope: MetadataScope, val parent: Category) :
        CategoryData()
}

sealed class CategoryFilter : Filter<Category> {
    abstract val name: StringFilter?
    abstract val scope: MetadataScope?

    data class TopLevel(
        override val name: StringFilter? = null,
        override val scope: MetadataScope? = null
    ) : CategoryFilter() {
        override fun apply(value: Category) = super.apply(value) && value is Category.TopLevel
    }

    data class Secondary(
        override val name: StringFilter? = null,
        override val scope: MetadataScope? = null,
        val parent: Category? = null
    ) : CategoryFilter() {

        override fun apply(value: Category) =
            super.apply(value) &&
                    value is Category.Secondary &&
                    (this.parent == null || this.parent == value.data.parent)
    }

    override fun apply(value: Category) =
        (this.name == null || this.name!!.apply(value.data.name)) &&
                (this.scope == null || this.scope == value.data.scope)

    companion object {
        fun exactly(category: Category) = when (category) {
            is Category.TopLevel, is Category.Unclassified -> TopLevel(
                StringFilter.Exactly(category.data.name),
                category.data.scope
            )
            is Category.Secondary -> Secondary(
                StringFilter.Exactly(category.data.name),
                category.data.scope,
                category.data.parent
            )
        }
    }
}

sealed class TagColor {
    object None : TagColor()
    data class RGB(val r: Int, val g: Int, val b: Int) : TagColor()
}

data class TagType(val name: String, val category: Category, val scope: MetadataScope)
data class Tag(
    val type: TagType,
    val name: String,
    val category: Category,
    val color: TagColor,
    val scope: MetadataScope
)

data class TagTypeFilter(
    val name: StringFilter? = null,
    val category: CategoryFilter? = null,
    val scope: MetadataScope? = null,
) : Filter<TagType> {

    override fun apply(value: TagType) =
        (this.name == null || this.name.apply(value.name)) &&
                (this.category == null || this.category.apply(value.category)) &&
                (this.scope == null || this.scope == value.scope)
}

data class TagFilter(
    val type: TagTypeFilter? = null,
    val name: StringFilter? = null,
    val category: CategoryFilter? = null,
    val color: TagColor? = null,
    val scope: MetadataScope? = null,
) : Filter<Tag> {

    override fun apply(value: Tag) =
        (this.type == null || this.type.apply(value.type)) &&
                (this.name == null || this.name.apply(value.name)) &&
                (this.category == null || this.category.apply(value.category)) &&
                (this.color == null || this.color === value.color) &&
                (this.scope == null || this.scope == value.scope)
}