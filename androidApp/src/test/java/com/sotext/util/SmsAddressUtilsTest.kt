package com.sotext.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsAddressUtilsTest {

    @Test
    fun splitSmsDisplayAddress_emptyString_returnsEmptyPair() {
        val (name, number) = splitSmsDisplayAddress("")
        assertEquals("", name)
        assertNull(number)
    }

    @Test
    fun splitSmsDisplayAddress_blankString_returnsEmptyPair() {
        val (name, number) = splitSmsDisplayAddress("   ")
        assertEquals("", name)
        assertNull(number)
    }

    @Test
    fun splitSmsDisplayAddress_singleValue_returnsValueAndNull() {
        val (name, number) = splitSmsDisplayAddress("1234567890")
        assertEquals("1234567890", name)
        assertNull(number)
    }

    @Test
    fun splitSmsDisplayAddress_standardDelimiter_returnsNameAndNumber() {
        val (name, number) = splitSmsDisplayAddress("John Doe | 1234567890")
        assertEquals("John Doe", name)
        assertEquals("1234567890", number)
    }

    @Test
    fun splitSmsDisplayAddress_bulletDelimiter_returnsNameAndNumber() {
        val (name, number) = splitSmsDisplayAddress("Jane Doe \u2022 9876543210")
        assertEquals("Jane Doe", name)
        assertEquals("9876543210", number)
    }

    @Test
    fun splitSmsDisplayAddress_customDelimiter_returnsNameAndNumber() {
        val (name, number) = splitSmsDisplayAddress("Alice L 5551234567")
        assertEquals("Alice", name)
        assertEquals("5551234567", number)
    }

    @Test
    fun splitSmsDisplayAddress_multipleDelimiters_usesFirstValid() {
        // Assuming implementation prioritizes first match or specific order
        val (name, number) = splitSmsDisplayAddress("Bob | 123 | 456")
        assertEquals("Bob", name)
        assertEquals("123 | 456", number)
    }

    @Test
    fun splitSmsDisplayAddress_noDigitsOnRight_returnsOriginal() {
        val (name, number) = splitSmsDisplayAddress("Not a number | Just text")
        // If right side has no digits, it might not split depending on implementation logic.
        // The implementation says: if (right.isNotBlank() && right.count { it.isDigit() } >= 2)
        assertEquals("Not a number | Just text", name)
        assertNull(number)
    }
}
