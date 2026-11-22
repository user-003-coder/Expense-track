package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CurrencyOption
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "verdant_user_prefs"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIDE_BALANCE = booleanPreferencesKey("hide_balance")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
        val PRIVACY_SCREEN_ENABLED = booleanPreferencesKey("privacy_screen_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        UserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "Aarav Sharma",
            userEmail = prefs[Keys.USER_EMAIL] ?: "aarav.sharma@verdant.app",
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: CurrencyOption.INR.code,
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME_MODE] }
                ?: ThemeMode.SYSTEM,
            hideBalance = prefs[Keys.HIDE_BALANCE] ?: false,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH],
            budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
            privacyScreenEnabled = prefs[Keys.PRIVACY_SCREEN_ENABLED] ?: false
        )
    }

    suspend fun updateProfile(name: String, email: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().ifEmpty { "Aarav Sharma" }
            prefs[Keys.USER_EMAIL] = email.trim().ifEmpty { "aarav.sharma@verdant.app" }
        }
    }

    suspend fun setCurrency(code: String) {
        context.userPreferencesDataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setHideBalance(hidden: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.HIDE_BALANCE] = hidden }
    }

    suspend fun setAppLock(enabled: Boolean, pinHash: String? = null) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
            if (pinHash != null) {
                prefs[Keys.PIN_HASH] = pinHash
            } else if (!enabled) {
                prefs.remove(Keys.PIN_HASH)
            }
        }
    }

    suspend fun setBudgetAlertsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.BUDGET_ALERTS_ENABLED] = enabled }
    }

    suspend fun setPrivacyScreenEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.PRIVACY_SCREEN_ENABLED] = enabled }
    }
}
