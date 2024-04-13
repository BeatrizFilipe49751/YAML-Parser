package pt.isel

import pt.isel.test.Address
import pt.isel.test.Name
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlParserReflectYamlConvertTest {

    class YamlToDate : YamlPropertyParser<LocalDate> {
        override fun parse(value: String): LocalDate {
            val parts = value.split("-")
            return LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }

    data class Student (
        @YamlArg("full name") val name: Name,
        @YamlConvert(YamlToDate::class) val birth: LocalDate,
        val nr: Int,
        val address: Address? = null,
    )

    @Test fun parseStudentWithAnnotation() {
        val yaml = """
            full name:
              first name: Maria
              last name: Candida
            birth: 2005-03-15
            nr: 123
            address:
              street: Rua Rosa
              nr: 78
              city: Lisbon"""

        val parser = YamlParserReflect.yamlParser(Student::class)
        val student = parser.parseObject(yaml.reader())

        assertEquals("Maria", student.name.first)
        assertEquals("Candida", student.name.last)
        assertEquals(LocalDate.of(2005, 3, 15), student.birth)
        assertEquals(123, student.nr)
        assertEquals("Rua Rosa", student.address?.street)
        assertEquals(78, student.address?.nr)
        assertEquals("Lisbon", student.address?.city)
    }

    class YamlToName : YamlPropertyParser<Name> {
        override fun parse(value: String): Name {
            val parts = value.split(", ")
            return Name(parts[1], parts[0])
        }
    }

    data class Person (
        @YamlConvert(YamlToName::class) @YamlArg("full name") val name: Name,
        @YamlConvert(YamlToDate::class) val birth: LocalDate,
        val age : Int,
        val email : String?
    )

    @Test fun parsePersonWithAnnotation() {
        val yaml = """
            full name: Candida, Maria
            birth: 2005-03-15
            age: 15
            email: maria@example.com"""
        val parser = YamlParserReflect.yamlParser(Person::class)
        val person = parser.parseObject(yaml.reader())

        assertEquals("Maria", person.name.first)
        assertEquals("Candida", person.name.last)
        assertEquals(LocalDate.of(2005, 3, 15), person.birth)
        assertEquals(15, person.age)
        assertEquals("maria@example.com", person.email)
    }

}