package io.github.brokoskokoli.birthdaysync

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Prüft die Übersetzungen direkt in den XML-Dateien: vollständige Schlüssel, passende Format-Argumente
 * und Platzhalter, "other" in jedem Plural, korrekt maskierte Apostrophe/Anführungszeichen.
 * Außerdem die Längengrenzen der Store-Texte unter fastlane/metadata/android.
 */
class TranslationsTest {

    private val resDir = File(System.getProperty("resDir") ?: "src/main/res")
    private val fastlaneDir = File(System.getProperty("fastlaneDir") ?: "../fastlane/metadata/android")

    /** Ein Eintrag: einfacher Text (Schlüssel "") oder Plural (Schlüssel = quantity). */
    private class Entry(val items: Map<String, String>, val translatable: Boolean)

    private fun load(file: File): Map<String, Entry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val result = linkedMapOf<String, Entry>()
        val children = doc.documentElement.childNodes
        for (i in 0 until children.length) {
            val e = children.item(i) as? Element ?: continue
            val translatable = e.getAttribute("translatable") != "false"
            when (e.tagName) {
                "string" -> result[e.getAttribute("name")] = Entry(mapOf("" to e.textContent), translatable)
                "plurals" -> {
                    val items = linkedMapOf<String, String>()
                    val nodes = e.getElementsByTagName("item")
                    for (j in 0 until nodes.length) {
                        val item = nodes.item(j) as Element
                        items[item.getAttribute("quantity")] = item.textContent
                    }
                    result[e.getAttribute("name")] = Entry(items, translatable)
                }
            }
        }
        return result
    }

    private val base by lazy { load(File(resDir, "values/strings.xml")) }
    private val locales by lazy {
        resDir.listFiles { f -> f.isDirectory && f.name.startsWith("values-") && File(f, "strings.xml").exists() }!!
            .sortedBy { it.name }
    }

    private fun formatArgs(s: String): List<String> =
        FORMAT.findAll(s).map { it.value }.filter { it != "%%" }.sorted().toList()

    private fun tokens(s: String): List<String> = TOKEN.findAll(s).map { it.value }.sorted().toList()

    @Test fun expectedLanguagesExist() {
        val present = locales.map { it.name.removePrefix("values-") }.toSet()
        val missing = EXPECTED_LOCALES - present
        assertTrue("Fehlende Sprachen: $missing", missing.isEmpty())
    }

    @Test fun everyLocaleIsCompleteAndConsistent() {
        val errors = mutableListOf<String>()
        for (dir in locales) {
            val tr = load(File(dir, "strings.xml"))
            val loc = dir.name
            for ((name, entry) in base) {
                if (!entry.translatable) {
                    if (name in tr) errors += "$loc: $name ist nicht übersetzbar"
                    continue
                }
                val t = tr[name] ?: run { errors += "$loc: $name fehlt"; continue }
                val reference = entry.items["other"] ?: entry.items.getValue("")
                if (entry.items.containsKey("other") && !t.items.containsKey("other")) errors += "$loc: $name ohne quantity=\"other\""
                if (!entry.items.containsKey("other") && !t.items.containsKey("")) errors += "$loc: $name muss ein <string> sein"
                for ((q, text) in t.items) {
                    if (formatArgs(text) != formatArgs(reference)) errors += "$loc: $name[$q] Format-Argumente ${formatArgs(text)} statt ${formatArgs(reference)}"
                    if (tokens(text) != tokens(reference)) errors += "$loc: $name[$q] Platzhalter ${tokens(text)} statt ${tokens(reference)}"
                    if (text.isBlank()) errors += "$loc: $name[$q] ist leer"
                }
            }
            (tr.keys - base.keys).forEach { errors += "$loc: $it gibt es in values/strings.xml nicht" }
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test fun pluralsHaveOther() {
        val errors = (listOf(File(resDir, "values")) + locales).flatMap { dir ->
            load(File(dir, "strings.xml")).filter { (_, e) -> !e.items.containsKey("") && !e.items.containsKey("other") }
                .map { (name, _) -> "${dir.name}: $name" }
        }
        assertTrue("Plurals ohne other: $errors", errors.isEmpty())
    }

    /** Unmaskierte ' oder " würden von aapt verschluckt; @ bzw. ? am Anfang gälten als Referenz. */
    @Test fun quotesAreEscaped() {
        val errors = mutableListOf<String>()
        for (dir in listOf(File(resDir, "values")) + locales) {
            val raw = File(dir, "strings.xml").readText()
            Regex("""<(string|item)\b[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL).findAll(raw).forEach { m ->
                val text = m.groupValues[2]
                if (Regex("""(?<!\\)['"]""").containsMatchIn(text)) errors += "${dir.name}: ${text.take(60)}"
                if (text.startsWith("@") || text.startsWith("?")) errors += "${dir.name}: beginnt mit @/?: ${text.take(60)}"
            }
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test fun storeListingsFitPlayLimits() {
        val dirs = fastlaneDir.listFiles { f -> f.isDirectory }.orEmpty()
        assertTrue("Keine Store-Texte unter $fastlaneDir", dirs.any { it.name == "en-US" })
        val errors = mutableListOf<String>()
        for (dir in dirs) {
            for ((file, max) in listOf("title.txt" to 30, "short_description.txt" to 80, "full_description.txt" to 4000)) {
                val f = File(dir, file)
                if (!f.exists()) { errors += "${dir.name}/$file fehlt"; continue }
                val length = f.readText().trimEnd().codePointCount()
                if (length == 0 || length > max) errors += "${dir.name}/$file: $length Zeichen (max. $max)"
            }
        }
        val missing = EXPECTED_STORE_LOCALES - dirs.map { it.name }.toSet()
        if (missing.isNotEmpty()) errors += "Fehlende Store-Sprachen: $missing"
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    private fun String.codePointCount() = codePointCount(0, length)

    companion object {
        private val FORMAT = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z%]""")
        private val TOKEN = Regex("""\{(name|age|alter)\}""")

        val EXPECTED_LOCALES = setOf(
            "de", "fr", "es", "it", "pt", "pt-rBR", "nl", "pl", "cs", "sk", "hu", "ro", "bg", "hr", "sl",
            "sr", "b+sr+Latn", "el", "sv", "da", "nb", "fi", "et", "lv", "lt", "uk", "ru", "tr", "ca",
            "zh-rCN", "zh-rTW", "ja", "ko", "hi", "in", "vi", "th", "ar", "iw",
        )

        val EXPECTED_STORE_LOCALES = setOf(
            "en-US", "de-DE", "fr-FR", "es-ES", "it-IT", "pt-PT", "pt-BR", "nl-NL", "pl-PL", "cs-CZ", "sk", "hu-HU",
            "ro", "bg", "hr", "sl", "sr", "el-GR", "sv-SE", "da-DK", "no-NO", "fi-FI", "et", "lv", "lt", "uk",
            "ru-RU", "tr-TR", "ca", "zh-CN", "zh-TW", "ja-JP", "ko-KR", "hi-IN", "id", "vi", "th", "ar", "iw-IL",
        )
    }
}
