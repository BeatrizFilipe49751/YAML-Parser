package pt.isel

import org.junit.jupiter.api.assertThrows
import pt.isel.test.Person
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlParserReflectAnnotationTest {

    @Test fun parsePersonWithMissingProperties() {
        val yaml = """
            city of birth: London
            full name:
              first name: John
              last name: Doe
            age: 30"""
        assertThrows<IllegalArgumentException> {
            YamlParserReflect.yamlParser(Person::class).parseObject(yaml.reader())
        }
    }

    @Test fun parsePersonWithAllProperties() {
        val yaml = """
            city of birth: London
            full name:
              first name: John
              last name: Doe
            age: 30
            email: john@example.com
            is_student: false"""

        val parser = YamlParserReflect.yamlParser(Person::class)
        val person = parser.parseObject(yaml.reader())

        assertEquals("London", person.from)
        assertEquals("John", person.name.first)
        assertEquals("Doe", person.name.last)
        assertEquals(30, person.age)
        assertEquals("john@example.com", person.email)
        assertEquals(false, person.student)
    }

    @Test fun parsePersonWithAddresses() {
        val yaml = """
            city of birth: Lisbon
            full name:
              first name: Maria
              last name: Candida
            age: 15
            is_student: true
            addresses:
              - 
                street: Rua Rosa
                nr: 78
                city: Lisbon
              - 
                street: Rua Azul
                nr: 45
                city: Porto
              - 
                street: Rua Verde
                nr: 23
                city: Coimbra
            """
        val parser = YamlParserReflect.yamlParser(Person::class)
        val person = parser.parseObject(yaml.reader())

        assertEquals("Lisbon", person.from)
        assertEquals("Maria", person.name.first)
        assertEquals("Candida", person.name.last)
        assertEquals(15, person.age)
        assertEquals(true, person.student)
        assertEquals(3, person.addresses.size)
        assertEquals("Rua Rosa", person.addresses[0].street)
        assertEquals(78, person.addresses[0].nr)
        assertEquals("Lisbon", person.addresses[0].city)
        assertEquals("Rua Azul", person.addresses[1].street)
        assertEquals(45, person.addresses[1].nr)
        assertEquals("Porto", person.addresses[1].city)
        assertEquals("Rua Verde", person.addresses[2].street)
        assertEquals(23, person.addresses[2].nr)
        assertEquals("Coimbra", person.addresses[2].city)
    }


    @Test fun parseListOfPerson() {
        val yaml = """
            - 
              city of birth: Lisbon
              full name:
                first name: Maria
                last name: Candida
              age: 15
              email: maria@gmail.com
              is_student: true
              addresses:
                - 
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                - 
                  street: Rua Azul
                  nr: 45
                  city: Porto
                - 
                  street: Rua Verde
                  nr: 23
                  city: Coimbra
            - 
              city of birth: Porto
              full name:
                first name: Jose
                last name: Carioca
              age: 20
              is_student: false
              addresses:
                - 
                  street: Rua Branca
                  nr: 100
                  city: Porto
                - 
                  street: Rua Amarela
                  nr: 5
                  city: Setúbal
            """
        val parser = YamlParserReflect.yamlParser(Person::class)
        val persons = parser.parseList(yaml.reader())

        assertEquals(2, persons.size)
        val person1 = persons[0]
        assertEquals("Lisbon", person1.from)
        assertEquals("Maria", person1.name.first)
        assertEquals("Candida", person1.name.last)
        assertEquals(15, person1.age)
        assertEquals("maria@gmail.com", person1.email)
        assertEquals(true, person1.student)
        assertEquals(3, person1.addresses.size)
        assertEquals("Rua Rosa", person1.addresses[0].street)
        assertEquals(78, person1.addresses[0].nr)
        assertEquals("Lisbon", person1.addresses[0].city)
        assertEquals("Rua Azul", person1.addresses[1].street)
        assertEquals(45, person1.addresses[1].nr)
        assertEquals("Porto", person1.addresses[1].city)
        assertEquals("Rua Verde", person1.addresses[2].street)
        assertEquals(23, person1.addresses[2].nr)
        assertEquals("Coimbra", person1.addresses[2].city)

        val person2 = persons[1]
        assertEquals("Porto", person2.from)
        assertEquals("Jose", person2.name.first)
        assertEquals("Carioca", person2.name.last)
        assertEquals(20, person2.age)
        assertEquals(false, person2.student)
        assertEquals(2, person2.addresses.size)
        assertEquals("Rua Branca", person2.addresses[0].street)
        assertEquals(100, person2.addresses[0].nr)
        assertEquals("Porto", person2.addresses[0].city)
        assertEquals("Rua Amarela", person2.addresses[1].street)
        assertEquals(5, person2.addresses[1].nr)
        assertEquals("Setúbal", person2.addresses[1].city)
    }

}