package pt.isel

import pt.isel.test.Student
import pt.isel.test.TestItem
import pt.isel.test.parseCount
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YamlParserFolderTest {

    private fun createFolder(path :String) {
        val directory = File(path)
        if (!directory.exists()) {
            val isDirectoryCreated = directory.mkdirs()
            if (isDirectoryCreated) {
                println("Directory created at: $directory")
            } else {
                println("Failed to create directory at: $directory")
                return
            }
        } else {
            println("Directory already exists at: $directory")
        }
    }

    private fun fillFolderWithYamlStudents(path: String) {
        val std1 = """
                name: Maria Candida
                nr: 873435
                address:
                  street: Rua Rosa
                  nr: 78
                  city: Lisbon
                from: Oleiros"""
        val std2 = """
                name: Ana Gomes
                nr: 765876
                address:
                  street: Rua Horta
                  nr: 34
                  city: Lisbon
                from: Alvalade"""
        val std3 = """
                name: Matilde Pereira
                nr: 987876
                address:
                  street: Rua da Liberdade
                  nr: 89
                  city: Faro
                from: Sé"""

        val std1File = File(path, "std1")
        val std2File = File(path, "std2")
        val std3File = File(path, "std3")
        try {
            std1File.writeText(std1)
            std2File.writeText(std2)
            std3File.writeText(std3)
        }
        catch (e :Exception) {
            throw e
        }
    }

    private fun deleteFolder(path: String) {
        val directory = File(path)
        if (!directory.exists()) {
            println("Directory does not exist: ${directory.absolutePath}")
            return
        }
        directory.deleteRecursively()
    }

    @Test
    fun parseFolderEager() {
        val dirName = "StudentsDirTest"
        createFolder(dirName)
        fillFolderWithYamlStudents(dirName)
        val seq = YamlParserCojen.yamlParser(Student::class, 4)
            .parseFolderEager(dirName)
            .iterator()

        val st1 = seq.next()
        assertEquals("Maria Candida", st1.name)
        assertEquals(873435, st1.nr)
        assertEquals("Oleiros", st1.from)
        assertEquals("Rua Rosa", st1.address?.street)
        assertEquals(78, st1.address?.nr)
        assertEquals("Lisbon", st1.address?.city)

        val st2 = seq.next()
        assertEquals("Ana Gomes", st2.name)
        assertEquals(765876, st2.nr)
        assertEquals("Alvalade", st2.from)
        assertEquals("Rua Horta", st2.address?.street)
        assertEquals(34, st2.address?.nr)
        assertEquals("Lisbon", st2.address?.city)

        val st3 = seq.next()
        assertEquals("Matilde Pereira", st3.name)
        assertEquals(987876, st3.nr)
        assertEquals("Sé", st3.from)
        assertEquals("Rua da Liberdade", st3.address?.street)
        assertEquals(89, st3.address?.nr)
        assertEquals("Faro", st3.address?.city)

        assertFalse { seq.hasNext() }
        deleteFolder(dirName)
    }

    private fun fillFolderWithYamlItems(path: String) {
        val item1 = """
                item: item1"""
        val item2 = """
                item: item2"""
        val item3 = """
                item: item3"""

        val std1File = File(path, "item1")
        val std2File = File(path, "item2")
        val std3File = File(path, "item3")
        try {
            std1File.writeText(item1)
            std2File.writeText(item2)
            std3File.writeText(item3)
        }
        catch (e :Exception) {
            throw e
        }
    }

    @Test
    fun parseFolderLazy() {
        val dirName = "ItemsDirTest"
        parseCount = 0
        createFolder(dirName)
        fillFolderWithYamlItems(dirName)
        val seq = YamlParserReflect.yamlParser(TestItem::class)
            .parseFolderLazy(dirName)
            .iterator()
        
        assertEquals(0, parseCount)

        assertTrue(seq.hasNext())
        assertEquals("item1", seq.next().item)
        assertEquals(1, parseCount)

        assertTrue(seq.hasNext())
        assertEquals("item2", seq.next().item)
        assertEquals(2, parseCount)

        assertTrue(seq.hasNext())
        assertEquals("item3", seq.next().item)
        assertEquals(3, parseCount)

        assertFalse(seq.hasNext())


        deleteFolder(dirName)
    }
}