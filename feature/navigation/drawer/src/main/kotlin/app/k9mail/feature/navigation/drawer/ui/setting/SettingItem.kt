package app.k9mail.feature.navigation.drawer.ui.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
internal fun SettingItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.width(MainTheme.sizes.large),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.padding(vertical = MainTheme.spacings.oneHalf),
            color = MainTheme.colors.surfaceContainer,
            shape = CircleShape,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = label,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(MainTheme.spacings.oneHalf),
            )
        }
    }
}