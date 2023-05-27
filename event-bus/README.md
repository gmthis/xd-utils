## Event Bus

之前使用[mirai](https://github.com/mamoe/mirai)框架时,深感其中事件监听的方便.

故在阅读源码之后,尝试抽离复刻一套出来,该项目就是本次尝试的结果.

纯玩具,不推荐使用.

项目注释十分混乱,~~开摆了~~请谅解.

### 使用

#### Handler 处理器

该项目对外暴露出GlobalEventBus对象用于注册事件处理器和过滤出符合条件的事件.

如下所示, 通过register函数可以注册一个响应默认Event类型的处理器.你需要返回一个Continue值来让这个事件不会在本次调用结束后被卸载.
```kotlin
GlobalEventBus.register { event: Event ->
    println(event)
    HandlerStatus.Continue
}
```
很显然,注册一个只能处理通用类型的处理器毫无意义,因此可以使用带有泛型的版本来指定要响应的类型(包括其子类).你可以返回一个Unregister来在本次调用结束后卸载该处理器.因此示例中的处理器只会执行一次.
```kotlin
class MessageEvent(
    val message: String
): Event

GlobalEventBus.register<MessageEvent> { event: MessageEvent ->
    println(event.message)
    HandlerStatus.Unregister
}
```
通过控制返回值类型,你可以决定什么时候卸载掉处理器.而如果每次编写都需要编写返回值来,再很多时候也会带来心智负担,因此还提供了`registerAutoContinue`和`registerOnlyRunOnce`函数来注册自动继续和只执行一次的处理器.
```kotlin
GlobalEventBus.registerAutoContinue {
    println(this)
}
GlobalEventBus.registerOnlyRunOnce<MessageEvent> { event: MessageEvent ->
    println(this.message)
}
```
如上个代码段所见,this和it在该lambda中均指向事件对象,同时两者也均有带有泛型和不带有泛型两个版本以方便使用.

通过泛型来指定要响应的类型固然很方便,但是过滤后直接使用register可以略去每次都需要添加泛型的苦恼,并在一定程度上复用被过滤后的事件总线.
```kotlin
val filterAfterEventBus = GlobalEventBus.filter<MessageEvent>()
filterAfterEventBus.register{ 
    println(message) // 上下文this为MessageEvent
}
```
filter除了可以通过泛型过滤类型,也可传递条件来过滤符合条件的事件,你也可以混合使用两者.
```kotlin
// 如果filter不指定泛型,则就是调用其EventBus的泛型类型,
// 在这里因为没有父级过滤,如果不指定泛型,则为Event
GlobalEventBus.filter<MessageEvent>{
    // this与it均为事件对象
    message.startsWith("m") // 过滤出message以m开头的MessageEvent
}.register{
    println(message) // 上下文this为MessageEvent
}
```
所有的register事件都会返回一个Handler对象,虽然绝大多数时间内你都不会需要操作这个对象,但是仍然提供了`unregister`和`register`函数来显示的注销和注册一个事件处理器.

注意,在任何时间都不推荐使用这两个函数(除非继承Handler来自定义Handler对象),应尽可能地使用EventBus提供的函数(虽然他们实质上最终调用了同一函数).

此外,注册和调用unregister注销都不是实施的,为了保证线程安全,这两者的实现同时通过在只允许一个并发的协程作用域上启动一个新协程来实现的.当处理协程挂起时,协程调度器执行注册和注销的协程时才会真正的将处理器注册/注销.(不过因为我的协程功底实在太烂,所以我认为这一段代码有一定的风险.)

### Event 事件

实现Event接口定义事件,调用`broadcast`函数来广播事件.广播这个行为是可能被挂起的,因为事件处理器循环同时只会处理一个事件.

该接口是个纯粹的标记接口,除了通过扩展函数提供了`broadcast`函数以外,没有任何实际的内容,如果希望事件可以被拦截,则可以继承InterceptableEvent,该抽象类实现了InterceptableEventInterface接口,InterceptableEventInterface接口定义了拦截相关的内容,如果你的事件类没有空余的位置继承InterceptableEvent,则可以直接实现InterceptableEventInterface接口来让事件可以被拦截.

对于可以被拦截的事件,直接在处理器中调用`event.intercept()`即可停止该事件的广播.

## 结尾
该项目依赖于[kotlinx-coroutines](https://github.com/Kotlin/kotlinx.coroutines),如果要使用,请保证依赖中存在kotlin协程.

你可以阅读[mirai](https://github.com/mamoe/mirai)[事件](https://github.com/mamoe/mirai/tree/dev/mirai-core/src/commonMain/kotlin/event)相关的源码以参阅更为详细与可靠的代码.
