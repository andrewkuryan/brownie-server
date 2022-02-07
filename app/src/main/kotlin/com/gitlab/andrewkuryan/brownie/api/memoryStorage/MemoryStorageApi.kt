package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.gitlab.andrewkuryan.brownie.entity.*
import com.gitlab.andrewkuryan.brownie.entity.post.*
import com.gitlab.andrewkuryan.brownie.entity.user.*
import java.math.BigInteger

class MemoryStorageApi : StorageApi {
    override val userApi = UserMemoryStorageApi()
    override val contactApi = ContactMemoryStorageApi()
    override val postApi = PostMemoryStorageApi()
    override val fileApi = FileMemoryStorageApi()
    override val categoryApi = CategoryMemoryStorageApi()
    override val tagApi = TagMemoryStorageApi()

    init {
        contacts[0] = UserContact.Active(
            id = 0,
            data = ContactData.Telegram(
                telegramId = 721992046,
                firstName = "Maestro",
                username = "maestro_magic",
            )
        )
        contactApi.currentContactId = 1
        users[0] = User.Active(
            id = 0,
            permissions = UserPermission.DEFAULT + listOf(UserPermission.BrowseAllPosts),
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
            BackendSession.Active(
                publicKey = "MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBmyKJUDrgFE+tJD69scWeR0ntmG0H2m8Why7GvHtNGgchO6IT915dcOsoUMvgaB76Sv8fAMv0KBZBj2IeMeAJttoB6NM1rN+oHzIr8aGImby9aw/oE/7LbPyOgAqGcFw2j4GHr4iPTtFTwzXjxf/seFJUtuKrqP+oe3Fk0h2RhpbzsLA=",
                browserName = "chrome",
                osName = "Mac OS"
            ) to 0

        fileApi.currentFileId = 1
        files[0] = StorageFile(0, 172279, StorageFileFormat.JPG, "")

        categoryApi.currentCategoryId = 4
        categories[1] = Category.TopLevel(1, CategoryData.TopLevel("Programming", MetadataScope.Global))
        categories[2] = Category.TopLevel(2, CategoryData.TopLevel("Bicycles", MetadataScope.Global))
        categories[3] = Category.Secondary(
            3,
            CategoryData.Secondary("Mobile Development", MetadataScope.Global, categories.getValue(1))
        )
        categories[4] = Category.Secondary(
            4,
            CategoryData.Secondary("System Development", MetadataScope.Global, categories.getValue(1))
        )

        tagTypes["Content Type"] = TagType("Content Type", Category.Unclassified, MetadataScope.Global)
        tagTypes["Organization"] = TagType("Organization", Category.Unclassified, MetadataScope.Global)
        tagTypes["Personality"] = TagType("Personality", Category.Unclassified, MetadataScope.Global)
        tagTypes["Programming Language"] = TagType("Programming Language", categories.getValue(1), MetadataScope.Global)

        tags["News"] = Tag(
            tagTypes.getValue("Content Type"),
            "News",
            Category.Unclassified,
            TagColor.RGB(208, 73, 6),
            MetadataScope.Global
        )
        tags["Book"] = Tag(
            tagTypes.getValue("Content Type"),
            "Book",
            Category.Unclassified,
            TagColor.RGB(215, 160, 89),
            MetadataScope.Global
        )
        tags["Mem"] = Tag(
            tagTypes.getValue("Content Type"),
            "Mem",
            Category.Unclassified,
            TagColor.RGB(166, 79, 188),
            MetadataScope.Global
        )
        tags["Jet Brains"] = Tag(
            tagTypes.getValue("Organization"),
            "Jet Brains",
            categories.getValue(1),
            TagColor.RGB(235, 62, 125),
            MetadataScope.Global
        )
        tags["Linus Torvalds"] = Tag(
            tagTypes.getValue("Personality"),
            "Linus Torvalds",
            categories.getValue(4),
            TagColor.RGB(132, 132, 132),
            MetadataScope.Global
        )
        tags["Kotlin"] = Tag(
            tagTypes.getValue("Programming Language"),
            "Kotlin",
            categories.getValue(1),
            TagColor.RGB(144, 72, 235),
            MetadataScope.Global
        )
    }
}

fun dumpDB() {
    println(users)
    println(contacts)
    println(sessions)
    println(posts)
}