@file:Suppress("UNCHECKED_CAST")

package xd.util.eventbus

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import xd.util.eventbus.GlobalEventBus.filter
import xd.util.eventbus.HandlerLevel.*
import xd.util.eventbus.annotation.UnknownSideEffects
import kotlin.reflect.KClass

/**
 * ### 事件总线
 *
 * 继承该类以实现事件总线.
 *
 * @see [EventBusImpl]
 * @see [GlobalEventBus]
 */
abstract class EventBus<Base: Event>(val clazz: KClass<out Base>) {

    /**
     * 返回一个过滤后的事件总线,在该总线上注册的事件处理器只会处理被过滤出来的事件.
     *
     * 此外你可以通过带泛型的版本直接指定需要过滤出的类型.
     *
     * @see [AfterFilterEventBus]
     * @param filter 过滤规则,返回true则表示事件通过,会被处理.
     * @return [AfterFilterEventBus] 过滤后的事件总线.
     */
    fun filter(filter: Base.(event: Base) -> Boolean): EventBus<Base> {
        return AfterFilterEventBus(this, filter)
    }

    /**
     * 快速过滤出泛型指定类型的事件.
     *
     * 如果再次过滤,请保证此次过滤的泛型是上次过滤的泛型的子类,否则不会被任何事件触发.
     *
     * 该函数有带有filter参数的重载版本,你仍然可以像使用无泛型的filter那样进行条件过滤.
     *
     * @see [AfterFilterEventBus]
     * @return [AfterFilterEventBus] 过滤后的事件总线.
     */
    @JvmName("filterByName")
    inline fun <reified E: Event> filter(): EventBus<E> {
        return AfterFilterEventBus(this as EventBus<E>) {
            E::class.isInstance(it)
        }
    }

    /**
     * 快速过滤出泛型指定类型的事件.
     *
     * 如果再次过滤,请保证此次过滤的泛型是上次过滤的泛型的子类,否则不会被任何事件触发.
     *
     * 与不带泛型的版本不同,带有泛型的该版本向lambda传入的形参会转换为泛型指定的类型以方便使用.
     *
     * @see [AfterFilterEventBus]
     * @param filter 过滤规则,返回true则表示事件通过,会被处理.
     * @return [AfterFilterEventBus] 过滤后的事件总线.
     */
    @JvmName("filterByName")
    inline fun <reified E: Event> filter(crossinline filter: E.(event: E) -> Boolean): EventBus<E> {
        return AfterFilterEventBus(this as EventBus<E>) {
            if (E::class.isInstance(it)) {
                filter(it)
            } else {
                false
            }
        }
    }

    /**
     * 注册一个新的事件处理器.
     *
     * 事件广播时处理器会按照指定的优先级运行,当同一等级上存在多个处理器时,按照注册顺序处理.
     *
     * lambda的形参类型为泛型指定的类型,你也可以使用不带有泛型的版本,则lambda的形参类型为当前总线泛型指定的类型.
     *
     * lambada返回Continue则下次事件广播会继续调用该处理器,如果返回[HandlerStatus.Unregister],则在本次执行完成后卸载该处理器.
     *
     * 示例:
     * ```kotlin
     * GlobalEventBus.register<Event> {
     *     println("-------------")
     *     println("捕获到事件")
     *     println(it::class)
     *     println(it)
     *     HandlerStatus.Continue
     * }
     * ```
     *
     * @param handler 处理方式.
     * @param level 处理器等级
     * @return [Handler] 处理器,通过该对象来对处理器本身进行操作.
     * @see HandlerLevel
     */
    inline fun <reified E: Event> register(
        level: HandlerLevel = DefaultPriority,
        noinline handler: E.(event: E) -> HandlerStatus
    ) = register(E::class, level, handler)

    /**
     * 注册一个新的事件处理器.
     *
     * 事件广播时处理器会按照指定的优先级运行,当同一等级上存在多个处理器时,按照注册顺序处理.
     *
     * lambda的形参类型为当前总线泛型指定的类型,你也可以使用带有泛型的版本,则lambda的形参类型为泛型指定的类型.
     *
     * lambada返回Continue则下次事件广播会继续调用该处理器,如果返回[HandlerStatus.Unregister],则在本次执行完成后卸载该处理器.
     *
     * 示例:
     * ```kotlin
     * GlobalEventBus.register {
     *     println("-------------")
     *     println("捕获到事件")
     *     println(it::class)
     *     println(it)
     *     HandlerStatus.Continue
     * }
     * ```
     *
     * @param handler 处理方式.
     * @param level 处理器等级
     * @return [Handler] 处理器,通过该对象来对处理器本身进行操作.
     * @see HandlerLevel
     * @see HandlerStatus
     */
    @JvmName("directRegister")
    fun register(level: HandlerLevel = DefaultPriority, handler: Base.(event: Base) -> HandlerStatus) =
        register(clazz, level, handler)

