import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.WebAppInfo

fun getSocialButtons(): List<List<KeyboardButton>> {
    return listOf(
        listOf(
            KeyboardButton("Pàgina Web", webApp = WebAppInfo("https://filamagenta.com/")),
            KeyboardButton("Instagram", webApp = WebAppInfo("https://www.instagram.com/filamagenta/"))
        ),
        listOf(
            KeyboardButton("TikTok", webApp = WebAppInfo("https://www.tiktok.com/@filamagenta")),
            KeyboardButton("Facebook", webApp = WebAppInfo("https://www.facebook.com/FilaMagenta/"))
        ),
        listOf(
            KeyboardButton("Twitter", webApp = WebAppInfo("https://twitter.com/filamagenta"))
        )
    )
}
