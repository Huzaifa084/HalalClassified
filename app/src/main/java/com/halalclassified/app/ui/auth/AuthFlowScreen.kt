package com.halalclassified.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.halalclassified.app.data.auth.AuthUiState
import com.halalclassified.app.data.session.StoredAccount
import java.util.Calendar
import java.util.Locale

private enum class AuthStep {
    AccountSwitcher,
    Launch,
    EmailSignUp,
    Login
}

@Composable
fun AuthFlowScreen(
    storedAccounts: List<StoredAccount>,
    authState: AuthUiState,
    onSelectAccount: (StoredAccount) -> Unit,
    onGoogleSignIn: () -> Unit,
    onEmailSignUp: (String, String, String, String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onClearStatus: () -> Unit
) {
    val initialStep = if (storedAccounts.isNotEmpty()) {
        AuthStep.AccountSwitcher
    } else {
        AuthStep.Launch
    }
    var step by rememberSaveable { mutableStateOf(initialStep) }

    when (step) {
        AuthStep.AccountSwitcher -> AccountSwitcherScreen(
            accounts = storedAccounts,
            authState = authState,
            onSelect = { onSelectAccount(it) },
            onAddNew = {
                onClearStatus()
                step = AuthStep.Launch
            }
        )
        AuthStep.Launch -> LaunchScreen(
            authState = authState,
            onGoogle = {
                onClearStatus()
                onGoogleSignIn()
            },
            onEmailSignUp = {
                onClearStatus()
                step = AuthStep.EmailSignUp
            },
            onLogin = {
                onClearStatus()
                step = AuthStep.Login
            }
        )
        AuthStep.EmailSignUp -> EmailSignUpScreen(
            authState = authState,
            onBack = {
                onClearStatus()
                step = AuthStep.Launch
            },
            onLogin = {
                onClearStatus()
                step = AuthStep.Login
            },
            onSubmit = onEmailSignUp
        )
        AuthStep.Login -> LoginScreen(
            authState = authState,
            onBack = {
                onClearStatus()
                step = AuthStep.Launch
            },
            onCreateAccount = {
                onClearStatus()
                step = AuthStep.EmailSignUp
            },
            onSubmit = onLogin
        )
    }
}

@Composable
private fun LaunchScreen(
    authState: AuthUiState,
    onGoogle: () -> Unit,
    onEmailSignUp: () -> Unit,
    onLogin: () -> Unit
) {
    AuthScreenFrame(
        title = "Welcome to Halal Classified",
        subtitle = "Find trusted Qurbani animals with verified listings across Pakistan.",
        authState = authState
    ) {
        GoogleButton(
            onClick = onGoogle,
            enabled = !authState.isLoading,
            loading = authState.isLoading
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = "Sign up with Email",
            onClick = onEmailSignUp,
            enabled = !authState.isLoading
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !authState.isLoading
        ) {
            Text(text = "Login")
        }
    }
}

@Composable
private fun EmailSignUpScreen(
    authState: AuthUiState,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var dobDisplay by rememberSaveable { mutableStateOf("") }
    var dobIso by rememberSaveable { mutableStateOf("") }

    AuthScreenFrame(
        title = "Create your account",
        subtitle = "Post listings, chat with sellers, and save favorites.",
        onBack = onBack,
        authState = authState
    ) {
        AuthField(
            label = "First name",
            value = firstName,
            onValueChange = { firstName = it },
            placeholder = "Enter your first name",
            leadingIcon = Icons.Outlined.Person,
            enabled = !authState.isLoading
        )
        AuthField(
            label = "Last name",
            value = lastName,
            onValueChange = { lastName = it },
            placeholder = "Enter your last name",
            leadingIcon = Icons.Outlined.Person,
            enabled = !authState.isLoading
        )
        AuthField(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "you@email.com",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            enabled = !authState.isLoading
        )
        AuthField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            placeholder = "Create a password",
            leadingIcon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            enabled = !authState.isLoading
        )
        DatePickerField(
            label = "Date of birth",
            value = dobDisplay,
            placeholder = "DD/MM/YYYY",
            leadingIcon = Icons.Outlined.Today,
            enabled = !authState.isLoading,
            onDateSelected = { millis ->
                dobDisplay = formatDateDisplay(millis)
                dobIso = formatDateIso(millis)
            }
        )
        PrimaryButton(
            text = "Create account",
            onClick = { onSubmit(firstName, lastName, email, password, dobIso) },
            enabled = !authState.isLoading,
            loading = authState.isLoading
        )
        TextButton(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !authState.isLoading
        ) {
            Text(text = "Already have an account? Login")
        }
    }
}