    /**
     * 注册一个新的事件处理器.
     *
     * 事件广播时处理器会按照指定的优先级运行,当同一等级上存在多个处理器时,按照注册顺序处理.
     *
     * lambda的形参类型为当前总线泛型指定的类型,你也可以使用带有泛型的版本,则lambda的形参类型为泛型指定的类型.
     *
     * 该处理器默认会返回[HandlerStatus.Continue]
     *
     * 示例:
     * ```kotlin
     * GlobalEventBus.register {
     *     println("-------------")
     *     println("捕获到事件")
     *     println(it::class)
     *     println(it)
     * }
     * ```
     *
     * @param handler 处理方式.
     * @param level 处理器等级
     * @return [Handler] 处理器,通过该对象来对处理器本身进行操作.
     * @see HandlerLevel
     * @see HandlerStatus
     */
    @JvmName("directRegisterAutoContinue")
    fun registerAutoContinue(level: HandlerLevel = DefaultPriority, handler: Base.(event: Base) -> Unit) =
        register(clazz, level) {
            handler(it)
            HandlerStatus.Continue
        }

    /**
     * 注册一个新的事件处理器.
     *
     * 事件广播时处理器会按照指定的优先级运行,当同一等级上存在多个处理器时,按照注册顺序处理.
     *
     * lambda的形参类型为泛型指定的类型,你也可以使用不带有泛型的版本,则lambda的形参类型为当前总线泛型指定的类型.
     *
     * 该处理器默认会返回[HandlerStatus.Continue]
     *
     * 示例:
     * ```kotlin
     * GlobalEventBus.register {
     *     println("-------------")
     *     println("捕获到事件")
     *     println(it::class)
     *     println(it)
     * }
     * ```
     *
     * @param handler 处理方式.
     * @param level 处理器等级
     * @return [Handler] 处理器,通过该对象来对处理器本身进行操作.
     * @see HandlerLevel
     * @see HandlerStatus
     */
    inline fun <reified E: Event> registerAutoContinue(level: HandlerLevel = DefaultPriority, noinline handler: E.(event: E) -> Unit) =
        register(clazz, level) {
            (it as E).handler(it)
            HandlerStatus.Continue
        }

    /**
     * 注册一个新的事件处理器.
     *
     * 事件广播时处理器会按照指定的优先级运行,当同一等级上存在多个处理器时,按照注册顺序处理.
     *
     * lambda的形参类型为泛型指定的类型,你也可以使用不带有泛型的版本,则lambda的形参类型为当前总线泛型指定的类型.
     *
     * 该处理器默认会返回[HandlerStatus.Unregister]
     *
     * 示例:
     * ```kotlin
     * GlobalEventBus.register {
     *     println("-------------")
     *     println("捕获到事件")
     *     println(it::class)
     *     println(it)
     * }
     * ```
     *
     * @param handler 处理方式.
     * @param level 处理器等级
     * @return [Handler] 处理器,通过该对象来对处理器本身进行操作.
     * @see HandlerLevel
     * @see HandlerStatus
     */
    inline fun <reified E: Event> registerOnlyRunOnce(level: HandlerLevel = DefaultPriority, noinline handler: E.(event: E) -> Unit) =
        register(clazz, level) {
            (it as E).handler(it)
            HandlerStatus.Unregister
        }

    /**
     * 注册一个新的事件处理器.
     *
     * 事件广播时处理器会按照指定的优先级运行,当同一等级上存在多个处理器时,按照注册顺序处理.
     *
     * lambda的形参类型为当前总线泛型指定的类型,你也可以使用带有泛型的版本,则lambda的形参类型为泛型指定的类型.
     *
     * 该处理器默认会返回[HandlerStatus.Unregister]
     *
     * 示例:
     * ```kotlin
     * GlobalEventBus.register {
     *     println("-------------")
     *     println("捕获到事件")
     *     println(it::class)
     *     println(it)
     * }
     * ```
     *
     * @param handler 处理方式.
     * @param level 处理器等级
     * @return [Handler] 处理器,通过该对象来对处理器本身进行操作.
     * @see HandlerLevel
     * @see HandlerStatus
     */
    @JvmName("directRegisterOnlyRunOnce")
    fun registerOnlyRunOnce(level: HandlerLevel = DefaultPriority, handler: Base.(event: Base) -> Unit) =
        register(clazz, level) {
            handler(it)
            HandlerStatus.Unregister
        }

