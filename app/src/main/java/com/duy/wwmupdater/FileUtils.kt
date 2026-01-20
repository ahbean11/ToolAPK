package com.duy.wwmupdater

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream

class FileUtils {
    companion object {
        /**
         * Creates a path of nested directories in the given root directory.
         * If directories don't exist, they will be created.
         */
        fun createPath(root: DocumentFile, path: String): DocumentFile? {
            val parts = path.split("/")
            var currentDir = root
            for (part in parts) {
                var nextDir = currentDir.findFile(part)
                if (nextDir == null || !nextDir.isDirectory) {
                    // If directory doesn't exist, create it
                    nextDir = currentDir.createDirectory(part)
                }
                if (nextDir == null) return null
                currentDir = nextDir
            }
            return currentDir
        }

        /**
         * Writes content to a file in the destination directory
         */
        fun writeTextToFile(context: Context, destinationDir: DocumentFile, fileName: String, content: String) {
            // Check if file exists and delete it to overwrite
            val existingFile = destinationDir.findFile(fileName)
            existingFile?.delete()

            // Create new file
            val newFile = destinationDir.createFile("text/plain", fileName)

            newFile?.uri?.let { fileUri ->
                try {
                    context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /**
         * Reads content from a file
         */
        fun readTextFromFile(context: Context, fileUri: Uri): String? {
            return try {
                context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Copies a file from InputStream to DocumentFile
         */
        fun copyFileFromStream(context: Context, inputStream: InputStream, destinationDir: DocumentFile, fileName: String, mimeType: String) {
            // Delete existing file if it exists
            val existingFile = destinationDir.findFile(fileName)
            existingFile?.delete()

            // Create new file
            val newFile = destinationDir.createFile(mimeType, fileName)

            newFile?.uri?.let { fileUri ->
                try {
                    context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}