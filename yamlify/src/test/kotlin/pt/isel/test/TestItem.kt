package pt.isel.test

import pt.isel.YamlConvert
import pt.isel.YamlPropertyParser

var parseCount = 0

data class TestItem(
    @YamlConvert(StringParser::class) val item: String
)

class StringParser : YamlPropertyParser<String> {
    override fun parse(value: String): String {
        parseCount++
        return value
    }
}