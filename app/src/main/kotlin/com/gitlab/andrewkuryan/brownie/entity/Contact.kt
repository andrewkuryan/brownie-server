package com.gitlab.andrewkuryan.brownie.entity

import com.gitlab.andrewkuryan.brownie.BackendField

sealed class UserContact {
    abstract val id: Int
    abstract val data: ContactData
}

data class UnconfirmedUserContact(
        override val id: Int,
        override val data: ContactData,
        @BackendField val verificationCode: String
) : UserContact()

data class ActiveUserContact(
        override val id: Int,
        override val data: ContactData
) : UserContact()

sealed class ContactData

data class EmailContactData(val emailAddress: String) : ContactData()
data class TelegramContactData(val telegramId: Long, val firstName: String, val username: String?) : ContactData()