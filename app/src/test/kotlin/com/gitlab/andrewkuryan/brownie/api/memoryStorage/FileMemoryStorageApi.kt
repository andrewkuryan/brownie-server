package com.gitlab.andrewkuryan.brownie.api.memoryStorage

import com.gitlab.andrewkuryan.brownie.entity.StorageFileFormat
import kotlinx.coroutines.runBlocking
import org.junit.Test

class FileMemoryStorageApiTest {

    private val api = FileMemoryStorageApi()

    @Test
    fun saveFileTest() = runBlocking {
        val bytes = byteArrayOf(65, 66, 67, 68, 69, 70, 71, 72, 73, 74)
        val result = api.saveFile(bytes.inputStream(),  StorageFileFormat.TEXT)
        println(result)
    }
}