    /**
     * 用于将泛型转换为KClass对象.
     */
    fun <E: Event> register(
        clazz: KClass<out E>,
        level: HandlerLevel,
        handler: E.(event: E) -> HandlerStatus
    ): Handler<E> =
        registerHandler(createHandler(clazz, level, handler))

    /**
     * 注册事件,由子类实现.
     * @see [EventBusImpl.registerHandler]
     */
    abstract fun <E: Event, H: Handler<E>> registerHandler(handler: H): Handler<E>

    /**
     * 创建一个新的处理器,由子类实现.
     * @see [AfterFilterEventBus.createHandler]
     * @see [EventBusImpl.createHandler]
     */
    abstract fun <E: Event> createHandler(
        clazz: KClass<out E>,
        level: HandlerLevel,
        handler: E.(event: E) -> HandlerStatus
    ): Handler<E>
}

/**
 * 实现该接口表示事件可以被广播到该类对象上.
 */
interface EventContainer<Base: Event> {
    /**
     * 广播.
     *
     * @param event 被广播的事件.
     */
    suspend fun broadcast(event: Base)
}

/**
 * 实现该接口表示处理器可以被注册到到该类对象上.
 */
interface HandlerContainer<Base: Event> {
    /**
     * 注销某个事件处理器.
     *
     * 注意!注销不一定是即时的,这由具体实现决定.
     *
     * @param handler 被注销的处理器
     */
    fun unregister(handler: Handler<Base>)

    /**
     * 注册某个事件处理器.
     *
     * 注意!注册不一定是即时的,这由具体实现决定.
     *
     * @param handler 注册的处理器
     */
    fun register(handler: Handler<Base>)
}

/**
 * 实现该接口表示事件可以在该类实力上广播,同时处理器也可以注册到该类上.
 */
interface EventAndHandlerContainer<Base: Event>: EventContainer<Base>, HandlerContainer<Base>

/**
 * ### EventBusImpl
 *
 * 核心实现类,通过单例模式实现.
 *
 * 所有的处理器在没有自行实现[EventBus]的情况下实际注册在该类中.
 * 注意!该类会在init块中启动一个线程以监听事件的广播.
 *
 * @see [EventBus]
 */
