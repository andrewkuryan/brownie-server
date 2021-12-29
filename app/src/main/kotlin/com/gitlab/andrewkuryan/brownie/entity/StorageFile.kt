package com.gitlab.andrewkuryan.brownie.entity

enum class StorageFileFormat(val extension: String) {
    JPG("jpg"), TEXT("txt"), UNDEFINED("");

    companion object {
        fun fromExtension(filename: String): StorageFileFormat {
            val fileExt = filename.substring(filename.lastIndexOf('.') + 1, filename.length)
            return values().filter { it != UNDEFINED }.find { it.extension == fileExt } ?: UNDEFINED
        }
    }
}

data class StorageFile(val id: Int, val size: Int, val format: StorageFileFormat, val checksum: String)