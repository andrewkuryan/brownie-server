package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.CategoryStorageApi
import com.gitlab.andrewkuryan.brownie.entity.Filter
import com.gitlab.andrewkuryan.brownie.entity.post.Category

typealias CategoryId = Int

internal val categories = mutableMapOf<CategoryId, Category.Meaningful>()

class CategoryMemoryStorageApi : CategoryStorageApi {

    var currentCategoryId = 0

    override suspend fun searchCategories(filter: Filter<Category>): List<Category.Meaningful> {
        return categories.values.filter { filter.apply(it) }
    }
}