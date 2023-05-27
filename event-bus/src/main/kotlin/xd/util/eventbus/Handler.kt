package xd.util.eventbus

import xd.util.eventbus.annotation.UnknownSideEffects
import kotlin.reflect.KClass

/**
 * 示意处理的优先度等级,共五档
 *
 * @property FirstOne 唯一的,该事件永远第一个执行,你只可以注册一个该等级的事件.
 * @property FirstPriority 第一优先级.
 * @property SecondaryPriority 次要优先级.
 * @property DefaultPriority 默认优先级.
 * @property Unimportant 低优先级.
 * @property AllowNotRun 最低优先级,可能不会运行(开玩笑的,不被拦截的话肯定会运行的).
 * @property LastOne 唯一的, 该事件永远第一个执行,且无论事件是否被拦截均会被调用.
 */
enum class HandlerLevel{
    /**
     * 唯一的,该事件永远第一个执行,你只可以注册一个该等级的事件.
     */
    FirstOne,

    /**
     * 第一优先级.
     */
    FirstPriority,

    /**
     * 次要优先级.
     */
    SecondaryPriority,

    /**
     * 默认优先级.
     */
    DefaultPriority,

    /**
     * 低优先级.
     */
    Unimportant,

    /**
     * 最低优先级,可能不会运行(开玩笑的,不被拦截的话肯定会运行的).
     */
    AllowNotRun,

    /**
     * 唯一的, 该事件永远第一个执行,且无论事件是否被拦截均会被调用.
     */
    LastOne,
}

/**
 * 事件优先级冲突异常
 */
class HandlerLevelException(message: String?): Exception(message)

/**
 * 处理器状态,表示此次执行完成后,下一次是否会正常执行
 *
 * @property Continue 下次仍然执行
 * @property Unregister 此次执行完成后卸载该事件
 */
enum class HandlerStatus{
    /**
     * 下次仍然执行
     */
    Continue,

    /**
     * 此次执行完成后卸载该事件
     */
    Unregister
}

/**
 * ### 事件处理器
 *
 * 共有五档优先级,事件广播时会按照从高到低的顺序处理,当同一等级上存在多个处理器时,按照注册顺序处理.
 *
 * @property handler (event: [E]) -> [Unit] 处理事件的方法.
 * @property level 优先级.
 * @see HandlerLevel
 */
open class Handler <E: Event>(
    val clazz: KClass<out E>,
    val level: HandlerLevel,
    private val handler: E.(event: E) -> HandlerStatus
) {

    /**
     * 处理指定的事件
     *
     * @param event 被处理的事件
     */
    fun run(event: E): HandlerStatus = event.handler(event)


}

/**
 * 注册该事件处理器.默认实现下,如果已注销则不会发生任何事情.
 *
 * 不推荐使用该函数,除非你是继承[Handler]后客制化的[Handler],否则更推荐使用[register][EventBus.register]人性化的注册处理器.
 *
 * 注意!这个操作并不是绝对安全的(因为没搞明白limitedParallelism函数,不过感觉应该是安全的).
 *
 * @receiver [Handler]
 */
@UnknownSideEffects
fun <E: Event> Handler<E>.register(container: HandlerContainer<E>? = null){
    if (container == null)
        EventBusImpl.instance.registerHandler(this)
    else
        container.register(this)
}

/**
 * 注销该事件处理器.默认实现下,如果未注册或者已注销则不会发生任何事情.
 *
 * 注意!这个操作并不是绝对安全的(因为没搞明白limitedParallelism函数,不过感觉应该是安全的).
 *
 * @receiver [Handler]
 */
@OptIn(UnknownSideEffects::class)
@Suppress("UNCHECKED_CAST")
fun <E: Event> Handler<E>.unregister(container: HandlerContainer<E>? = null){
    if (container == null)
        EventBusImpl.instance.unregister(this as Handler<Event>)
    else
        container.unregister(this)
}