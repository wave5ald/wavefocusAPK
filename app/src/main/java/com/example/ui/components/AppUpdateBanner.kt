package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.FocusIndigo
import com.example.ui.viewmodel.RoutineViewModel
import com.example.update.UpdateState

/**
 * Slim, non-blocking "update available" strip shown under the top bar.
 * Disappears on its own once the update is installed or dismissed; offline
 * errors are only reported in Settings → Updates.
 */
@Composable
fun AppUpdateBanner(viewModel: RoutineViewModel) {
    val state by viewModel.updateState.collectAsStateWithLifecycle()
    val dismissed by viewModel.updateBannerDismissed.collectAsStateWithLifecycle()
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isArabic = language == "ar"

    if (dismissed) return

    when (val current = state) {
        is UpdateState.Available -> {
            BannerShell {
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = FocusIndigo,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) {
                            "يتوفر تحديث ${current.release.versionName}"
                        } else {
                            "Update ${current.release.versionName} is available"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (current.release.releaseNotes.isNotBlank()) {
                        Text(
                            text = current.release.releaseNotes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Button(
                    onClick = { viewModel.downloadAndInstallUpdate() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(if (isArabic) "تحديث" else "Update", fontSize = 12.sp)
                }
                TextButton(onClick = { viewModel.dismissUpdateBanner() }) {
                    Text(
                        if (isArabic) "لاحقًا" else "Later",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is UpdateState.Downloading -> {
            BannerShell {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isArabic) {
                            "جارٍ تنزيل التحديث… ${current.percent}%"
                        } else {
                            "Downloading update… ${current.percent}%"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LinearProgressIndicator(
                        progress = { current.percent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        is UpdateState.Ready -> {
            BannerShell {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isArabic) {
                            "التحديث جاهز للتثبيت"
                        } else {
                            "Update ready to install"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isArabic) {
                            "السماح بالتثبيت من Wave Focus عند الطلب."
                        } else {
                            "Allow installs from Wave Focus when Android asks."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { viewModel.installDownloadedUpdate() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    )
                ) {
                    Text(if (isArabic) "تثبيت" else "Install", fontSize = 12.sp)
                }
            }
        }

        else -> Unit
    }
}

@Composable
private fun BannerShell(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Surface(
        color = FocusIndigo.copy(alpha = 0.12f),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            content()
        }
    }
}
