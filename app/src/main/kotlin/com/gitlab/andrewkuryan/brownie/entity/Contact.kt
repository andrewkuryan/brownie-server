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

sealed class ContactData {
    abstract val identifier: ContactUniqueKey
}

data class EmailContactData(val emailAddress: String) : ContactData() {
    override val identifier: ContactUniqueKey
        get() = EmailContactKey(emailAddress)
}

data class TelegramContactData(val telegramId: Long, val firstName: String, val username: String?) : ContactData() {
    override val identifier: ContactUniqueKey
        get() = TelegramContactKey(telegramId)
}

sealed class ContactUniqueKey

data class EmailContactKey(val emailAddress: String) : ContactUniqueKey()
data class TelegramContactKey(val telegramId: Long) : ContactUniqueKey()