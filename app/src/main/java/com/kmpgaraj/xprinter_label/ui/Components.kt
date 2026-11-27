package com.kmpgaraj.xprinter_label.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmpgaraj.xprinter_label.ui.theme.palette

private val FieldShape = RoundedCornerShape(12.dp)

/** White top bar with the app logo, name and printer status. */
@Composable
fun AppHeader(
    title: String,
    statusText: String,
    statusOk: Boolean,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.palette
    Column(modifier.fillMaxWidth().background(c.card)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = horizontalPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(c.accent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.Printer, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                color = c.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f)
            )
            Box(Modifier.size(9.dp).background(if (statusOk) c.ok else c.off, CircleShape))
            Text(statusText, color = c.mute, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = c.line)
    }
}

/** A white rounded panel with a hairline border; children are laid out in a column. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = MaterialTheme.palette
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.card, shape)
            .border(1.dp, c.line, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.palette.ink,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun ErrorText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MaterialTheme.palette.error,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

/** A soft indigo hint box. */
@Composable
fun NoteBox(text: String, modifier: Modifier = Modifier) {
    val c = MaterialTheme.palette
    Text(
        text = text,
        color = c.mute,
        fontSize = 13.sp,
        modifier = modifier
            .fillMaxWidth()
            .background(c.accentSoft, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/** A pill-shaped switcher: the selected option is a raised white tab. */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.tabTrack, RoundedCornerShape(14.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val shape = RoundedCornerShape(10.dp)
        options.forEachIndexed { index, text ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .then(if (selected) Modifier.shadow(2.dp, shape) else Modifier)
                    .clip(shape)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .background(if (selected) c.tabSelected else c.tabTrack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (selected) c.accentText else c.mute,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Small bold label that sits above a field. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, optional: Boolean = false) {
    val c = MaterialTheme.palette
    Text(
        text = buildAnnotatedString {
            append(text)
            if (optional) {
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = c.mute)) { append("(optional)") }
            }
        },
        color = c.label,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** The rounded, bordered box every field shares. The border turns accent-coloured when [focused]. */
@Composable
private fun FieldBox(
    focused: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    role: Role = Role.Button,
    verticalPadding: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.CenterStart,
    content: @Composable BoxScope.() -> Unit
) {
    val c = MaterialTheme.palette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(FieldShape)
            .then(if (onClick != null) Modifier.clickable(role = role, onClick = onClick) else Modifier)
            .background(c.field)
            .border(1.5.dp, if (focused) c.accent else c.border, FieldShape)
            .padding(horizontal = 14.dp, vertical = verticalPadding),
        contentAlignment = contentAlignment,
        content = content
    )
}

private val FieldTextStyle: TextStyle
    @Composable get() = TextStyle(
        color = MaterialTheme.palette.ink,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
    )

/** A text input with its label above it. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    optional: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false
) {
    val c = MaterialTheme.palette
    var focused by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label, optional = optional)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            maxLines = if (multiline) 6 else 1,
            textStyle = FieldTextStyle,
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                FieldBox(
                    focused = focused,
                    verticalPadding = if (multiline) 12.dp else 0.dp,
                    contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, color = c.placeholder, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    innerTextField()
                }
            }
        )
    }
}

/** A dropdown that looks like the text fields. [onSelect] gets the index into [options]. */
@Composable
fun SelectField(
    label: String,
    valueText: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.palette
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Box {
            FieldBox(focused = expanded, onClick = { expanded = true }, role = Role.DropdownList) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(valueText, style = FieldTextStyle, modifier = Modifier.weight(1f))
                    Icon(
                        AppIcons.ChevronDown,
                        contentDescription = null,
                        tint = c.mute,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, text ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * A read-only field that reports taps, used to open a date picker. When it has a
 * value and [onClear] is given, a clear button replaces the calendar icon.
 */
@Composable
fun DateSelectField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    val c = MaterialTheme.palette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        FieldBox(focused = false, onClick = onClick) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = c.placeholder,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(value, style = FieldTextStyle, modifier = Modifier.weight(1f))
                }
                if (value.isNotEmpty() && onClear != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onClear),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.Close,
                            contentDescription = "Clear $label",
                            tint = c.mute,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Icon(
                        AppIcons.Calendar,
                        contentDescription = null,
                        tint = c.mute,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** The big call-to-action. Greys out when [enabled] is false; shows a spinner while [loading]. */
@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.palette
    val shape = RoundedCornerShape(16.dp)
    val contentColor = if (enabled) c.onAccent else c.mute
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(
                if (enabled) {
                    Modifier.shadow(10.dp, shape, ambientColor = c.accent, spotColor = c.accent)
                } else Modifier
            )
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .background(if (enabled) c.accent else c.disabled),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.5.dp,
                color = contentColor
            )
            Box(Modifier.width(10.dp))
        }
        Text(text, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** A labelled on/off switch; the whole row is the tap target. */
@Composable
fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val c = MaterialTheme.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = c.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (description != null) {
                Text(description, color = c.mute, fontSize = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.accent,
                uncheckedThumbColor = c.mute,
                uncheckedTrackColor = c.field,
                uncheckedBorderColor = c.border
            )
        )
    }
}

/** A small outlined button for secondary actions. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val c = MaterialTheme.palette
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .background(c.card)
            .border(1.5.dp, c.border, shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = c.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** One selectable printer in the USB printer list. */
@Composable
fun PrinterRow(
    name: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = MaterialTheme.palette
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .background(if (selected) c.accentSoft else c.card)
            .border(1.5.dp, if (selected) c.accent else c.line, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.mute)
        )
        Text(
            name,
            color = c.label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(detail, color = c.mute, fontSize = 12.sp)
    }
}
