package org.futo.inputmethod.latin.uix.settings.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.actions.clipboard.PasteReplaceRule
import org.futo.inputmethod.latin.uix.actions.clipboard.PasteReplaceRules
import org.futo.inputmethod.latin.uix.actions.clipboard.PasteReplaceRulesSetting
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.ScrollableList
import org.futo.inputmethod.latin.uix.settings.SettingItem
import org.futo.inputmethod.latin.uix.settings.Tip
import org.futo.inputmethod.latin.uix.settings.useDataStore

private const val NEW_RULE_INDEX = -1

@Composable
private fun PasteReplaceRuleItem(
    rule: PasteReplaceRule,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit
) {
    SettingItem(
        title = rule.pattern,
        onClick = onEdit,
        subcontent = {
            Column {
                Text(
                    stringResource(R.string.paste_replace_rules_replaced_with, rule.replacement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.paste_replace_rules_move_up)
                        )
                    }
                    IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.paste_replace_rules_move_down)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.paste_replace_rules_delete)
                        )
                    }
                }
            }
        }
    ) {
        Switch(checked = rule.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun PasteReplaceRuleDialog(
    rule: PasteReplaceRule?,
    onDismiss: () -> Unit,
    onSave: (pattern: String, replacement: String) -> Unit
) {
    var pattern by remember { mutableStateOf(rule?.pattern ?: "") }
    var replacement by remember { mutableStateOf(rule?.replacement ?: "") }

    val patternError = remember(pattern) { PasteReplaceRules.patternError(pattern) }
    val replacementError = remember(pattern, replacement) {
        if(patternError == null) PasteReplaceRules.replacementError(pattern, replacement) else null
    }

    val keyboardOptions = KeyboardOptions(autoCorrectEnabled = false)

    AlertDialog(
        title = {
            Text(stringResource(if(rule == null) R.string.paste_replace_rules_add else R.string.paste_replace_rules_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.paste_replace_rules_pattern)) },
                    isError = patternError != null,
                    supportingText = patternError?.let {
                        { Text(stringResource(R.string.paste_replace_rules_invalid_pattern, it)) }
                    },
                    keyboardOptions = keyboardOptions
                )
                TextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.paste_replace_rules_replacement)) },
                    isError = replacementError != null,
                    supportingText = replacementError?.let {
                        { Text(stringResource(R.string.paste_replace_rules_invalid_replacement, it)) }
                    },
                    keyboardOptions = keyboardOptions
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(pattern, replacement) },
                enabled = pattern.isNotEmpty() && patternError == null && replacementError == null
            ) {
                Text(stringResource(R.string.paste_replace_rules_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.paste_replace_rules_cancel))
            }
        }
    )
}

@Preview
@Composable
fun PasteReplaceRulesScreen(navController: NavHostController = rememberNavController()) {
    val (rulesJson, setRulesJson) = useDataStore(PasteReplaceRulesSetting, blocking = true)
    val rules = remember(rulesJson) { PasteReplaceRules.decode(rulesJson) }
    val setRules = { newRules: List<PasteReplaceRule> -> setRulesJson(PasteReplaceRules.encode(newRules)) }

    var editingIndex by remember { mutableStateOf<Int?>(null) }

    ScrollableList {
        ScreenTitle(stringResource(R.string.paste_replace_rules_title), showBack = true, navController)

        Tip(stringResource(R.string.paste_replace_rules_tip))

        if(rules.isEmpty()) {
            Tip(stringResource(R.string.paste_replace_rules_none))
        }

        rules.forEachIndexed { i, rule ->
            PasteReplaceRuleItem(
                rule = rule,
                canMoveUp = i > 0,
                canMoveDown = i < rules.lastIndex,
                onEdit = { editingIndex = i },
                onToggle = { enabled ->
                    setRules(rules.toMutableList().also { it[i] = rule.copy(enabled = enabled) })
                },
                onMove = { direction ->
                    setRules(rules.toMutableList().also {
                        it[i] = rules[i + direction]
                        it[i + direction] = rule
                    })
                },
                onDelete = { setRules(rules.filterIndexed { j, _ -> j != i }) }
            )
        }

        Button(
            onClick = { editingIndex = NEW_RULE_INDEX },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(stringResource(R.string.paste_replace_rules_add))
        }
    }

    editingIndex?.let { index ->
        PasteReplaceRuleDialog(
            rule = rules.getOrNull(index),
            onDismiss = { editingIndex = null },
            onSave = { pattern, replacement ->
                setRules(
                    if(index == NEW_RULE_INDEX) {
                        rules + PasteReplaceRule(pattern = pattern, replacement = replacement)
                    } else {
                        rules.toMutableList().also {
                            it[index] = it[index].copy(pattern = pattern, replacement = replacement)
                        }
                    }
                )
                editingIndex = null
            }
        )
    }
}
