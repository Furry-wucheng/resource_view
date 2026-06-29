package dev.wucheng.resource_viewer.shared.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderComparatorTest {

    private val comparator = NaturalOrderComparator

    @Test
    fun `should sort numeric filenames naturally`() {
        val files = listOf("file10.jpg", "file2.jpg", "file1.jpg", "file20.jpg", "file3.jpg")
        val sorted = files.sortedWith(comparator)
        assertEquals(listOf("file1.jpg", "file2.jpg", "file3.jpg", "file10.jpg", "file20.jpg"), sorted)
    }

    @Test
    fun `should handle pure numbers`() {
        val files = listOf("100", "2", "10", "1", "20")
        val sorted = files.sortedWith(comparator)
        assertEquals(listOf("1", "2", "10", "20", "100"), sorted)
    }

    @Test
    fun `should handle mixed alphanumeric`() {
        val files = listOf("chapter10-5", "chapter2-1", "chapter2-10", "chapter1-20", "chapter1-3")
        val sorted = files.sortedWith(comparator)
        assertEquals(
            listOf("chapter1-3", "chapter1-20", "chapter2-1", "chapter2-10", "chapter10-5"),
            sorted
        )
    }

    @Test
    fun `should handle equal strings`() {
        val files = listOf("abc", "abc", "abc")
        val sorted = files.sortedWith(comparator)
        assertEquals(listOf("abc", "abc", "abc"), sorted)
    }

    @Test
    fun `should handle empty strings`() {
        val files = listOf("", "a", "1")
        val sorted = files.sortedWith(comparator)
        assertEquals(listOf("", "1", "a"), sorted)
    }

    @Test
    fun `should be case insensitive`() {
        val files = listOf("Abc", "abc", "ABC")
        val sorted = files.sortedWith(comparator)
        assertEquals(listOf("Abc", "abc", "ABC"), sorted)
    }

    @Test
    fun `should handle leading zeros in numbers`() {
        val files = listOf("file001", "file01", "file1", "file10")
        val sorted = files.sortedWith(comparator)
        // 001, 01, 1 all parse to 1L, they differ only in the non-numeric prefix "file"
        // but since the numeric parts are equal (1), they are sorted by the identical prefix
        assertTrue(sorted.indexOf("file1") < sorted.indexOf("file10"))
        assertTrue(sorted.indexOf("file01") < sorted.indexOf("file10"))
    }

    @Test
    fun `should put shorter string first when one is prefix of another`() {
        val files = listOf("file", "file10", "file2")
        val sorted = files.sortedWith(comparator)
        assertEquals(listOf("file", "file2", "file10"), sorted)
    }
}
