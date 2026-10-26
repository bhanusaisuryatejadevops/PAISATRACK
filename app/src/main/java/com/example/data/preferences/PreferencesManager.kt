package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paisatrack_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        private val KEY_IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        private val KEY_OVERALL_BUDGET_PAISA = longPreferencesKey("overall_budget_paisa")
        private val KEY_DEFAULT_PAYMENT_MODE = stringPreferencesKey("default_payment_mode")
    }

    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val code = prefs[KEY_APP_LANGUAGE] ?: "en"
        AppLanguage.fromCode(code)
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language.code
        }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_APP_LOCK_ENABLED] ?: false
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_HASH]
    }

    val pinSalt: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_SALT]
    }

    suspend fun setAppLock(enabled: Boolean, hash: String? = null, salt: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_APP_LOCK_ENABLED] = enabled
            if (hash != null) prefs[KEY_PIN_HASH] = hash
            if (salt != null) prefs[KEY_PIN_SALT] = salt
            if (!enabled) {
                prefs.remove(KEY_PIN_HASH)
                prefs.remove(KEY_PIN_SALT)
            }
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_ONBOARDING_COMPLETED] = completed
        }
    }

    val overallBudgetPaisa: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_OVERALL_BUDGET_PAISA] ?: 0L
    }

    suspend fun setOverallBudgetPaisa(amount: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OVERALL_BUDGET_PAISA] = amount
        }
    }

    val defaultPaymentMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_PAYMENT_MODE] ?: "UPI"
    }

    suspend fun setDefaultPaymentMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_PAYMENT_MODE] = mode
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
