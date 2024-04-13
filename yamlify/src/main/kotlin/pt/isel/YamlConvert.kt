package pt.isel

import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class YamlConvert (val parser: KClass<out YamlPropertyParser<*>>)

interface YamlPropertyParser<T> {
    fun parse(value: String): T
}