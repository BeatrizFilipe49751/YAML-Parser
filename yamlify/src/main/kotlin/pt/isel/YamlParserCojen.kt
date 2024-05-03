package pt.isel

import org.cojen.maker.ClassMaker
import org.cojen.maker.MethodMaker
import org.cojen.maker.Variable
import java.lang.reflect.Constructor
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
/**
 * A YamlParser that uses Cojen Maker to generate a parser.
 */
open class YamlParserCojen<T : Any>(
    private val type: KClass<T>,
    private val nrOfInitArgs: Int)
: AbstractYamlParser<T>(type) {

    companion object {
        private val yamlParsers: MutableMap<String, YamlParserCojen<*>> = mutableMapOf()

        private fun parserName(type: KClass<*>, nrOfInitArgs: Int): String {
            return "YamlParser${type.simpleName}$nrOfInitArgs"
        }

        /**
         * Creates a YamlParser for the given type using Cojen Maker if it does not already exist.
         * Keep it in an internal cache.
         */
        fun <T : Any> yamlParser(type: KClass<T>, nrOfInitArgs: Int = type.constructors.first().parameters.size): AbstractYamlParser<T> {
            return yamlParsers.getOrPut(parserName(type, nrOfInitArgs)) {
                val a = YamlParserCojen(type, nrOfInitArgs)
                    .buildYamlParser()
                    .finish()
                a.getConstructor(KClass::class.java, Integer::class.java)
                    .newInstance(type, nrOfInitArgs) as YamlParserCojen<*>

            } as YamlParserCojen<T>
        }
    }
    /**
     * Used to get a parser for other Type using the same parsing approach.
     */
    override fun <T : Any> yamlParser(type: KClass<T>) =
        YamlParserCojen.yamlParser(type)

    /**
     * Do not change this method in YamlParserCojen.
     */
    override fun newInstance(args: Map<String, Any>): T {
        throw UnsupportedOperationException("This method is overridden in a subclass dynamically generated by buildYamlParser() function!")
    }

    private fun buildYamlParser() : ClassMaker {
        val cm = ClassMaker.begin(parserName(type, nrOfInitArgs)).extend(YamlParserCojen::class.java).public_()
        println("ClassMaker created for class: ${parserName(type, nrOfInitArgs)}")

        // Generate Constructor
        val init: MethodMaker = cm.addConstructor(KClass::class.java, Integer::class.java).public_()
        init.invokeSuperConstructor(init.param(0), init.param(1))

        val newInstance = cm.addMethod(Any::class.java, "newInstance", Map::class.java)

        // Generate newInstance
        val constructor: Constructor<*> = type.java.constructors.first { it.parameters.size == nrOfInitArgs }
        val params = constructor.parameters.map {
            println("Processing parameter: ${it.name} of type: ${it.type}")
            getValueForParameter(it, newInstance)
        }.toTypedArray()

        newInstance.return_(newInstance.new_(type.java, *params))

        return cm
    }

    private fun getValueForParameter(param: Parameter, newInstance: MethodMaker): Variable {
        val value = newInstance.param(0).invoke("get", param.name)
        return when (param.type) {
            String::class.java -> {
                value.cast(String::class.java)
            }
            Int::class.java -> {
                newInstance.`var`(Integer::class.java)
                    .invoke("parseInt", value.cast(String::class.java))
            }
            Long::class.java -> newInstance.`var`(Long::class.java)
                .invoke("parseLong", value.cast(String::class.java))
            Double::class.java -> newInstance.`var`(Double::class.java)
                .invoke("parseDouble", value.cast(String::class.java))
            Float::class.java -> newInstance.`var`(Float::class.java)
                .invoke("parseFloat", value.cast(String::class.java))
            Short::class.java -> newInstance.`var`(Short::class.java)
                .invoke("parseShort", value.cast(String::class.java))
            Boolean::class.java -> newInstance.`var`(Boolean::class.java)
                .invoke("parseBoolean", value.cast(String::class.java))
            List::class.java -> {
                val nestedClass = (param.parameterizedType as ParameterizedType).actualTypeArguments[0] as Class<*>
                // TODO
                value
                /*val list = value.cast(List::class.java) as List<Map<String, Any>>
                val listVar = newInstance.new_(ArrayList::class.java)
                list.forEach {
                    listVar.invoke("add", yamlParser(nestedClass.kotlin).newInstance(it))
                }
                listVar*/
            }
            else -> {
                // TODO
                value
            }
        }
    }

}