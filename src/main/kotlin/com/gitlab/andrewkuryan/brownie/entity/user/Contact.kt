package com.gitlab.andrewkuryan.brownie.entity.user

import com.gitlab.andrewkuryan.brownie.BackendField

sealed class UserContact {
    abstract val id: Int
    abstract val data: ContactData

    data class Unconfirmed(
        override val id: Int,
        override val data: ContactData,
        @BackendField val verificationCode: String
    ) : UserContact()

    data class Active(
        override val id: Int,
        override val data: ContactData
    ) : UserContact()
}

sealed class ContactData {
    abstract val identifier: ContactUniqueKey

    data class Email(val emailAddress: String) : ContactData() {
        override val identifier = ContactUniqueKey.Email(emailAddress)
    }

    data class Telegram(val telegramId: Long, val firstName: String, val username: String?) : ContactData() {
        override val identifier = ContactUniqueKey.Telegram(telegramId)
    }
}

sealed class ContactUniqueKey {
    data class Email(val emailAddress: String) : ContactUniqueKey()
    data class Telegram(val telegramId: Long) : ContactUniqueKey()
}