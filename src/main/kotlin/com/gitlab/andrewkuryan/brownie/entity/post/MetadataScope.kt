package com.gitlab.andrewkuryan.brownie.entity.post

import com.gitlab.andrewkuryan.brownie.entity.Filter
import com.gitlab.andrewkuryan.brownie.entity.exactlyFilter
import com.gitlab.andrewkuryan.brownie.entity.or

sealed class MetadataScope {
    object Global : MetadataScope()

    sealed class Secondary(val parent: MetadataScope) : MetadataScope()

    data class Author(val authorId: Int) : Secondary(Global)
}

fun MetadataScope.anyParentFilter() = parentScopes()
    .map(MetadataScope::exactlyFilter)
    .reduce(Filter<MetadataScope>::or)

fun MetadataScope.parentScopes(): List<MetadataScope> = when (this) {
    is MetadataScope.Global -> listOf(this)
    is MetadataScope.Secondary -> listOf(this) + parent.parentScopes()
}

fun MetadataScope.isSubscopeOf(other: MetadataScope): Boolean = when {
    other is MetadataScope.Global && this is MetadataScope.Global -> true
    other is MetadataScope.Global && this is MetadataScope.Secondary -> parent.isSubscopeOf(other)
    other is MetadataScope.Secondary && this is MetadataScope.Secondary -> this == other || parent.isSubscopeOf(other)
    else -> false
}