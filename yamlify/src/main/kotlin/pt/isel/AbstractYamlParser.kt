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
        val parser = objectParser(yaml)
        return newInstance(parser)
    }
    private fun objectParser(yaml: Reader) :Map<String, Any> {
        BufferedReader(yaml).use { reader ->
            val args = mutableMapOf<String, Any>()
            var lastKey: String? = null

            var isList = false
            var lastIndentation: Int? = null
            val objLines = mutableListOf<String>()

            reader.forEachLine { line ->
                if (line.isBlank()) {
                    return@forEachLine
                }

                val indentation = line.takeWhile { it == ' ' }.length

                if (lastIndentation == null) {
                    lastIndentation = indentation
                }

                if (indentation > lastIndentation!!) {
                    if (line.trim().startsWith("-") && objLines.isEmpty()) isList = true
                    objLines.add(line)
                } else {
                    if (objLines.isNotEmpty()) {
                        val r = objLines.joinToString("\n").reader()
                        args[lastKey!!] = if (isList) listParser(r) else objectParser(r)
                        objLines.clear()
                    }
                    isList = false
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

            if (objLines.isNotEmpty()) {
                val r = objLines.joinToString("\n").reader()
                args[lastKey!!] = if (isList) listParser(r) else objectParser(r)
            }

            return args
        }
    }
    private fun listParser(yaml: Reader) :List<Any> {
        BufferedReader(yaml).use { reader ->
            val result = mutableListOf<Any>()
            val simpleType = type.isSimpleType()

            if (simpleType) {
                val data = reader.readText()
                val lines = data.split("-").map { it.trim() }.filter { it.isNotBlank() }
                result.addAll(lines)
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
                            result.add(objectParser(obj.joinToString("\n").reader()))
                            obj.clear()
                        }
                    } else {
                        obj.add(line)
                    }
                }

                if (obj.isNotEmpty()) {
                    result.add(objectParser(obj.joinToString("\n").reader()))
                }
            }
            return result
        }
    }

    final override fun parseList(yaml: Reader): List<T> {
        val simpleType = type.isSimpleType()
        val parser = listParser(yaml)
        return if (simpleType) {
            parser.map { parseSimpleValue(it as String, type) as T}
        } else parser.map { newInstance(it as Map<String, Any>) }
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
    final override fun parseSequence(yaml: Reader) :Sequence<T> {
        return sequence {
            BufferedReader(yaml).use { reader ->
                val simpleType = type.isSimpleType()
                val objLines = mutableListOf<String>()
                var lastIndentation: Int? = null

                for (line in reader.lines()) {
                    if (line.isBlank()) continue

                    val indentation = line.takeWhile { it == ' ' }.length

                    if (simpleType) {
                        val value = line.split('-').map { it.trim() }.filter { it.isNotBlank() }
                        yield(parseSimpleValue(value.first(), type) as T)
                    } else if (lastIndentation == null) {
                        lastIndentation = indentation
                    } else if (line.trim().startsWith('-')) {
                        if (indentation > lastIndentation) {
                            objLines.add(line)
                        } else {
                            yield(parseObject(objLines.joinToString("\n").reader()))
                            objLines.clear()
                        }
                    } else {
                        objLines.add(line)
                    }
                }

                if (objLines.isNotEmpty()){
                    yield(parseObject(objLines.joinToString("\n").reader()))
                }
            }
        }
    }
}