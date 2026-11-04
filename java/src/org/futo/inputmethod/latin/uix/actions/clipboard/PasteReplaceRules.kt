package org.futo.inputmethod.latin.uix.actions.clipboard

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.futo.inputmethod.latin.uix.SettingsKey
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Serializable
data class PasteReplaceRule(
    val enabled: Boolean = true,
    val pattern: String,
    val replacement: String
)

@Suppress("HardCodedStringLiteral")
object PasteReplaceRules {
    // Same limit as clipboard history, these patterns are run on the main thread
    private const val MAX_TEXT_LENGTH = 500_000

    private val json = Json { ignoreUnknownKeys = true }

    val defaultRules = listOf(
        PasteReplaceRule(
            pattern = """https?://(www\.)?x\.com""",
            replacement = "https://fixvx.com"
        )
    )

    fun encode(rules: List<PasteReplaceRule>): String = json.encodeToString(rules)

    fun decode(data: String): List<PasteReplaceRule> = try {
        json.decodeFromString<List<PasteReplaceRule>>(data)
    } catch(_: IllegalArgumentException) {
        emptyList()
    }

    /** Returns a description of why the pattern is invalid, or null if it is valid */
    fun patternError(pattern: String): String? {
        return try {
            Pattern.compile(pattern)
            null
        } catch(e: PatternSyntaxException) {
            e.description ?: e.message ?: ""
        }
    }

    /**
     * Returns a description of why the replacement can't be used with the pattern, or null if it
     * can. The pattern should be valid.
     */
    fun replacementError(pattern: String, replacement: String): String? {
        // The replacement is only evaluated when there's a match. The empty alternative guarantees
        // one while keeping the same capture groups as the original pattern.
        val matcher = try {
            Pattern.compile("(?:$pattern)|").matcher("")
        } catch(_: PatternSyntaxException) {
            return null
        }

        return try {
            matcher.replaceAll(replacement)
            null
        } catch(e: IllegalArgumentException) {
            e.message ?: ""
        } catch(e: IndexOutOfBoundsException) {
            e.message ?: ""
        }
    }

    /**
     * Applies the enabled rules in order, each rule operating on the output of the previous one.
     * Rules that fail, such as for having an invalid pattern, are skipped.
     */
    fun applyRules(text: String, rules: List<PasteReplaceRule>): String {
        if(text.length > MAX_TEXT_LENGTH) return text

        return rules.filter { it.enabled && it.pattern.isNotEmpty() }.fold(text) { current, rule ->
            try {
                Pattern.compile(rule.pattern).matcher(current).replaceAll(rule.replacement)
            } catch(_: IllegalArgumentException) {
                current
            } catch(_: IndexOutOfBoundsException) {
                current
            }
        }
    }
}

val PasteReplaceRulesSetting = SettingsKey(
    stringPreferencesKey("paste_replace_rules"),
    PasteReplaceRules.encode(PasteReplaceRules.defaultRules)
)
