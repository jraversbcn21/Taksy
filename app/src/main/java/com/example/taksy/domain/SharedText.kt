package com.example.taksy.domain

/** Tarea propuesta a partir de un texto compartido desde otra app. */
data class SharedTask(val title: String, val description: String?)

object SharedText {
    private const val MAX_TITLE = 120

    /**
     * El navegador comparte el título de la página como asunto y la dirección como texto; otras
     * apps solo mandan texto. Con asunto, ese es el título y el texto pasa a la descripción; sin
     * asunto, el título es la primera línea y el resto la descripción. Un título demasiado largo
     * se corta por una palabra y el resto va a la descripción. Sin nada que mostrar devuelve null.
     */
    fun parse(subject: String?, text: String?): SharedTask? {
        val subj = subject?.trim().orEmpty()
        val body = text?.trim().orEmpty()
        if (subj.isEmpty() && body.isEmpty()) return null

        val (title, rest) = if (subj.isNotEmpty()) {
            subj to body
        } else {
            val first = body.lineSequence().first().trim()
            first to body.removePrefix(body.lineSequence().first()).trim()
        }
        if (title.length <= MAX_TITLE) return SharedTask(title, rest.ifEmpty { null })

        val cut = title.take(MAX_TITLE).substringBeforeLast(' ', title.take(MAX_TITLE))
        val overflow = title.removePrefix(cut).trim()
        return SharedTask(cut, listOf(overflow, rest).filter { it.isNotEmpty() }.joinToString("\n").ifEmpty { null })
    }
}
