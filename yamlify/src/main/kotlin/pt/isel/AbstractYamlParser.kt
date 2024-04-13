package pt.isel

import java.io.BufferedReader
import java.io.Reader
import kotlin.reflect.KClass

abstract class AbstractYamlParser<T : Any>(private val type: KClass<T>) : YamlParser<T> {
    /**
     * Used to get a parser for other Type using this same parsing approach.
     */
    abstract fun <T : Any> yamlParser(type: KClass<T>) : AbstractYamlParser<T>
    /**
     * Creates a new instance of T through the first constructor
     * that has all the mandatory parameters in the map and optional parameters for the rest.
     */
    abstract fun newInstance(args: Map<String, Any>): T

    final override fun parseObject(yaml: Reader): T {
        BufferedReader(yaml).use { reader ->
            val args = mutableMapOf<String, Any>()
            val obj = mutableListOf<String>()
            var lastIndentation: Int? = null
            var lastKey: String? = null

            reader.forEachLine { line ->
                if (line.isBlank()) {
                    return@forEachLine
                }

                val indentation = line.takeWhile { it == ' ' }.length

                if (lastIndentation == null) {
                    lastIndentation = indentation
                }

                if (indentation > lastIndentation!!) {
                    obj.add(line)
                } else {
                    if (obj.isNotEmpty()) {
                        val res = parseBlock(obj, lastKey)
                        args[lastKey!!] = res.getOrDefault(lastKey, res)
                        obj.clear()
                    }
                    val parts = line.split(":")
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        args[key] = value
                        lastKey = key
                        lastIndentation = indentation
                    } else if (line.isNotBlank()) {
                        throw IllegalArgumentException("Missing properties for ${type.simpleName}")
                    }
                }
            }

            if (obj.isNotEmpty()) {
                val res = parseBlock(obj, lastKey)
                args[lastKey!!] = res.getOrDefault(lastKey, res)
            }

            return newInstance(args)
        }
    }

    final override fun parseList(yaml: Reader): List<T> {
        BufferedReader(yaml).use { reader ->
            val result = mutableListOf<T>()
            val simpleType = type.isSimpleType()

            if (simpleType) {
                val data = reader.readText()
                val lines = data.split("-").map { it.trim() }.filter { it.isNotBlank() }
                result.addAll(lines.map { parseSimpleValue(it, type) as T })
            } else {
                val obj = mutableListOf<String>()
                var lastIndentation: Int? = null

                reader.forEachLine { line ->
                    if (line.isBlank()) {
                        return@forEachLine
                    }

                    val indentation = line.takeWhile { it == ' ' }.length

                    if (lastIndentation == null) {
                        lastIndentation = indentation
                    } else if (line.trim().startsWith('-')) {
                        if (indentation > lastIndentation!!) {
                            obj.add(line)
                        } else {
                            result.add(parseObject(obj.joinToString("\n").reader()))
                            obj.clear()
                        }
                    } else {
                        obj.add(line)
                    }
                }

                if (obj.isNotEmpty()) {
                    result.add(parseObject(obj.joinToString("\n").reader()))
                }
            }

            return result
        }
    }

    private fun parseBlock(lines : List<String>, parent : String?) : Map<String, Any> {
        val args = mutableMapOf<String, Any>()
        var lastIndentation : Int? = null
        var lastKey : String? = null
        val obj = mutableListOf<String>()

        val list = mutableListOf<Map<String, Any>>()
        var isList = false
        var parentValue : String? = null

        for (line in lines) {
            val indentation = line.takeWhile { it == ' ' }.length

            if (line.trim().startsWith('-')) {
                if (!isList) {
                    lastIndentation = indentation
                    isList = true
                } else {
                    if (indentation == lastIndentation) {
                        list.add(parseBlock(obj, parentValue))
                        obj.clear()
                    } else {
                        if (parentValue == null) {
                            parentValue = obj.last().toString().removeSuffix(":").trim()
                        }
                        obj.add(line)
                    }
                }
                continue
            }

            val parts = line.split(":")

            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()

                if (lastIndentation == null) {
                    lastIndentation = indentation
                }

                if (indentation > lastIndentation) {
                    obj.add(line)
                } else {
                    if (obj.isNotEmpty()) {
                        if (isList) {
                            list.add(parseBlock(obj, parentValue))
                            obj.clear()
                            isList = false
                        } else {
                            args[lastKey!!] = parseBlock(obj, parentValue)
                            obj.clear()
                        }
                    }
                    args[key] = value
                    lastKey = key
                    lastIndentation = indentation
                }

            } else if (line.isNotBlank()) {
                throw IllegalArgumentException("Missing properties for ${type.simpleName}")
            }
        }

        if (obj.isNotEmpty()) {
            if (isList) {
                list.add(parseBlock(obj, parentValue))
            } else {
                args[lastKey!!] = parseBlock(obj, parentValue)
            }
        }

        if (list.isNotEmpty()) {
            args[parent!!] = list
        }

        return args
    }

    private fun KClass<*>.isSimpleType() = this in setOf(String::class, Int::class, Long::class,
        Double::class, Float::class)

    private fun parseSimpleValue(value:String, classType:KClass<*>): Any {
        return when (classType) {
            String::class -> value
            Int::class -> value.toInt()
            Long::class -> value.toLong()
            Double::class -> value.toDouble()
            Float::class -> value.toFloat()
            Short::class -> value.toShort()
            Boolean::class -> value.toBoolean()
            List::class -> emptyList<Any>()
            else -> value
        }
    }

}