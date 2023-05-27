package xd.util.eventbus.annotation

@RequiresOptIn("该函数在编写时被认为可能出现出乎意料的问题,请阅读源码确定安全后谨慎使用.", level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
/**
 * 被标记的函数在编写时被认为可能出现出乎意料的问题,请阅读源码确定安全后谨慎使用.
 *
 * 憋说了,就我这破水平这个注解得在项目里满天乱飞
 */
annotation class UnknownSideEffects
