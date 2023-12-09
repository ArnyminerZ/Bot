package operations

import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.network.ResponseError
import com.github.kotlintelegrambot.network.fold
import java.io.File
import respondWithError
import storage.Registry
import storage.UnzipUtils

object DistributeOperation : IOperation {
    data class FileToDistribute(
        val userName: String,
        val file: File,
        val userId: Long
    )

    override fun MessageHandlerEnvironment.execute() {
        val document = message.document
        if (document == null || document.mimeType != "application/zip") {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "❌ Per favor, envia un fitxer vàlid."
            )
        } else {
            bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "✅ Fitxer rebut, processant..."
            )
            println("Received file to distribute. Downloading...")
            val bytes = bot.downloadFileBytes(document.fileId)
            if (bytes == null) {
                respondWithError()
            } else {
                val tempFile = File.createTempFile("filamagenta", document.fileName)
                val targetDir = File(tempFile.parentFile, tempFile.nameWithoutExtension)
                try {
                    println("File downloaded. Temp path: $tempFile")
                    tempFile.outputStream().use { it.write(bytes) }

                    println("Unzipping file...")
                    UnzipUtils.unzip(tempFile, targetDir)

                    val unprocessableFiles = mutableListOf<File>()
                    val toDistribute = mutableListOf<FileToDistribute>()
                    val errors = mutableListOf<ResponseError>()

                    println("Collecting names...")
                    val files = targetDir.listFiles()!!
                    for (file in files) {
                        if (!file.isFile) {
                            unprocessableFiles.add(file)
                            println("- Not a file: ${file.nameWithoutExtension}")
                            continue
                        }
                        val pieces = file.nameWithoutExtension.split("-")
                        if (pieces.size != 2) {
                            unprocessableFiles.add(file)
                            println("- Invalid file name: ${file.nameWithoutExtension}")
                            continue
                        }
                        val (name, phone) = pieces
                        val userData = Registry.findByPhone(phone)
                        if (userData == null) {
                            unprocessableFiles.add(file)
                            println("- User not registered: ${file.nameWithoutExtension}")
                        } else {
                            toDistribute.add(
                                FileToDistribute(
                                    name, file, userData.userId
                                )
                            )
                        }
                    }

                    println("Lost ${unprocessableFiles.size} files. Got ${toDistribute.size} to distribute.")
                    println(toDistribute.joinToString("\n") { it.toString() })

                    for (data in toDistribute) {
                        val telegramFile = TelegramFile.ByFile(data.file)
                        bot.sendDocument(
                            chatId = ChatId.fromId(data.userId),
                            document = telegramFile,
                            caption = message.caption
                        ).fold { error ->
                            toDistribute.remove(data)
                            errors.add(error)
                            System.err.println("Could not send file. Error:")
                            error.errorBody?.byteStream()?.readBytes()?.decodeToString()?.let(System.err::println)
                            error.exception?.printStackTrace()
                        }
                    }

                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "✅ S'han distribuit ${toDistribute.size} fitxers."
                    )
                    if (unprocessableFiles.isNotEmpty()) {
                        val info = unprocessableFiles.joinToString("\n- ") { it.toRelativeString(targetDir) }
                        bot.sendMessage(
                            chatId = ChatId.fromId(message.chat.id),
                            text = "❌ No s'han pogut processar ${unprocessableFiles.size} fitxers.\n- $info"
                        )
                    }
                    if (errors.isNotEmpty()) {
                        val info = errors.joinToString("\n- ") { it.toString() }
                        bot.sendMessage(
                            chatId = ChatId.fromId(message.chat.id),
                            text = "❌ No s'han pogut distribuir ${unprocessableFiles.size} fitxers.\n- $info"
                        )
                    }
                } finally {
                    if (tempFile.exists()) tempFile.delete()
                    if (targetDir.exists()) targetDir.deleteRecursively()
                }
            }
        }
    }
}
