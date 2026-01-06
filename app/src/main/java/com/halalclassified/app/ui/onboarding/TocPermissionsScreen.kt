package com.halalclassified.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RawRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.halalclassified.app.R
import com.halalclassified.app.data.session.OnboardingStore

@Composable
fun TocPermissionsScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    val onboardingStore = remember(context) { OnboardingStore(context) }

    val currentTermsVersion = 1

    var onboardingStep by rememberSaveable { mutableStateOf(OnboardingStep.Terms) }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var permissionRefresh by remember { mutableStateOf(0) }
    var pendingContinue by remember { mutableStateOf(false) }

    val permissions = remember { buildPermissionList() }
    val grantedMap = remember(permissionRefresh) {
        permissions.associate { it.permission to isGranted(context, it.permission) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permissionRefresh += 1
        if (pendingContinue) {
            pendingContinue = false
            onContinue()
        }
    }

    fun requestMissingPermissions() {
        val missing = permissions.map { it.permission }.filter { !isGranted(context, it) }
        if (missing.isEmpty()) {
            onContinue()
        } else {
            pendingContinue = true
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    OnboardingBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (onboardingStep) {
                OnboardingStep.Terms -> TermsStep(
                    appName = stringResource(R.string.app_name),
                    termsResId = R.raw.terms_and_conditions_v1,
                    termsAccepted = termsAccepted,
                    onTermsAcceptedChange = { termsAccepted = it },
                    onContinue = {
                        onboardingStore.setAcceptedTermsVersion(currentTermsVersion)
                        onboardingStep = OnboardingStep.Permissions
                    }
                )
                OnboardingStep.Permissions -> PermissionsStep(
                    appName = stringResource(R.string.app_name),
                    permissions = permissions,
                    grantedMap = grantedMap,
                    onRequestPermission = { permissionLauncher.launch(arrayOf(it)) },
                    onContinue = { requestMissingPermissions() }
                )
            }
        }
    }
}

private enum class OnboardingStep {
    Terms,
    Permissions
}

@Composable
private fun ColumnScope.TermsStep(
    appName: String,
    @RawRes termsResId: Int,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = "Welcome to $appName",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = "Review and accept the Terms & Conditions to continue.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val context = LocalContext.current
    val termsText = remember(termsResId) { loadRawText(context, termsResId) }
    val scrollState = rememberScrollState()
    val hasReachedEnd by remember {
        derivedStateOf { scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue }
    }

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Terms & Conditions",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Scroll to the bottom to enable acceptance.",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasReachedEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            val progress = if (scrollState.maxValue == 0) 0f else scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (hasReachedEnd) "Reached the end" else "Reading…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = termsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = termsAccepted,
            enabled = hasReachedEnd,
            onCheckedChange = { onTermsAcceptedChange(it) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "I agree to the Terms & Conditions.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (hasReachedEnd) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Button(
        onClick = onContinue,
        enabled = termsAccepted,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = "Agree & continue")
    }
}

@Composable
private fun ColumnScope.PermissionsStep(
    appName: String,
    permissions: List<PermissionEntry>,
    grantedMap: Map<String, Boolean>,
    onRequestPermission: (String) -> Unit,
    onContinue: () -> Unit
) {
    Text(
        text = "Almost done",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = "Allow key permissions to unlock the best experience in $appName.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium
            )
            permissions.forEach { entry ->
                val granted = grantedMap[entry.permission] == true
                PermissionRow(
                    title = entry.title,
                    description = entry.description,
                    granted = granted,
                    onRequest = { onRequestPermission(entry.permission) }
                )
            }
        }
    }

    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = "Continue")
    }

    Text(
        text = "You can change permissions later in Settings.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun loadRawText(context: Context, @RawRes resId: Int): String {
    return runCatching {
        context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }.getOrElse { "Unable to load terms. Please try again." }
}

private data class PermissionEntry(
    val title: String,
    val description: String,
    val permission: String
)

private fun buildPermissionList(): List<PermissionEntry> {
    val items = mutableListOf<PermissionEntry>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        items.add(
            PermissionEntry(
                title = "Notifications",
                description = "Get updates for chats and listing activity.",
                permission = Manifest.permission.POST_NOTIFICATIONS
            )
        )
        items.add(
            PermissionEntry(
                title = "Photos",
                description = "Upload images for your ads.",
                permission = Manifest.permission.READ_MEDIA_IMAGES
            )
        )
    } else {
        items.add(
            PermissionEntry(
                title = "Photos",
                description = "Upload images for your ads.",
                permission = Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }
    return items
}

private fun isGranted(context: Context, permission: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        if (granted) {
            Text(
                text = "Granted",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            OutlinedButton(onClick = onRequest) {
                Text(text = "Allow")
            }
        }
    }
}

@Composable
private fun TermsBullet(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingBackground(content: @Composable () -> Unit) {
    val topGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    val bottomGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(240.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(topGlow, Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .alpha(0.9f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(220.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(bottomGlow, Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .alpha(0.9f)
        )
        content()
    }
}
