package com.gitlab.andrewkuryan.brownie.routes

import com.gitlab.andrewkuryan.brownie.ClientException
import com.gitlab.andrewkuryan.brownie.api.StorageApi
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*

val fileChecksumKey = AttributeKey<String>("StorageFileChecksum")

fun Route.fileRoutes(
    storageApi: StorageApi,
    gson: Gson,
) {
    get("/api/files/{id}") {
        val fileId = call.parameters["id"]?.toInt() ?: -1
        val file = storageApi.fileApi.getFileById(fileId)
        val fileContent = storageApi.fileApi.getFileContentById(fileId)
        if (file != null && fileContent != null) {
            call.attributes.put(fileChecksumKey, file.checksum)
            call.respondFile(fileContent)
        } else {
            throw ClientException("No such file")
        }
    }
}