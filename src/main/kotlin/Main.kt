import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import kotlin.system.exitProcess
import operations.DistributeOperation
import operations.Operations
import storage.Registry

/**
 * Holds all currently running operations for each chat id.
 */
var operations: Map<Long, Operations> = emptyMap()

/**
 * Holds the ids of all the chats that have been unlocked by entering the correct password.
 */
var isUnlocked: List<Long> = emptyList()

fun main() {
    val apiToken: String? = System.getenv("TOKEN")
    if (apiToken == null) {
        System.err.println("TOKEN environment variable must be set.")
        exitProcess(1)
    }

    val databasePath: String? = System.getenv("DATABASE")
    if (databasePath == null) {
        System.err.println("DATABASE environment variable must be set.")
        exitProcess(1)
    }

    val masterPassword: String? = System.getenv("PASSWORD")
    if (masterPassword == null) {
        System.err.println("PASSWORD environment variable must be set.")
        exitProcess(1)
    }

    Registry.update()

    println("Initializing bot...")
    val bot = bot {
        token = apiToken

        dispatch {
            command("start") {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Hola, sóc el bot 🤖 de Telegram de la Filà Magenta ☪.\n" +
                        "Pots veure informació sobre els comandaments disponibles al teclat ⌨️, o escrivint /help"
                )

                val username = message.from?.username
                if (username != null && !Registry.has(username)) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "ℹ️ Encara no estàs registrat, no rebras missatges distribuïts.\n" +
                            "Fes servir el comandament /register per a fer-ho el més prompte possible."
                    )
                }
            }
            command("help") {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "*Comandaments lliures:*\n" +
                        "- /start: Mostra el missatge inicial\n" +
                        "- /help: Mostra aquest missatge, ajuda sobre les opcions del bot\n" +
                        "- /social: Mostra informació sobre les xarxes socials oficials de la filà\n" +
                        "- /cancel: Cancel·la l'operació actual en cas d'haver alguna pendent\n" +
                        "- /unlock: Desbloqueja els comandaments segurs. Cal introduïr la contrasenya correcta\n" +
                        "\n" +
                        "*Comandaments segurs:*\n" +
                        "- /distribute: Activa el mode de distribució, escriu el comandament per a saber més informació",
                    parseMode = ParseMode.MARKDOWN
                )
            }
            command("register") {
                val phone = args.getOrNull(0)

                // Make sure a phone is specified
                if (phone == null) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "❌ Per favor, indica un telèfon mòbil que registrar."
                    )
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "ℹ️ Ús correcte del comandament: /register <mòbil>"
                    )
                    return@command
                }

                // Make sure the phone number is valid
                if (phone.length != 9) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "❌ Per favor, indica un telèfon mòbil vàlid.\n" +
                            "No inclogues el prefixe del país (+34), ni deixes espais entre els números."
                    )
                    return@command
                }

                val username = message.from?.username
                if (username == null) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "❌ No disposes d'un nom d'usuari que registrar"
                    )
                } else {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "ℹ️ Actualitzant el contacte..."
                    )
                    Registry.set(username, phone, message.from!!.id)
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "ℹ️ Registre actualitzat!\n" +
                            "- Nom d'usuari: $username\n" +
                            "- Mòbil: $phone"
                    )
                }
            }
            command("social") {
                val keyboardMarkup = KeyboardReplyMarkup(keyboard = getSocialButtons(), resizeKeyboard = true)
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "*Xarxes socials oficials:*\n" +
                        "Pàgina web 🌐: https://filamagenta.com/\n" +
                        "Instagram 📷: https://www.instagram.com/filamagenta/\n" +
                        "TikTok 📱: https://www.tiktok.com/@filamagenta\n" +
                        "Facebook 🗣️: https://www.facebook.com/FilaMagenta/\n" +
                        "X 🐦️: https://twitter.com/filamagenta",
                    parseMode = ParseMode.MARKDOWN,
                    replyMarkup = keyboardMarkup
                )
            }
            command("cancel") {
                val operation = operations[message.chat.id]
                if (operation == null) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "❌ No hi ha cap operació pendent"
                    )
                } else {
                    operations = operations.toMutableMap().also {
                        it.remove(message.chat.id)
                    }
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "✅ Operació cancel·lada"
                    )
                }
            }
            command("unlock") {
                val password = args.getOrNull(0)

                if (password == masterPassword) {
                    isUnlocked = isUnlocked.toMutableList().also {
                        it.add(message.chat.id)
                    }
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "🔓 Seguretat desbloquejada. Pots accedir als comandaments segurs."
                    )
                } else {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "❌ Contrasenya incorrecta."
                    )
                }
            }

            command("distribute") {
                if (!isUnlocked.contains(message.chat.id)) {
                    bot.sendMessage(
                        chatId = ChatId.fromId(message.chat.id),
                        text = "🔒 L'accés a aquest comandament està bloquejat." +
                            "Fes servir el comandament /unlock per a desbloquejar l'accés."
                    )
                    return@command
                }

                operations = operations.toMutableMap().also {
                    it[message.chat.id] = Operations.DISTRIBUTE
                }

                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "✅ S'ha activat el mode de distribució. Adjunta un fitxer ZIP amb els arxius a distribuïr."
                )
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "ℹ️ Recorda que el format del nom dels arxius ha de ser:\n" +
                        "- <nom>-<mòbil/usuari>.<format>"
                )
            }

            message {
                val operation = operations[message.chat.id]
                when (operation) {
                    Operations.DISTRIBUTE -> with(DistributeOperation) { execute() }
                    else -> { }
                }
            }

            telegramError {
                System.err.println("An error has happened!")
                System.err.println("Error/%s: %s".format(error.getType().name, error.getErrorMessage()))
            }
        }
    }
    println("Starting poll")
    bot.startPolling()
}
