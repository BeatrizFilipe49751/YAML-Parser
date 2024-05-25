package pt.isel

import pt.isel.test.TestItem
import pt.isel.test.parseCount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YamlParserReflectSequenceTest {

    @Test
    fun testParseLazySequenceReflect() {
        parseCount = 0
        val yaml = """
            - 
                item: item1
            -
                item: item2
            -
                item: item3
        """
        val parser = YamlParserReflect.yamlParser(TestItem::class)

        val sequence = parser.parseSequence(yaml.reader())
        val iterator = sequence.iterator()

        assertEquals(0, parseCount)

        assertTrue(iterator.hasNext())
        assertEquals("item1", iterator.next().item)
        assertEquals(1, parseCount)

        assertTrue(iterator.hasNext())
        assertEquals("item2", iterator.next().item)
        assertEquals(2, parseCount)

        assertTrue(iterator.hasNext())
        assertEquals("item3", iterator.next().item)
        assertEquals(3, parseCount)

        assertFalse(iterator.hasNext())
    }

}