package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WaveTheme
import com.example.ui.viewmodel.NavTab

/* ==========================================================================
   Wave component library — Compose mirror of pwa/css/app.css
   ========================================================================== */

/** PWA brandmark: rounded gradient tile with a wave glyph. */
@Composable
fun WaveBrandmark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(primary, secondary)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val stroke = h * 0.11f
            for (i in 0..1) {
                val path = Path().apply {
                    val baseline = h * (0.56f + i * 0.18f)
                    moveTo(w * 0.16f, baseline)
                    cubicTo(
                        w * 0.34f, baseline - h * 0.20f,
                        w * 0.50f, baseline + h * 0.20f,
                        w * 0.68f, baseline - h * 0.02f
                    )
                    cubicTo(
                        w * 0.78f, baseline - h * 0.13f,
                        w * 0.84f, baseline - h * 0.14f,
                        w * 0.88f, baseline - h * 0.10f
                    )
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = if (i == 0) 0.95f else 0.55f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/** Page background: base color plus the PWA radial glows. */
@Composable
fun WaveBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val wave = WaveTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(scheme.primary.copy(alpha = if (wave.isDark) 0.10f else 0.07f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = 900f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(scheme.secondary.copy(alpha = if (wave.isDark) 0.08f else 0.05f), Color.Transparent),
                    center = Offset(2200f, 1600f),
                    radius = 1100f
                )
            )
    ) {
        content()
    }
}

/** Compact pill used by the top bar (PWA `.chip`). */
@Composable
fun WaveTopChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    border: BorderStroke? = null,
    testTag: String? = null
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        contentColor = contentColor,
        border = border,
        modifier = modifier.then(
            if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = contentColor)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * PWA top bar: brandmark + title on the leading side, chips and menu on the
 * trailing side. Transparent so the page gradient shows through.
 */
@Composable
fun WaveTopBar(
    title: String,
    editionLabel: String,
    editionActive: Boolean,
    languageLabel: String,
    onToggleEdition: () -> Unit,
    onToggleLanguage: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val wave = WaveTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaveBrandmark(size = 28.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
            color = scheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))

        WaveTopChip(
            text = editionLabel,
            onClick = onToggleEdition,
            icon = Icons.Default.Mosque,
            container = if (editionActive) Color(0xFF0F766E).copy(alpha = 0.22f) else wave.primarySoft,
            contentColor = if (editionActive) Color(0xFF5EEAD4) else scheme.onPrimaryContainer,
            border = if (editionActive) BorderStroke(1.dp, Color(0xFF0F766E)) else null,
            testTag = "top_hijri_version_button"
        )
        Spacer(Modifier.width(6.dp))
        WaveTopChip(
            text = languageLabel,
            onClick = onToggleLanguage,
            icon = Icons.Default.Language,
            container = wave.primarySoft,
            contentColor = scheme.primary,
            testTag = "top_language_toggle_button"
        )
        Spacer(Modifier.width(2.dp))
        Surface(
            onClick = onMenu,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = scheme.onSurface,
            modifier = Modifier
                .size(38.dp)
                .testTag("top_right_menu_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }
}

private data class WaveNavItem(val tab: NavTab, val label: String, val icon: ImageVector, val tag: String)

/** PWA bottom tab bar with an active pill behind the icon. */
@Composable
fun WaveBottomBar(
    currentTab: NavTab,
    labels: Map<NavTab, String>,
    icons: Map<NavTab, ImageVector>,
    onSelect: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val wave = WaveTheme.colors
    val order = listOf(NavTab.ROUTINE, NavTab.FOCUS, NavTab.SCRIPTS, NavTab.NOTES)

    Surface(
        color = wave.navBackground,
        contentColor = scheme.onSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(scheme.outline)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                order.forEach { tab ->
                    val selected = currentTab == tab
                    val color = if (selected) scheme.primary else wave.onSurfaceMuted
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .testTag("nav_tab_${tab.name.lowercase()}")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(tab) }
                    ) {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .widthIn(min = 46.dp)
                                .clip(CircleShape)
                                .background(if (selected) wave.primarySoft else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icons[tab] ?: Icons.Default.Check,
                                contentDescription = labels[tab],
                                tint = color,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = labels[tab].orEmpty(),
                            color = color,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Standard PWA card: surface, 20dp radius, 1dp outline, soft shadow. */
@Composable
fun WaveCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    container: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Hero card with a primary glow in the top corner (PWA `.card--hero`). */
@Composable
fun WaveHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val wave = WaveTheme.colors
    val glowColor = if (wave.isDark) scheme.primary.copy(alpha = 0.18f) else scheme.primary.copy(alpha = 0.12f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.28f)),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(Float.MAX_VALUE, 0f),
                        radius = 700f
                    )
                )
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun WaveCardHead(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Uppercase group heading with a trailing divider (PWA `.group-label`). */
@Composable
fun WaveSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    val wave = WaveTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = wave.onSurfaceMuted,
            maxLines = 1
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(wave.outlineSoft)
        )
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = wave.onSurfaceMuted
            )
        }
    }
}