internal class EventBusImpl<Base: Event> internal constructor(
    clazz: KClass<out Base>
): EventBus<Base>(clazz), EventAndHandlerContainer<Base> {

    companion object {
        internal val instance by lazy {
            EventBusImpl(Event::class)
        }
    }

    private val eventChannel = Channel<Base>()
    private var firstOneHandler: Handler<Base>? = null
    private var lastOneHandler: Handler<Base>? = null
    private var handlerMap = mapOf<HandlerLevel, MutableCollection<Handler<Base>>>(
        FirstPriority to mutableSetOf(),
        SecondaryPriority to mutableSetOf(),
        DefaultPriority to mutableSetOf(),
        Unimportant to mutableSetOf(),
        AllowNotRun to mutableSetOf()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val coroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private var eventHandlerJob: Job = coroutineScope.launch {
        try {
            while (isActive) {
                val event = eventChannel.receive()
                firstOneHandler?.run(event)
                if (event is InterceptableEvent) {
                    notifyHandlers(event, true)
                } else {
                    notifyHandlers(event)
                }
                lastOneHandler?.run(event)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyHandlers(event: Base, isInterceptableEvent: Boolean = false) {
        for ((_, handlers) in handlerMap) {
            val iterator = handlers.iterator()
            while (iterator.hasNext()) {
                val handler = iterator.next()
                val handlerStatus = if (handler.clazz.isInstance(event))
                    if (isInterceptableEvent)
                        if ((event as InterceptableEvent).interceptStatus == InterceptStatus.NotIntercepted)
                            handler.run(event)
                        else
                            return
                    else
                        handler.run(event)
                else
                    continue
                if (handlerStatus == HandlerStatus.Unregister)
                    iterator.remove()
            }
        }
    }

    override suspend fun broadcast(event: Base) {
        eventChannel.send(event)
    }

    /**
     * 注销某个事件处理器.如果已经被注销则不会发生任何事情.
     *
     * 注意!这个操作并不是绝对安全的(因为没搞明白limitedParallelism函数,不过感觉应该是安全的).
     *
     * @param handler 被注销的处理器
     */
    @UnknownSideEffects
    override fun unregister(handler: Handler<Base>) {
        coroutineScope.launch {
            when (handler.level) {
                FirstOne -> firstOneHandler = null
                LastOne -> lastOneHandler = null
                else -> {
                    handlerMap[handler.level]?.remove(handler)
                }
            }
        }
    }

    override fun <E: Event, H: Handler<E>> registerHandler(handler: H): Handler<E> {
        coroutineScope.launch {
            when (handler.level) {
                FirstOne -> {
                    if (firstOneHandler != null) throw HandlerLevelException("fistOneHandler等级已注册")
                    firstOneHandler = handler as Handler<Base>
                }

                LastOne -> {
                    if (lastOneHandler != null) throw HandlerLevelException("lastOneHandler等级已注册")
                    lastOneHandler = handler as Handler<Base>
                }

                else -> {
                    handlerMap[handler.level]!!.add(handler as Handler<Base>)
                }
            }
        }
        return handler
    }

    override fun register(handler: Handler<Base>) {
        registerHandler(handler)
    }

    /**
     * 创建处理器.请不要调用.
     */
    override fun <E: Event> createHandler(
        clazz: KClass<out E>,
        level: HandlerLevel,
        handler: E.(event: E) -> HandlerStatus
    ): Handler<E> {
        return Handler(clazz, level, handler)
    }
}

/**
 * ### 全局事件总线
 * 所有的事件均可在全局总线上被监听到.
 *
 * 注册的事件会按照注册的优先级顺序依次处理事件,而同一优先级时会按照注册顺序处理,因此请注意优先级和注册顺序.
 *
 * 你可以注册[HandlerLevel.FirstOne]和[HandlerLevel.LastOne]两个等级来在开始和结束时处理事件,详情请看[HandlerLevel]
 *
 * 示例:
 * ```kotlin
 * GlobalEventBus.register<Event> {
 *     println("-------------")
 *     println("捕获到事件")
 *     println(it::class)
 *     println(it)
 * }
 * ```
 *
 * 也可以通过[filter]函数监听过滤后的事件.
 *
 * 例如:
 * ```kotlin
 * GlobalEventBus.filter {
 *     if (it is MessageEvent) it.text.startsWith("message") else false
 * }.register<MessageEvent> {
 *     println("-----------------")
 *     println("捕获到被过滤的事件")
 *     println(it::class)
 *     println(it.text)
 * }
 * ```
 *
 * 内部函数基本代理到[EventBusImpl]完成实际的操作.
 *
 * @see [EventBus]
 * @see [EventBusImpl]
 * @see HandlerLevel
 */
object GlobalEventBus: EventBus<Event>(Event::class) {

    private val delegate = EventBusImpl.instance

    /**
     * 注册处理器.
     *
     * 虽然可以在外部被调用,但是如果不继承实现客制化的[Handler]的话,请不要调用该函数注册!
     */
    override fun <E: Event, H: Handler<E>> registerHandler(handler: H): Handler<E> =
        delegate.registerHandler(handler)

    /**
     * 创建处理器.请不要调用,请使用[register]函数通过人性化的方式注册处理器
     */
    override fun <E: Event> createHandler(
        clazz: KClass<out E>,
        level: HandlerLevel,
        handler: E.(event: E) -> HandlerStatus
    ): Handler<E> =
        delegate.createHandler(clazz, level, handler)
}

/**
 * ### 过滤后事件总线
 * 该类内部不会保存任何处理器,处理器均会注册在其父总线中.
 *
 * @property filter 过滤规则,返回true则表示事件通过,会被处理.
 * @property father 父总线,用于调用父总线的注册方法.
 * @see [EventBus]
 */
class AfterFilterEventBus<Base: Event>(
    private val father: EventBus<Base>,
    private val filter: Base.(event: Base) -> Boolean
): EventBus<Base>(father.clazz) {

    /**
     * 注册处理器,会将处理器注册到父总线.请不要调用.
     */
    override fun <E: Event, H: Handler<E>> registerHandler(handler: H): Handler<E> =
        father.registerHandler(handler)

    /**
     * 创建一个带有过滤器的处理器,内部会在运行时检查事件是否符合指定的条件链.请不要调用.
     */
    override fun <E: Event> createHandler(
        clazz: KClass<out E>,
        level: HandlerLevel,
        handler: E.(event: E) -> HandlerStatus
    ): Handler<E> =
        father.createHandler(clazz, level) {
//            这里需要强转一下,一般不会报无法转换类的错.
            if ((this as Base).filter(it as Base))
                handler(it)
            else
                HandlerStatus.Continue
        }
}