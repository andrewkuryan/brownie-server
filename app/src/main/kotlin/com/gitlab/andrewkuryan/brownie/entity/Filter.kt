package com.gitlab.andrewkuryan.brownie.entity

interface Filter<in T : Any> {
    fun apply(value: T): Boolean
}

sealed class StringFilter : Filter<String> {
    data class Exactly(val query: String) : StringFilter() {
        override fun apply(value: String) = value == query
    }

    data class Regexp(val query: Regex) : StringFilter() {
        override fun apply(value: String) = value.matches(query)
    }
}

enum class FilterOperator { OR, AND }
sealed class FilterMember<T : Any> : Filter<T> {
    data class Operand<T : Any>(val filter: Filter<T>) : FilterMember<T>() {
        override fun apply(value: T) = filter.apply(value)
    }

    data class Expression<T : Any>(
        val operator: FilterOperator,
        val left: FilterMember<T>,
        val right: FilterMember<T>
    ) : FilterMember<T>() {
        override fun apply(value: T) = when (operator) {
            FilterOperator.OR -> left.apply(value) || right.apply(value)
            FilterOperator.AND -> left.apply(value) && right.apply(value)
        }
    }
}

infix fun <T : Any> Filter<T>.or(other: Filter<T>) = FilterMember.Expression(
    FilterOperator.OR,
    FilterMember.Operand(this),
    FilterMember.Operand(other),
)