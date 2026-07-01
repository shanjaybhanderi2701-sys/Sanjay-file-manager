package com.appblish.filora.feature.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One hop in the breadcrumb trail: a human label and the [location] a tap navigates to
 * (FR-2.2). [location] is the same opaque locator the rest of navigation uses — a
 * filesystem path or a SAF tree-document URI string.
 */
internal data class BreadcrumbSegment(
    val label: String,
    val location: String,
)

/**
 * Splits a browser [location] into its tappable ancestor trail.
 *
 * Filesystem paths fan out into cumulative prefixes (`/a/b/c` → root, `/a`, `/a/b`,
 * `/a/b/c`) so every ancestor is its own navigable crumb. A blank or pure-root
 * location collapses to a single [rootLabel] crumb. SAF tree URIs are opaque — they
 * can't be safely decomposed into readable ancestors — so they render as one crumb
 * labelled by their last path segment.
 *
 * Pure (no Compose/Android dependency) so the ancestor-navigation logic is unit-testable
 * the same way [com.appblish.filora.feature.browser] keeps its mapping host-testable.
 */
internal fun breadcrumbSegments(
    location: String,
    rootLabel: String,
): List<BreadcrumbSegment> {
    val trimmed = location.trim()
    if (trimmed.isEmpty() || trimmed == "/") {
        return listOf(BreadcrumbSegment(label = rootLabel, location = location))
    }
    if (trimmed.startsWith("content://")) {
        val last = trimmed.substringAfterLast('/')
        val label = last.substringAfterLast("%3A").ifBlank { last }.ifBlank { rootLabel }
        return listOf(BreadcrumbSegment(label = label, location = location))
    }
    val parts = trimmed.split('/').filter { it.isNotEmpty() }
    val crumbs = ArrayList<BreadcrumbSegment>(parts.size + 1)
    crumbs += BreadcrumbSegment(label = rootLabel, location = "/")
    val builder = StringBuilder()
    for (part in parts) {
        builder.append('/').append(part)
        crumbs += BreadcrumbSegment(label = part, location = builder.toString())
    }
    return crumbs
}

/**
 * Horizontally scrollable breadcrumb bar (FR-2.2). The trailing crumb is the current
 * folder (non-clickable, emphasised); every ancestor is tappable and routes back up
 * via [onNavigate]. Auto-scrolls to keep the current folder in view as you descend.
 */
@Composable
internal fun BreadcrumbBar(
    location: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    rootLabel: String = stringResource(R.string.browser_breadcrumb_root),
) {
    val crumbs = remember(location, rootLabel) { breadcrumbSegments(location, rootLabel) }
    val scrollState = rememberScrollState()
    LaunchedEffect(crumbs.size) { scrollState.scrollTo(scrollState.maxValue) }

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            Text(
                text = crumb.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (isLast) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                modifier =
                    Modifier
                        .clickable(enabled = !isLast) { onNavigate(crumb.location) }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