@Composable
private fun LoginScreen(
    authState: AuthUiState,
    onBack: () -> Unit,
    onCreateAccount: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthScreenFrame(
        title = "Login",
        subtitle = "Welcome back. Continue where you left off.",
        onBack = onBack,
        authState = authState
    ) {
        AuthField(
            label = "Email",
            value = email,
            onValueChange = { email = it },
            placeholder = "you@email.com",
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            enabled = !authState.isLoading
        )
        AuthField(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            placeholder = "Enter your password",
            leadingIcon = Icons.Outlined.Lock,
            keyboardType = KeyboardType.Password,
            isPassword = true,
            enabled = !authState.isLoading
        )
        PrimaryButton(
            text = "Login",
            onClick = { onSubmit(email, password) },
            enabled = !authState.isLoading,
            loading = authState.isLoading
        )
        TextButton(
            onClick = onCreateAccount,
            modifier = Modifier.fillMaxWidth(),
            enabled = !authState.isLoading
        ) {
            Text(text = "Create new account")
        }
    }
}

@Composable
private fun AccountSwitcherScreen(
    accounts: List<StoredAccount>,
    authState: AuthUiState,
    onSelect: (StoredAccount) -> Unit,
    onAddNew: () -> Unit
) {
    AuthScreenFrame(
        title = "Welcome back",
        subtitle = "Choose a saved account to continue.",
        authState = authState
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            accounts.forEach { account ->
                ElevatedCard(
                    onClick = {
                        if (!authState.isLoading) {
                            onSelect(account)
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = account.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = account.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Continue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        OutlinedButton(
            onClick = onAddNew,
            modifier = Modifier.fillMaxWidth(),
            enabled = !authState.isLoading
        ) {
            Text(text = "Use another account")
        }
    }
}

@Composable
private fun AuthScreenFrame(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    authState: AuthUiState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandHeader(onBack = onBack)

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AuthStatusBanner(authState)
                    Spacer(modifier = Modifier.height(4.dp))
                    content()
                }
            }

            Text(
                text = "By continuing, you agree to our community guidelines for halal listings.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun AuthStatusBanner(state: AuthUiState?) {
    val message = state?.error ?: state?.message ?: return
    val isError = state?.error != null
    val container = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = container,
        contentColor = onContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BrandHeader(onBack: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 2.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "HC",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column {
            Text(
                text = "Halal Classified",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Qurbani animal marketplace",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        leadingIcon = leadingIcon?.let {
            {
                Icon(imageVector = it, contentDescription = null)
            }
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = MaterialTheme.shapes.large,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    value: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onDateSelected: (Long) -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(millis)
                        }
                        showDialog = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDialog = true }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = label) },
            placeholder = { Text(text = placeholder) },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(imageVector = it, contentDescription = null)
                }
            },
            singleLine = true,
            readOnly = true,
            enabled = enabled,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun ReadOnlyField(
    label: String,
    value: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = label) },
        readOnly = true,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text)
    }
}

@Composable
private fun GoogleButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled && !loading,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "G", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = if (loading) "Opening Google..." else "Continue with Google")
    }
}

@Composable
private fun AuthBackground(content: @Composable BoxScope.() -> Unit) {
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

private fun formatDateDisplay(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    return String.format(Locale.US, "%02d/%02d/%04d", day, month, year)
}

private fun formatDateIso(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
}
