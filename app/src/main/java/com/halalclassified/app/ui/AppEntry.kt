package com.halalclassified.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.halalclassified.app.ui.auth.AuthFlowScreen
import com.halalclassified.app.ui.auth.StoredAccount

@Composable
fun AppEntry() {
    var isAuthenticated by rememberSaveable { mutableStateOf(false) }
    val storedAccounts = remember {
        listOf(
            StoredAccount(name = "Ayesha Khan", email = "ayesha@email.com"),
            StoredAccount(name = "Umar Farooq", email = "umar@email.com")
        )
    }

    if (isAuthenticated) {
        HalalClassifiedApp()
    } else {
        AuthFlowScreen(
            storedAccounts = storedAccounts,
            onAuthenticated = { isAuthenticated = true }
        )
    }
}
