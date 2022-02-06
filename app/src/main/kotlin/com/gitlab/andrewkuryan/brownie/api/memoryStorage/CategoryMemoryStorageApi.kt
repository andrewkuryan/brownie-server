package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.CategoryStorageApi
import com.gitlab.andrewkuryan.brownie.entity.Category
import com.gitlab.andrewkuryan.brownie.entity.CategoryFilter
import com.gitlab.andrewkuryan.brownie.entity.Filter

typealias CategoryId = Int

internal val categories = mutableMapOf<CategoryId, Category>()

class CategoryMemoryStorageApi : CategoryStorageApi {

    var currentCategoryId = 0

    override suspend fun searchCategories(filter: Filter<Category>): List<Category> {
        return categories.values.filter { filter.apply(it) }
    }
}