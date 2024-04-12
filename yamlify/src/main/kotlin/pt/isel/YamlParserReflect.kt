package pt.isel

import kotlin.reflect.KClass
import kotlin.reflect.KParameter

/**
 * A YamlParser that uses reflection to parse objects.
 */
class YamlParserReflect<T : Any>(private val type: KClass<T>) : AbstractYamlParser<T>(type) {
    companion object {
        /**
         *Internal cache of YamlParserReflect instances.
         */
        private val yamlParsers: MutableMap<KClass<*>, YamlParserReflect<*>> = mutableMapOf()
        /**
         * Creates a YamlParser for the given type using reflection if it does not already exist.
         * Keep it in an internal cache of YamlParserReflect instances.
         */
        fun <T : Any> yamlParser(type: KClass<T>): AbstractYamlParser<T> {
            return yamlParsers.getOrPut(type) { YamlParserReflect(type) } as YamlParserReflect<T>
        }
    }
    /**
     * Used to get a parser for other Type using the same parsing approach.
     */
    override fun <T : Any> yamlParser(type: KClass<T>) = YamlParserReflect.yamlParser(type)
    /**
     * Creates a new instance of T through the first constructor
     * that has all the mandatory parameters in the map and optional parameters for the rest.
     */
    override fun newInstance(args: Map<String, Any>): T {
        val constructor = type.constructors.first()
        val argsMap = mutableMapOf<KParameter, Any?>()

        for (param in constructor.parameters) {
            val value = args[param.name]
            if (value == null && !param.isOptional && !param.type.isMarkedNullable) {
                throw IllegalArgumentException("Missing properties for ${type.simpleName}")
            }
            argsMap[param] = parseParameterValue(param, value)
        }

        return constructor.callBy(argsMap)
    }

    private fun parseParameterValue(param: KParameter, value: Any?): Any? {
        return when (value) {
            is Map<*, *> -> {
                val nestedClass = param.type.classifier as? KClass<*>
                if (nestedClass != null) {
                    yamlParser(nestedClass).newInstance(value as Map<String, Any>)
                } else {
                    value
                }
            }

            is List<*> -> {
                val nestedClass = param.type.arguments.first().type!!.classifier as? KClass<*>
                if (nestedClass != null) {
                    value.map { yamlParser(nestedClass).newInstance(it as Map<String, Any>) }
                } else {
                    value
                }
            }

            else -> {
                when (param.type.classifier) {
                    Int::class -> value.toString().toInt()
                    Long::class -> value.toString().toLong()
                    Double::class -> value.toString().toDouble()
                    Float::class -> value.toString().toFloat()
                    List::class -> value ?: emptyList<Any>()
                    else -> value
                }
            }
        }
    }

}
