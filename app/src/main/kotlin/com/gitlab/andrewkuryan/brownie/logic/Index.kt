package com.gitlab.andrewkuryan.brownie.logic

import java.util.*

private val random = Random()

fun generateVerificationCode(): String {
    return (random.nextInt(899999) + 100000).toString()
}