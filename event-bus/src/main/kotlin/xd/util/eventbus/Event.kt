package xd.util.eventbus

/**
 * ### 事件标记
 * 实现该接口以注册事件.
 *
 * 该接口的实例可以通过[broadcast]函数进行广播.
 *
 * 虽然未对重复广播进行检查,但是仍然不建议重复广播同一个事件.
 *
 * 示例:
 * ```kotlin
 * data class MessageEvent(
 *     val text: String
 * ): Event
 *
 * //在函数中
 * MessageEvent("new event").broadcast()
 * ```
 *
 * 事件的监听则通过[EventBus]的[register][EventBus.register]函数注册.
 */
interface Event

/**
 * 时间的拦截状态
 *
 * @property Intercepted 被拦截
 * @property NotIntercepted 未被拦截
 */
enum class InterceptStatus{
    /**
     * 被拦截
     */
    Intercepted,

    /**
     * 未被拦截
     */
    NotIntercepted
}

/**
 * 实现此接口表示该事件可以被拦截,当调用的[intercept]方法后,事件将不会在被之后的事件处理器处理.
 *
 * 如果你的事件有空余的位置可以继承父类,那么可以选择继承[InterceptableEvent]来避免重复编写代码.
 *
 * @see InterceptableEvent
 */
interface InterceptableEventInterface: Event{

    /**
     * 当前时间的拦截状态
     *
     * @see InterceptStatus
     */
    val interceptStatus: InterceptStatus

    /**
     * 拦截当前事件
     *
     * @return [Boolean] 是否拦截成功,如果为false,则表示时间已经是被拦截的状态了
     */
    fun intercept(): Boolean
}

/**
 * 继承该接口的事件可以被拦截,当调用的[intercept]方法后,事件将不会在被之后的事件处理器处理.
 *
 * 你可以重写[intercept]来自定义操作,但一定要在函数内部调用`super.intercept()`.
 *
 * 例子:
 * ```kotlin
 * class MessageEvent: InterceptableEvent()
 *
 * GlobalEventBus.filter<MessageEvent>().register {
 *     intercept()
 * }
 * ```
 *
 * 如果你需要一个其他父类而无法直接继承该类,请则请实现[InterceptableEventInterface]接口来让事件可拦截.
 *
 * @see InterceptableEventInterface
 */
abstract class InterceptableEvent: InterceptableEventInterface{

    private var _interceptStatus = InterceptStatus.NotIntercepted

    final override val interceptStatus: InterceptStatus
        get() = _interceptStatus

    override fun intercept(): Boolean {
        if (_interceptStatus == InterceptStatus.Intercepted){
            return false
        }
        _interceptStatus = InterceptStatus.Intercepted
        return true
    }
}

/**
 * ### 广播事件
 *
 * 调用该函数即可将事件广播至全局事件总线上.事件总线同一时刻只会处理一个事件,其他事件在没有处理完成之前,会被挂起,因此不能保证入队顺序
 *
 * 事件的监听则通过[EventBus]的[register][EventBus.register]函数注册.
 *
 * 注意!这是一个挂起函数,必须在协程中调用.
 *
 * 如果您不熟悉协程请参阅[kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
 *
 * @receiver [Event]
 */
suspend fun <E: Event> E.broadcast(container: EventContainer<E>? = null){
    if (container == null)
        EventBusImpl.instance.broadcast(this)
    else
        container.broadcast(this)
}