/** Filter pill: opaque inverted fill when active (PWA `.filter.is-active`). */
@Composable
fun WaveFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val wave = WaveTheme.colors
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) scheme.onSurface else scheme.surface,
        contentColor = if (selected) scheme.background else wave.onSurfaceMuted,
        border = BorderStroke(1.dp, if (selected) scheme.onSurface else scheme.outline),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            if (dotColor != null) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (selected) scheme.background else dotColor)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                maxLines = 1
            )
            if (trailing != null) {
                Spacer(Modifier.width(4.dp))
                trailing()
            }
        }
    }
}

/** Accent badge used inside cards for streaks, counts, etc. */
@Composable
fun WaveBadge(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

/**
 * Circular progress ring with the PWA primary→indigo sweep.
 */
@Composable
fun WaveProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    label: String? = null,
    caption: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (clamped > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(scheme.primary, scheme.secondary, scheme.primary)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface
                )
            }
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = WaveTheme.colors.onSurfaceMuted
                )
            }
        }
    }
}

/** PWA task row: accent bar, round check, title + meta, trailing slot. */
@Composable
fun WaveTaskRow(
    title: String,
    meta: String,
    accent: Color,
    completed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val wave = WaveTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outline),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 11.dp, bottom = 11.dp)
        ) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent.copy(alpha = 0.85f))
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (completed) accent else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (completed) accent else wave.onSurfaceMuted.copy(alpha = 0.65f),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (completed) 0.62f else 1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = wave.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun WaveEmptyState(
    emoji: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 18.dp, vertical = 30.dp)
    ) {
        Text(text = emoji, fontSize = 30.sp)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = WaveTheme.colors.onSurfaceMuted,
            textAlign = TextAlign.Center
        )
    }
}

/** Primary FAB pill (PWA `.fab`). */
@Composable
fun WaveFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 15.dp, end = 18.dp, top = 14.dp, bottom = 14.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
        }
    }
}

/** Segmented control used by Focus modes / notes filters. */
@Composable
fun WaveSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(11.dp),
                color = if (active) scheme.surface else Color.Transparent,
                contentColor = if (active) scheme.onSurface else WaveTheme.colors.onSurfaceMuted,
                shadowElevation = if (active) 2.dp else 0.dp,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                )
            }
        }
    }
}

/** Settings / sheet style list row. */
@Composable
fun WaveListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconContainer: Color = WaveTheme.colors.primarySoft,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val wave = WaveTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = wave.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Two-column stat tile used by Progress (PWA `.stat`). */
@Composable
fun WaveStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    emoji: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outline),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (emoji != null) {
                    Text(emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WaveTheme.colors.onSurfaceMuted,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                color = accent,
                maxLines = 1
            )
        }
    }
}
