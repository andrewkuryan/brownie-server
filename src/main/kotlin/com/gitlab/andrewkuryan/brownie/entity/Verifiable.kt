package com.gitlab.andrewkuryan.brownie.entity

import arrow.core.Either

class VerificationException(val value: Any, override val message: String? = null) : Exception()

data class Verified<T : Any>(val value: T)

interface Verifiable<T : Any> {

    fun verify(): Either<VerificationException, Verified<T>>

    fun <T : Any> T.correct() = Either.Right(Verified(this))
    fun <T : Any> T.wrong(errorMessage: String? = null) =
        Either.Left(VerificationException(this, errorMessage))
}