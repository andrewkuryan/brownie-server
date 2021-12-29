package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import java.math.BigInteger

class MemoryStorageApi : StorageApi {
    override val userApi = UserMemoryStorageApi()
    override val contactApi = ContactMemoryStorageApi()
    override val postApi = PostMemoryStorageApi()
    override val fileApi = FileMemoryStorageApi()

    init {
        contacts[0] = ActiveUserContact(
            id = 0,
            data = TelegramContactData(
                telegramId = 721992046,
                firstName = "Maestro",
                username = "maestro_magic",
            )
        )
        contactApi.currentContactId = 1
        users[0] = ActiveUser(
            id = 0,
            permissions = listOf(),
            data = UserData(
                login = "andrewkuryan",
                credentials = UserCredentials(
                    salt = "e97db7860af03553f23717556b5c3bd36848268fdc1443d355166d4226d6a83b",
                    verifier = BigInteger(
                        "6468514393918468404626075481415338999013836280098359801846762521685666786496023497176681405128872145408787359340009764825663905285888828127167416238459644127497828159796273986238506752294397362865425016737222355408575593010556916171396313154666846378679105004535686508064089729857052839890827481045659908016307295443711120213178784226681853802359919754789943582631277781640974049126719326069997336722125078269767546321321008870450036491012437298517699073530270993619863460494907565902391868815764403978372316977025062037815217954255293037459724897310529470293372319072762834271164668008533827534474670968218074005024",
                        10
                    )
                )
            ),
            contacts = contacts.values.toList(),
            publicItems = listOf(UserPublicItemType.ID, UserPublicItemType.LOGIN)
        )
        userApi.currentUserId = 1
        sessions["MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBmyKJUDrgFE+tJD69scWeR0ntmG0H2m8Why7GvHtNGgchO6IT915dcOsoUMvgaB76Sv8fAMv0KBZBj2IeMeAJttoB6NM1rN+oHzIr8aGImby9aw/oE/7LbPyOgAqGcFw2j4GHr4iPTtFTwzXjxf/seFJUtuKrqP+oe3Fk0h2RhpbzsLA"] =
            ActiveSession(
                publicKey = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBmyKJUDrgFE+tJD69scWeR0ntmG0H2m8Why7GvHtNGgchO6IT915dcOsoUMvgaB76Sv8fAMv0KBZBj2IeMeAJttoB6NM1rN+oHzIr8aGImby9aw/oE/7LbPyOgAqGcFw2j4GHr4iPTtFTwzXjxf/seFJUtuKrqP+oe3Fk0h2RhpbzsLA=",
                browserName = "chrome",
                osName = "Mac OS"
            ) to 0

        fileApi.currentFileId = 1
        files[0] = StorageFile(0, 172279, StorageFileFormat.JPG, "")
    }
}

fun dumpDB() {
    println(users)
    println(contacts)
    println(sessions)
    println(posts)
}