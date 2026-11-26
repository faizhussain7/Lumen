package com.mfhapps.lumen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingTest {
    private val apps = listOf("Settings", "WhatsApp", "Google Maps", "Waffarha", "Messages", "Calculator")

    @Test
    fun prefixMatchRanksFirst() {
        assertEquals("WhatsApp", rankBy(apps, "wha") { it }.first())
    }

    @Test
    fun wordStartMatchesSecondWord() {
        assertEquals("Google Maps", rankBy(apps, "maps") { it }.first())
    }

    @Test
    fun initialsMatch() {
        assertEquals(listOf("Google Maps"), rankBy(apps, "gm") { it })
    }

    @Test
    fun looseSubsequenceMatches() {
        assertTrue("Calculator" in rankBy(apps, "clctr") { it })
    }

    @Test
    fun noMatchReturnsEmpty() {
        assertTrue(rankBy(apps, "zzz") { it }.isEmpty())
    }

    @Test
    fun blankQueryKeepsOrder() {
        assertEquals(apps, rankBy(apps, "  ") { it })
    }
}

class CalculatorTest {
    @Test
    fun precedence() = assertEquals("100", Calculator.evaluate("12*8+4"))

    @Test
    fun parentheses() = assertEquals("40", Calculator.evaluate("(2+3)*8"))

    @Test
    fun decimals() = assertEquals("2.5", Calculator.evaluate("5/2"))

    @Test
    fun power() = assertEquals("1024", Calculator.evaluate("2^10"))

    @Test
    fun plainNumberIsNotACalculation() = assertNull(Calculator.evaluate("2026"))

    @Test
    fun textIsIgnored() = assertNull(Calculator.evaluate("hello + world"))

    @Test
    fun divisionByZeroIsIgnored() = assertNull(Calculator.evaluate("1/0"))
}

class UnitsTest {
    @Test
    fun kilometresToMiles() = assertEquals("5 km = 3.1069 miles", Units.convert("5 km in miles"))

    @Test
    fun celsiusToFahrenheit() = assertEquals("30 C = 86 F", Units.convert("30 c to f"))

    @Test
    fun kilogramsToPounds() = assertEquals("2 kg = 4.4092 lb", Units.convert("2 kg in lb"))

    @Test
    fun incompatibleUnits() = assertNull(Units.convert("5 km in kg"))

    @Test
    fun unknownUnit() = assertNull(Units.convert("5 parsecs in km"))
}

class SettingsIndexTest {
    @Test
    fun titleMatch() = assertEquals("Wi-Fi", SettingsIndex.search("wi-fi").first().title)

    @Test
    fun synonymMatch() = assertTrue(SettingsIndex.search("tether").any { it.title == "Hotspot and tethering" })

    @Test
    fun brightnessFindsDisplaySettings() = assertEquals("Brightness", SettingsIndex.search("brightness").first().title)

    @Test
    fun multiWordQuery() = assertTrue(SettingsIndex.search("usb debugging").any { it.title == "Developer options" })

    @Test
    fun shortQueryReturnsNothing() = assertTrue(SettingsIndex.search("w").isEmpty())

    @Test
    fun resultsAreCapped() = assertTrue(SettingsIndex.search("settings").size <= 6)

    @Test
    fun everyEntryHasAnAction() = assertTrue(SettingsIndex.entries.all { it.action.isNotBlank() && it.title.isNotBlank() })
}
