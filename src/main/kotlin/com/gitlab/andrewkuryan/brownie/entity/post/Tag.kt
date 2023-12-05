package com.gitlab.andrewkuryan.brownie.entity.post

import arrow.core.flatMap
import com.gitlab.andrewkuryan.brownie.entity.*

data class TagType(val name: String, val category: Category, val scope: MetadataScope) : Verifiable<TagType> {

    override fun verify() = when (category) {
        is Category.Unclassified -> correct()
        is Category.Meaningful -> when (scope.isSubscopeOf(category.data.scope)) {
            true -> correct()
            false -> wrong("$scope is not a subscope of ${category.data.scope}")
        }
    }
}

sealed class TagColor {
    object None : TagColor()
    data class RGB(val r: Int, val g: Int, val b: Int) : TagColor()
}

data class Tag(
    val type: TagType,
    val name: String,
    val category: Category,
    val scope: MetadataScope,
    val color: TagColor = TagColor.None,
) : Verifiable<Tag> {

    override fun verify() = when (category.isSubcategoryOf(type.category)) {
        true -> correct()
        false -> wrong("$category is not a subcategory of ${type.category}")
    }.flatMap {
        when (category) {
            is Category.Unclassified -> correct()
            is Category.Meaningful -> when (scope.isSubscopeOf(category.data.scope)) {
                true -> correct()
                false -> wrong("$scope is not a subscope of ${category.data.scope}")
            }
        }
    }
}

data class TagFilter(
    val type: Filter<TagType> = EmptyFilter(),
    val name: Filter<String> = EmptyFilter(),
    val category: Filter<Category> = EmptyFilter(),
    val color: Filter<TagColor> = EmptyFilter(),
    val scope: Filter<MetadataScope> = EmptyFilter(),
) : Filter<Tag> {

    override fun apply(value: Tag) = applyAll(
        type, name, category, color, scope,
        value.type, value.name, value.category, value.color, value.scope
    )
}