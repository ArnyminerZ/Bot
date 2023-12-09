import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import java.util.UUID

inline fun MessageHandlerEnvironment.respondWithError(block: () -> Unit = {}) {
    val errorUUID = UUID.randomUUID()
    bot.sendMessage(
        chatId = ChatId.fromId(message.chat.id),
        text = "❌ No s'ha pogut carregar l'arxiu. Contacta amb un administrador.\n" +
            "Codi de suport: `$errorUUID`",
        parseMode = ParseMode.MARKDOWN
    )
    // todo: insert error in some file
    System.err.println("Could not download file to distribute. Error code: $errorUUID")
    block()
}
