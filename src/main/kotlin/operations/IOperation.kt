package operations

import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment

interface IOperation {
    fun MessageHandlerEnvironment.execute()
}
