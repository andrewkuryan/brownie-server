package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.TagStorageApi
import com.gitlab.andrewkuryan.brownie.entity.Filter
import com.gitlab.andrewkuryan.brownie.entity.Tag
import com.gitlab.andrewkuryan.brownie.entity.TagFilter
import com.gitlab.andrewkuryan.brownie.entity.TagType

typealias TagName = String
typealias TagTypeName = String

internal val tagTypes = mutableMapOf<TagTypeName, TagType>()
internal val tags = mutableMapOf<TagName, Tag>()

class TagMemoryStorageApi : TagStorageApi {

    override suspend fun searchTags(filter: Filter<Tag>): List<Tag> {
        return tags.values.filter { filter.apply(it) }
    }

    override suspend fun getTagByName(name: String): Tag? {
        return tags[name]
    }
}