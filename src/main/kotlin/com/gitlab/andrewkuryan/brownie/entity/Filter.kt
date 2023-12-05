package com.gitlab.andrewkuryan.brownie.entity

interface Filter<in T : Any> {
    fun apply(value: T): Boolean
}

class EmptyFilter<T : Any> : Filter<T> {
    override fun apply(value: T) = true
}

data class ExactlyFilter<T : Any>(val value: T) : Filter<T> {
    override fun apply(value: T) = value == this.value
}

fun <T : Any> T.exactlyFilter() = ExactlyFilter(this)

data class RegexpFilter(val query: Regex) : Filter<String> {
    override fun apply(value: String) = value.matches(query)
}

enum class FilterOperator { OR, AND }

data class FilterExpression<T : Any>(
    val operator: FilterOperator,
    val left: Filter<T>,
    val right: Filter<T>
) : Filter<T> {
    override fun apply(value: T) = when (operator) {
        FilterOperator.OR -> left.apply(value) || right.apply(value)
        FilterOperator.AND -> left.apply(value) && right.apply(value)
    }
}

infix fun <T : Any> Filter<T>.or(other: Filter<T>) =
    FilterExpression(FilterOperator.OR, this, other)

infix fun <T : Any> Filter<T>.and(other: Filter<T>) =
    FilterExpression(FilterOperator.AND, this, other)

fun <T1 : Any, T2 : Any> applyAll(
    f1: Filter<T1>, f2: Filter<T2>,
    v1: T1, v2: T2
) = f1.apply(v1) && f2.apply(v2)

fun <T1 : Any, T2 : Any, T3 : Any> applyAll(
    f1: Filter<T1>, f2: Filter<T2>, f3: Filter<T3>,
    v1: T1, v2: T2, v3: T3
) = f1.apply(v1) && f2.apply(v2) && f3.apply(v3)

fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any> applyAll(
    f1: Filter<T1>, f2: Filter<T2>, f3: Filter<T3>, f4: Filter<T4>,
    v1: T1, v2: T2, v3: T3, v4: T4
) = f1.apply(v1) && f2.apply(v2) && f3.apply(v3) && f4.apply(v4)

fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any> applyAll(
    f1: Filter<T1>, f2: Filter<T2>, f3: Filter<T3>, f4: Filter<T4>, f5: Filter<T5>,
    v1: T1, v2: T2, v3: T3, v4: T4, v5: T5
) = f1.apply(v1) && f2.apply(v2) && f3.apply(v3) && f4.apply(v4) && f5.apply(v5)