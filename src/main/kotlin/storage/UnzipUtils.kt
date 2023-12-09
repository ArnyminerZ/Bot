package storage

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

object UnzipUtils {
    /**
     * Size of the buffer to read/write data
     */
    private const val BUFFER_SIZE = 4096

    fun unzip(zipFilePath: File, destDirectory: File) {
        if (!destDirectory.exists()) {
            destDirectory.mkdirs()
        }

        ZipFile(zipFilePath).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val filePath = File(destDirectory, entry.name)

                    if (!entry.isDirectory) {
                        // if the entry is a file, extracts it
                        extractFile(input, filePath)
                    } else {
                        // if the entry is a directory, make the directory
                        filePath.mkdir()
                    }
                }
            }
        }
    }

    private fun extractFile(inputStream: InputStream, destFile: File) {
        val bos = destFile.outputStream().buffered()
        val bytesIn = ByteArray(BUFFER_SIZE)
        var read: Int
        while (inputStream.read(bytesIn).also { read = it } != -1) {
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }
}
