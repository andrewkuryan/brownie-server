package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.api.FileStorageApi
import com.gitlab.andrewkuryan.brownie.entity.StorageFile
import com.gitlab.andrewkuryan.brownie.entity.StorageFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

internal val files = mutableMapOf<Int, StorageFile>()

class FileMemoryStorageApi : FileStorageApi {

    var currentFileId = 0

    override suspend fun saveFile(inputStream: InputStream, format: StorageFileFormat): StorageFile {
        return withContext(Dispatchers.IO) {
            val (fileSize, checksum) = Files.newOutputStream(
                Paths.get("files/${currentFileId}.${format.extension}"),
                StandardOpenOption.CREATE
            ).use {
                val chunk = inputStream.readAllBytes()
                it.write(chunk)
                chunk.size to DigestUtils.sha512Hex(chunk)
            }
            currentFileId += 1
            files[currentFileId - 1] = StorageFile(currentFileId - 1, fileSize, format, checksum)
            files.getValue(currentFileId - 1)
        }
    }

    override suspend fun getFileById(id: Int): StorageFile? {
        return files[id]
    }

    override suspend fun getFileContentById(id: Int): File? {
        val file = files[id]
        return if (file != null) {
            Paths.get("files/${file.id}.${file.format.extension}").toFile()
        } else {
            null
        }
    }
}