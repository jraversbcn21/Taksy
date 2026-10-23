package com.example.taksy.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedTextTest {

    @Test
    fun `nothing shared is null`() {
        assertNull(SharedText.parse(null, null))
        assertNull(SharedText.parse("  ", "\n "))
    }

    @Test
    fun `plain short text is the title`() {
        assertEquals(SharedTask("Comprar pan", null), SharedText.parse(null, "  Comprar pan "))
    }

    @Test
    fun `a subject becomes the title and the text the description`() {
        assertEquals(
            SharedTask("Receta de lentejas", "https://example.com/lentejas"),
            SharedText.parse("Receta de lentejas", "https://example.com/lentejas")
        )
    }

    @Test
    fun `without subject the first line is the title and the rest the description`() {
        assertEquals(
            SharedTask("Llamar al dentista", "Pedir cita para el jueves\nPreguntar precio"),
            SharedText.parse(null, "Llamar al dentista\nPedir cita para el jueves\nPreguntar precio")
        )
    }

    @Test
    fun `only a subject gives a title with no description`() {
        assertEquals(SharedTask("Solo asunto", null), SharedText.parse("Solo asunto", null))
    }

    @Test
    fun `a long title is cut at a word and the rest goes to the description`() {
        val words = List(40) { "palabra$it" }
        val long = words.joinToString(" ")
        val result = SharedText.parse(null, long)!!
        assert(result.title.length <= 120)
        assert(!result.title.endsWith(" "))
        assertEquals(long, listOf(result.title, result.description).joinToString(" "))
    }
}
