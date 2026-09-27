package com.devos.inputpasteparser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A single action button (icon + label) used in [SmartInputPasteItem]'s
 * button row — Type, Paste, File, Photo, Video, Record.
 *
 * [isActive] flips the button into its "active" color scheme (used for the
 * Record button while recording is in progress).
 */
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = 2.dp,
        onClick = onClick
    ) {

        Column(
            modifier = Modifier.padding(
                vertical = 10.dp,
                horizontal = 4.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isActive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}