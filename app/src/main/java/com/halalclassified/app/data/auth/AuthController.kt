package com.halalclassified.app.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class AuthUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class AuthController(
    private val supabase: SupabaseClient
) {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearStatus() {
        _state.update { it.copy(message = null, error = null) }
    }

    suspend fun signUpEmail(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        dob: String
    ) {
        updateLoading(true)
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password
                data = buildJsonObject {
                    if (firstName.isNotBlank()) put("first_name", firstName.trim())
                    if (lastName.isNotBlank()) put("last_name", lastName.trim())
                    if (dob.isNotBlank()) put("dob", dob.trim())
                }
            }
        }.onSuccess {
            _state.update {
                it.copy(
                    isLoading = false,
                    message = "Account created. Check your email to verify before logging in.",
                    error = null
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to create account. Try again.",
                    message = null
                )
            }
        }
    }

    suspend fun signInEmail(email: String, password: String) {
        updateLoading(true)
        runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.onSuccess {
            _state.update { it.copy(isLoading = false, error = null, message = null) }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isLoading = false,
                    error = error.message ?: "Login failed. Check your credentials.",
                    message = null
                )
            }
        }
    }

    suspend fun signInWithGoogle() {
        updateLoading(true)
        runCatching {
            supabase.auth.signInWith(Google)
        }.onSuccess {
            _state.update {
                it.copy(
                    isLoading = false,
                    message = "Continue in your browser to finish Google sign-in.",
                    error = null
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    isLoading = false,
                    error = error.message ?: "Google sign-in failed. Try again.",
                    message = null
                )
            }
        }
    }

    private fun updateLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading, error = null, message = null) }
    }
}
