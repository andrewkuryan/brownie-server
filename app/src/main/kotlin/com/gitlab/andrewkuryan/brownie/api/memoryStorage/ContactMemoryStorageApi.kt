package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.ContactStorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.logic.generateVerificationCode

internal val contacts = mutableMapOf<Int, UserContact>()

class ContactMemoryStorageApi : ContactStorageApi {

    var currentContactId = 0

    override suspend fun createContact(contactData: ContactData): UnconfirmedUserContact {
        val contact = UnconfirmedUserContact(currentContactId, contactData, generateVerificationCode())
        contacts[contact.id] = contact
        currentContactId += 1
        return contact
    }

    override suspend fun confirmContact(contact: UnconfirmedUserContact): ActiveUserContact {
        val newContact = ActiveUserContact(contact.id, contact.data)
        contacts[contact.id] = newContact
        return newContact
    }

    override suspend fun regenerateVerificationCode(contact: UnconfirmedUserContact): UnconfirmedUserContact {
        val newContact = contact.copy(verificationCode = generateVerificationCode())
        contacts[contact.id] = newContact
        return newContact
    }

    override suspend fun getContactByUniqueKey(uniqueKey: ContactUniqueKey): ActiveUserContact? {
        return contacts.values
            .filterIsInstance<ActiveUserContact>()
            .find { it.data.identifier == uniqueKey }
    }
}