package com.halalclassified.app.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.halalclassified.app.R
import com.halalclassified.app.data.auth.AuthController
import com.halalclassified.app.data.profile.Profile
import com.halalclassified.app.data.profile.ProfileRepository
import com.halalclassified.app.data.profile.ProfileUpsert
import com.halalclassified.app.data.session.OnboardingStore
import com.halalclassified.app.data.session.SessionStore
import com.halalclassified.app.data.supabase.SupabaseClientProvider
import com.halalclassified.app.ui.auth.AuthFlowScreen
import com.halalclassified.app.ui.onboarding.TocPermissionsScreen
import com.halalclassified.app.ui.profile.ProfileCompletionScreen
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun AppEntry() {
    val supabase = SupabaseClientProvider.client
    val authController = remember { AuthController(supabase) }
    val authState by authController.state.collectAsState()
    val sessionStatus by supabase.auth.sessionStatus.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionStore = remember(context) { SessionStore(context) }
    val onboardingStore = remember(context) { OnboardingStore(context) }
    val profileRepository = remember { ProfileRepository(supabase) }
    var storedAccounts by remember { mutableStateOf(sessionStore.loadAccounts()) }
    var onboardingComplete by remember { mutableStateOf(onboardingStore.isCompleted()) }
    var profileState by remember { mutableStateOf<Profile?>(null) }
    var profileFetchLoading by remember { mutableStateOf(false) }
    var profileUpdateLoading by remember { mutableStateOf(false) }
    var profileLoaded by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }
    val webClientId = stringResource(R.string.google_web_client_id)
    val googleSignInClient = remember(context, webClientId) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()
        GoogleSignIn.getClient(context, options)
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            authController.endGoogleSignInWithError("Google sign-in canceled.")
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                authController.endGoogleSignInWithError("Google sign-in failed. Try again.")
            } else {
                scope.launch {
                    authController.signInWithGoogleIdToken(idToken)
                }
            }
        }.onFailure { error ->
            val message = if (error is ApiException) {
                "Google sign-in failed (code ${error.statusCode})."
            } else {
                error.message ?: "Google sign-in failed."
            }
            authController.endGoogleSignInWithError(message)
        }
    }

    LaunchedEffect(Unit) {
        supabase.auth.awaitInitialization()
    }

    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            sessionStore.saveSession((sessionStatus as SessionStatus.Authenticated).session)
            storedAccounts = sessionStore.loadAccounts()
        }
        if (sessionStatus is SessionStatus.NotAuthenticated) {
            profileState = null
            profileError = null
            profileFetchLoading = false
            profileUpdateLoading = false
            profileLoaded = false
        }
    }

    val currentSession = (sessionStatus as? SessionStatus.Authenticated)?.session
    LaunchedEffect(currentSession?.user?.id) {
        val userId = currentSession?.user?.id
        if (userId.isNullOrBlank()) {
            profileState = null
            profileFetchLoading = false
            profileLoaded = false
            return@LaunchedEffect
        }
        profileFetchLoading = true
        profileLoaded = false
        profileError = null
        profileState = runCatching { profileRepository.fetchProfile(userId) }
            .onFailure { profileError = it.message ?: "Unable to load profile." }
            .getOrNull()
        profileFetchLoading = false
        profileLoaded = true
    }

    if (!onboardingComplete) {
        TocPermissionsScreen(
            onContinue = {
                onboardingStore.setCompleted(true)
                onboardingComplete = true
            }
        )
    } else {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> {
                val user = (sessionStatus as SessionStatus.Authenticated).session.user
                when {
                    !profileLoaded && profileFetchLoading -> AuthLoadingScreen()
                    needsProfileCompletion(profileState) -> ProfileCompletionScreen(
                        name = user.displayName(),
                        email = user?.email ?: "",
                        isLoading = profileUpdateLoading,
                        errorMessage = profileError,
                        onSubmitDob = { dob ->
                            if (dob.isBlank()) {
                                profileError = "Please enter your date of birth."
                            } else {
                                scope.launch {
                                    profileUpdateLoading = true
                                    profileError = null
                                    val userId = user?.id ?: return@launch
                                    val upsert = ProfileUpsert(
                                        id = userId,
                                        firstName = user.firstName(),
                                        lastName = user.lastName(),
                                        email = user.email,
                                        dob = dob
                                    )
                                    profileState = runCatching { profileRepository.upsertProfile(upsert) }
                                        .onFailure { profileError = it.message ?: "Unable to update profile." }
                                        .getOrNull()
                                    profileUpdateLoading = false
                                }
                            }
                        }
                    )
                    else -> HalalClassifiedApp()
                }
            }
            is SessionStatus.LoadingFromStorage -> AuthLoadingScreen()
            is SessionStatus.NetworkError -> AuthStatusScreen(
                title = "Offline",
                subtitle = "Connect to the internet to continue."
            )
            is SessionStatus.NotAuthenticated -> AuthFlowScreen(
                storedAccounts = storedAccounts,
                authState = authState,
                onSelectAccount = { account ->
                    scope.launch {
                        authController.clearStatus()
                        sessionStore.getSession(account.id)?.let { stored ->
                            supabase.auth.importSession(stored.session)
                        }
                    }
                },
                onGoogleSignIn = {
                    authController.clearStatus()
                    if (webClientId.isBlank() || webClientId.startsWith("YOUR_")) {
                        authController.endGoogleSignInWithError("Set google_web_client_id in strings.xml.")
                    } else {
                        authController.startGoogleSignIn()
                        // Sign out locally to always show the account picker.
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    }
                },
                onEmailSignUp = { firstName, lastName, email, password, dob ->
                    scope.launch {
                        authController.signUpEmail(firstName, lastName, email, password, dob)
                    }
                },
                onLogin = { email, password ->
                    scope.launch {
                        authController.signInEmail(email, password)
                    }
                },
                onClearStatus = { authController.clearStatus() }
            )
        }
    }
}

private fun needsProfileCompletion(profile: Profile?): Boolean {
    return profile?.dob.isNullOrBlank()
}

private fun UserInfo?.displayName(): String {
    if (this == null) return ""
    val fullName = userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
    if (!fullName.isNullOrBlank()) return fullName
    val first = firstName()
    val last = lastName()
    return listOfNotNull(first, last).joinToString(" ").ifBlank { email ?: "" }
}

private fun UserInfo?.firstName(): String? {
    return this?.userMetadata?.get("first_name")?.jsonPrimitive?.contentOrNull
}

private fun UserInfo?.lastName(): String? {
    return this?.userMetadata?.get("last_name")?.jsonPrimitive?.contentOrNull
}

@Composable
private fun AuthLoadingScreen() {
    AuthStatusScreen(
        title = "Loading",
        subtitle = "Restoring your session."
    )
}

@Composable
private fun AuthStatusScreen(
    title: String,
    subtitle: String
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
