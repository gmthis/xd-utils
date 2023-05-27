import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import xd.util.eventbus.*

open class MessageEvent(
    val text: String,
    val index: Int
): InterceptableEvent()

class MessageEvent2(
    text: String,
    index: Int,
    val flags: Boolean
): MessageEvent(text, index)

data class IntEvent(
    val int: Int
): Event

fun main() = runBlocking {
    GlobalEventBus.filter<MessageEvent>().register {
        println(this.text)
        HandlerStatus.Unregister
    }

    GlobalEventBus.filter<MessageEvent> {
        true
    }.registerOnlyRunOnce(
        level = HandlerLevel.SecondaryPriority
    ) {
        println(this.index)
    }

    delay(1000)
    MessageEvent("message事件1", index = 2).broadcast()
    delay(1000)
    IntEvent(1).broadcast()
    delay(1000)
    MessageEvent2("message事件2", index = 1, flags = true).broadcast()
    delay(1000)
    IntEvent(2).broadcast()

    Unit
}