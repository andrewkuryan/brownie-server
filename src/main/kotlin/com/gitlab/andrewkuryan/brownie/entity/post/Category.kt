package com.gitlab.andrewkuryan.brownie.entity.post

import arrow.core.Either
import com.gitlab.andrewkuryan.brownie.entity.*

sealed class Category {
    object Unclassified : Category()

    sealed class Meaningful : Category(), Verifiable<Meaningful> {
        abstract val id: Int
        abstract val data: CategoryData
    }

    data class TopLevel(override val id: Int, override val data: CategoryData.TopLevel) : Meaningful() {
        override fun verify(): Either<VerificationException, Verified<Meaningful>> = correct()
    }

    data class Secondary(override val id: Int, override val data: CategoryData.Secondary) : Meaningful() {

        override fun verify(): Either<VerificationException, Verified<Meaningful>> =
            when (data.scope.isSubscopeOf(data.parent.data.scope)) {
                true -> correct()
                false -> wrong("${data.scope} is not a subscope of ${data.parent.data.scope}")
            }
    }
}

sealed class CategoryData {
    abstract val name: String
    abstract val scope: MetadataScope

    data class TopLevel(override val name: String, override val scope: MetadataScope) : CategoryData()
    data class Secondary(
        override val name: String,
        override val scope: MetadataScope,
        val parent: Category.Meaningful
    ) : CategoryData()
}

sealed class CategoryFilter : Filter<Category> {

    data class TopLevel(
        val name: Filter<String> = EmptyFilter(),
        val scope: Filter<MetadataScope> = EmptyFilter()
    ) : CategoryFilter() {

        override fun apply(value: Category) = value is Category.TopLevel &&
                applyAll(name, scope, value.data.name, value.data.scope)
    }

    data class Secondary(
        val name: Filter<String> = EmptyFilter(),
        val scope: Filter<MetadataScope> = EmptyFilter(),
        val parent: Filter<Category> = EmptyFilter()
    ) : CategoryFilter() {

        override fun apply(value: Category) = value is Category.Secondary &&
                applyAll(name, scope, parent, value.data.name, value.data.scope, value.data.parent)
    }
}

fun Category.anyParentFilter() = parentCategories()
    .map(Category::exactlyFilter)
    .reduce(Filter<Category>::or)

fun Category.parentCategories(): List<Category> = when (this) {
    is Category.Unclassified -> listOf(Category.Unclassified)
    is Category.TopLevel -> listOf(this, Category.Unclassified)
    is Category.Secondary -> listOf(this) + this.data.parent.parentCategories()
}

fun Category.isSubcategoryOf(other: Category): Boolean = when {
    other is Category.Unclassified -> true
    other is Category.TopLevel && this is Category.TopLevel -> other == this
    other is Category.TopLevel && this is Category.Secondary -> data.parent.isSubcategoryOf(other)
    other is Category.Secondary && this is Category.Secondary -> other == this || data.parent.isSubcategoryOf(other)
    else -> false
}