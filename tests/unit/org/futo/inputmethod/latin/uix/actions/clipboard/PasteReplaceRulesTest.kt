package org.futo.inputmethod.latin.uix.actions.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PasteReplaceRulesTest {
    private fun rule(pattern: String, replacement: String, enabled: Boolean = true) =
        PasteReplaceRule(enabled = enabled, pattern = pattern, replacement = replacement)

    private fun applyDefault(text: String) =
        PasteReplaceRules.applyRules(text, PasteReplaceRules.defaultRules)

    @Test
    fun defaultRuleRewritesXLinks() {
        assertEquals("https://fixvx.com/a/status/1", applyDefault("https://x.com/a/status/1"))
        assertEquals("https://fixvx.com/a/status/1", applyDefault("http://www.x.com/a/status/1"))
        assertEquals(
            "see https://fixvx.com/a and https://fixvx.com/b",
            applyDefault("see https://x.com/a and https://www.x.com/b")
        )
    }

    @Test
    fun defaultRuleLeavesOtherTextAlone() {
        assertEquals("https://fixvx.com/a", applyDefault("https://fixvx.com/a"))
        assertEquals("https://netflix.com/a", applyDefault("https://netflix.com/a"))
        assertEquals("https://xxcom/a", applyDefault("https://xxcom/a"))
    }

    @Test
    fun defaultRuleIsEnabled() {
        assertEquals(1, PasteReplaceRules.defaultRules.size)
        assertEquals(true, PasteReplaceRules.defaultRules[0].enabled)
    }

    @Test
    fun replacementSupportsGroupReferences() {
        assertEquals(
            "b-a",
            PasteReplaceRules.applyRules("a-b", listOf(rule("(\\w)-(\\w)", "$2-$1")))
        )
    }

    @Test
    fun rulesApplyInOrderOnPreviousOutput() {
        val first = rule("a", "b")
        val second = rule("b", "c")

        assertEquals("c", PasteReplaceRules.applyRules("a", listOf(first, second)))
        assertEquals("b", PasteReplaceRules.applyRules("a", listOf(second, first)))
    }

    @Test
    fun disabledRulesAreSkipped() {
        val rules = listOf(rule("a", "b", enabled = false), rule("c", "d"))
        assertEquals("ad", PasteReplaceRules.applyRules("ac", rules))
    }

    @Test
    fun invalidPatternIsSkippedAndOtherRulesStillApply() {
        val rules = listOf(rule("(", "x"), rule("a", "b"))
        assertEquals("b", PasteReplaceRules.applyRules("a", rules))
    }

    @Test
    fun invalidReplacementIsSkippedAndOtherRulesStillApply() {
        val rules = listOf(rule("a", "$5"), rule("a", "x$"), rule("a", "b"))
        assertEquals("b", PasteReplaceRules.applyRules("a", rules))
    }

    @Test
    fun emptyPatternIsSkipped() {
        assertEquals("abc", PasteReplaceRules.applyRules("abc", listOf(rule("", "x"))))
    }

    @Test
    fun noRulesReturnsTextUnchanged() {
        assertEquals("abc", PasteReplaceRules.applyRules("abc", emptyList()))
    }

    @Test
    fun massiveTextIsNotProcessed() {
        val text = "a".repeat(500_001)
        assertEquals(text, PasteReplaceRules.applyRules(text, listOf(rule("a", "b"))))
    }

    @Test
    fun patternErrorIdentifiesInvalidPatterns() {
        assertNull(PasteReplaceRules.patternError("""https?://(www\.)?x\.com"""))
        assertNotNull(PasteReplaceRules.patternError("("))
        assertNotNull(PasteReplaceRules.patternError("[a"))
    }

    @Test
    fun replacementErrorIdentifiesInvalidReplacements() {
        assertNull(PasteReplaceRules.replacementError("(a)", "$1"))
        assertNull(PasteReplaceRules.replacementError("(?<name>a)", "\${name}"))
        assertNull(PasteReplaceRules.replacementError("a", "plain"))
        assertNull(PasteReplaceRules.replacementError("a", "\\$1"))

        assertNotNull(PasteReplaceRules.replacementError("(a)", "$2"))
        assertNotNull(PasteReplaceRules.replacementError("a", "x$"))
        assertNotNull(PasteReplaceRules.replacementError("a", "x\\"))
        assertNotNull(PasteReplaceRules.replacementError("a", "\${missing}"))
    }

    @Test
    fun rulesSurviveEncoding() {
        val rules = listOf(rule("a\\.b", "$1\"", enabled = false), rule("c", "d"))
        assertEquals(rules, PasteReplaceRules.decode(PasteReplaceRules.encode(rules)))
    }

    @Test
    fun corruptDataDecodesToNoRules() {
        assertEquals(emptyList<PasteReplaceRule>(), PasteReplaceRules.decode("not json"))
        assertEquals(emptyList<PasteReplaceRule>(), PasteReplaceRules.decode("{}"))
    }
}
