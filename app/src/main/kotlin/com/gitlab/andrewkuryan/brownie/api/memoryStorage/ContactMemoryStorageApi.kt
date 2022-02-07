package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.ContactStorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.entity.user.*
import com.gitlab.andrewkuryan.brownie.logic.generateVerificationCode

internal val contacts = mutableMapOf<Int, UserContact>()

class ContactMemoryStorageApi : ContactStorageApi {

    var currentContactId = 0

    override suspend fun createContact(contactData: ContactData): UserContact.Unconfirmed {
        val contact = UserContact.Unconfirmed(currentContactId, contactData, generateVerificationCode())
        contacts[contact.id] = contact
        currentContactId += 1
        return contact
    }

    override suspend fun confirmContact(contact: UserContact.Unconfirmed): UserContact.Active {
        val newContact = UserContact.Active(contact.id, contact.data)
        contacts[contact.id] = newContact
        return newContact
    }

    override suspend fun regenerateVerificationCode(contact: UserContact.Unconfirmed): UserContact.Unconfirmed {
        val newContact = contact.copy(verificationCode = generateVerificationCode())
        contacts[contact.id] = newContact
        return newContact
    }

    override suspend fun getContactByUniqueKey(uniqueKey: ContactUniqueKey): UserContact.Active? {
        return contacts.values
            .filterIsInstance<UserContact.Active>()
            .find { it.data.identifier == uniqueKey }
    }
}