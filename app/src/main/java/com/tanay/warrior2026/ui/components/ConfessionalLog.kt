package com.tanay.warrior.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── ConfessionalLog.kt ────────────────────────────────────────
// Confessional Log — v1.0.0
//
// A raw, minimal confession entry surface.
// Designed to feel like a private journal entry — no chrome,
// no decoration, no softening. Black on black, red on black.
//
// Layout (top → bottom):
//   HEADER      — "CONFESSIONAL" label + hairline rule
//   LAST LOG    — shown only if lastConfession != null
//                 "LAST TIME YOU SAID:" label in dim red
//                 Previous confession text in muted red
//                 Hairline separator
//   INPUT AREA  — BasicTextField, monospace feel, dim red cursor
//                 Placeholder: "Write what broke you. No excuses."
//   SUBMIT      — "SEAL IT" button, full width, dark red fill
//
// Design rules:
//   • BasicTextField — no Material decoration, no underline, no border flash
//   • FontFamily.Monospace for all body text — cold, clinical, honest
//   • SolidColor(DimRed) cursor — the only color that bleeds through
//   • No emojis anywhere
//   • Suitable as bottom sheet content OR full-screen overlay —
//     caller decides the outer container and clip shape
//
// Public API:
//   ConfessionalSheet(
//       onSubmit:       (String) -> Unit,
//       lastConfession: String?           = null,
//       modifier:       Modifier          = Modifier,
//   )
// ─────────────────────────────────────────────────────────────

// ── Colors ────────────────────────────────────────────────────

private val SheetBg        = Color(0xFF050505)
private val DimRed         = Color(0xFF8B1A1A)
private val MutedRed       = Color(0xFF6B1010)
private val InputText      = Color(0xFFCC3333)
private val PlaceholderClr = Color(0xFF3A2020)
private val RuleColor      = Color(0xFF1E0A0A)
private val HeaderLabel    = Color(0xFF4A1A1A)
private val LastLabel      = Color(0xFF5A1A1A)
private val SealBg         = Color(0xFF1A0505)
private val SealBorder     = Color(0xFF3A0808)
private val SealText       = Color(0xFFCC2222)

// ── Internal sub-composables ──────────────────────────────────

@Composable
private fun HairlineRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RuleColor)
    )
}

@Composable
private fun ConfessionalHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text          = "CONFESSIONAL",
            fontSize      = 9.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = HeaderLabel,
            letterSpacing = 5.sp,
            fontFamily    = FontFamily.Monospace,
        )
        HairlineRule()
    }
}

@Composable
private fun LastConfessionBlock(confession: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text          = "LAST TIME YOU SAID:",
            fontSize      = 8.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = LastLabel,
            letterSpacing = 3.sp,
            fontFamily    = FontFamily.Monospace,
        )
        Text(
            text       = confession,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Normal,
            color      = MutedRed,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace,
        )
        HairlineRule()
    }
}

@Composable
private fun InputField(
    value:    String,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value           = value,
        onValueChange   = onChange,
        modifier        = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 140.dp),
        textStyle       = TextStyle(
            color      = InputText,
            fontSize   = 14.sp,
            lineHeight = 22.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
        ),
        cursorBrush     = SolidColor(DimRed),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType   = KeyboardType.Text,
        ),
        decorationBox   = { innerTextField ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(
                        text       = "Write what broke you. No excuses.",
                        fontSize   = 14.sp,
                        lineHeight = 22.sp,
                        color      = PlaceholderClr,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun SealButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    // indication = null — no ripple, keeps the cold dark aesthetic intact
    val source = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) SealBg else Color(0xFF0A0A0A))
            .border(
                width = 1.dp,
                color = if (enabled) SealBorder else Color(0xFF151515),
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(
                interactionSource = source,
                indication        = null,
                enabled           = enabled,
                onClick           = onClick,
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text          = "SEAL IT",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = if (enabled) SealText else Color(0xFF2A2A2A),
            letterSpacing = 5.sp,
            fontFamily    = FontFamily.Monospace,
        )
    }
}

// ── Public composable ─────────────────────────────────────────

/**
 * Confession entry surface.
 *
 * @param onSubmit       Called with the typed text when user taps "SEAL IT".
 *                       Caller is responsible for persisting the text and
 *                       passing it back as [lastConfession] next time.
 * @param lastConfession Previous confession to show above the input, or null.
 * @param modifier       Outer modifier — caller sets width, height, clip.
 *                       Example for bottom sheet:
 *                         Modifier
 *                           .fillMaxWidth()
 *                           .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
 */
@Composable
fun ConfessionalSheet(
    onSubmit:       (String) -> Unit,
    lastConfession: String?  = null,
    modifier:       Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .background(SheetBg)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ConfessionalHeader()

        if (!lastConfession.isNullOrBlank()) {
            LastConfessionBlock(confession = lastConfession)
        }

        InputField(value = text, onChange = { text = it })

        // Flexible spacer — pushes SEAL IT toward bottom in tall containers
        Spacer(modifier = Modifier.weight(1f, fill = false))

        SealButton(
            enabled = text.isNotBlank(),
            onClick = {
                if (text.isNotBlank()) {
                    onSubmit(text.trim())
                    text = ""
                }
            },
        )
    }
}
