package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PaisaExpenseRed
import com.example.util.AppLanguage
import com.example.util.AppStrings

@Composable
fun SettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    isAppLockEnabled: Boolean,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onCreateBackupClick: (password: String) -> Unit,
    onRestoreBackupTrigger: () -> Unit,
    onWipeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = AppStrings.get("settings", language),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
        }

        // Language & Appearance Group
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferences & Appearance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Item
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = AppStrings.get("language", language),
                        subtitle = "${language.displayName} (${language.nativeName})",
                        onClick = { showLanguageDialog = true },
                        testTag = "setting_language"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Theme Item
                    SettingsRowItem(
                        icon = Icons.Default.Palette,
                        title = AppStrings.get("theme", language),
                        subtitle = when (themeMode) {
                            "LIGHT" -> AppStrings.get("theme_light", language)
                            "DARK" -> AppStrings.get("theme_dark", language)
                            else -> AppStrings.get("theme_system", language)
                        },
                        onClick = { showThemeDialog = true },
                        testTag = "setting_theme"
                    )
                }
            }
        }

        // Security & Privacy Group
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("security_privacy", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // App Lock Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = AppStrings.get("app_lock_pin", language),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (isAppLockEnabled) "Enabled • Requires 4-digit PIN" else "Disabled • Tap to configure PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    showSetPinDialog = true
                                } else {
                                    onDisablePin()
                                }
                            },
                            modifier = Modifier.testTag("switch_app_lock")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Privacy Policy
                    SettingsRowItem(
                        icon = Icons.Default.PrivacyTip,
                        title = AppStrings.get("privacy_policy", language),
                        subtitle = "100% Offline • Zero Tracking • Device-Only Storage",
                        onClick = { showPrivacyPolicyDialog = true },
                        testTag = "setting_privacy_policy"
                    )
                }
            }
        }

        // Encrypted Backup & Restore Group
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Backup & Recovery",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Encrypted using AES-256-GCM with your personal password. Safe to store anywhere.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Upload,
                        title = AppStrings.get("backup_encrypted", language),
                        subtitle = "Export password-protected .ptbackup file",
                        onClick = { showBackupPasswordDialog = true },
                        testTag = "setting_backup"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Download,
                        title = AppStrings.get("restore_backup", language),
                        subtitle = "Decrypt and restore data from a backup file",
                        onClick = onRestoreBackupTrigger,
                        testTag = "setting_restore"
                    )
                }
            }
        }

        // Danger Zone: Clear all data
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaisaExpenseRed
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = AppStrings.get("clear_all_data", language),
                        subtitle = "Permanently delete all transactions, budgets, and records",
                        onClick = { showWipeConfirmDialog = true },
                        tint = PaisaExpenseRed,
                        testTag = "setting_wipe_data"
                    )
                }
            }
        }

        // App Version Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PaisaTrack — Offline Expense Manager",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Version 1.0.0 • Made for India 🇮🇳",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Language Picker Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(AppStrings.get("language", language)) },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onLanguageChange(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == lang,
                                onClick = {
                                    onLanguageChange(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${lang.displayName} • ${lang.nativeName}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(AppStrings.get("theme", language)) },
            text = {
                Column {
                    listOf("SYSTEM" to AppStrings.get("theme_system", language),
                        "LIGHT" to AppStrings.get("theme_light", language),
                        "DARK" to AppStrings.get("theme_dark", language)).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = {
                                    onThemeModeChange(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Configure Security PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a 4-digit PIN to lock the app whenever you close it:")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                        label = { Text("4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_setup_pin")
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmPinInput = it },
                        label = { Text("Confirm 4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_setup_pin_confirm")
                    )

                    errorMsg?.let { Text(it, color = PaisaExpenseRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length != 4) {
                            errorMsg = "PIN must be exactly 4 digits."
                            return@Button
                        }
                        if (pinInput != confirmPinInput) {
                            errorMsg = "PINs do not match."
                            return@Button
                        }
                        onSetPin(pinInput)
                        showSetPinDialog = false
                    },
                    modifier = Modifier.testTag("btn_save_pin")
                ) {
                    Text(AppStrings.get("save", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Create Backup Password Dialog
    if (showBackupPasswordDialog) {
        var backupPassword by remember { mutableStateOf("") }
        var backupPasswordConfirm by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showBackupPasswordDialog = false },
            title = { Text(AppStrings.get("backup_encrypted", language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Set an encryption password for this backup file. You will need this password to restore your data.")
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Backup Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_backup_pwd")
                    )
                    OutlinedTextField(
                        value = backupPasswordConfirm,
                        onValueChange = { backupPasswordConfirm = it },
                        label = { Text("Confirm Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_backup_pwd_confirm")
                    )
                    errorMsg?.let { Text(it, color = PaisaExpenseRed, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupPassword.length < 4) {
                            errorMsg = "Password must be at least 4 characters."
                            return@Button
                        }
                        if (backupPassword != backupPasswordConfirm) {
                            errorMsg = "Passwords do not match."
                            return@Button
                        }
                        onCreateBackupClick(backupPassword)
                        showBackupPasswordDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_export_backup")
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupPasswordDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacy Policy — PaisaTrack") },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Completely Offline Architecture:\n" +
                                "PaisaTrack operates 100% on your device. There is no cloud database, remote sync server, or telemetry backend.",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "2. Zero Tracking & No Data Harvesting:\n" +
                                "We do not collect personal identifiers, bank credentials, device location, contacts, or financial activity.",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "3. Strong Local Cryptography:\n" +
                                "PIN credentials and exported backup files are protected using industry-standard PBKDF2 key derivation and AES-256-GCM encryption.",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "4. User-Controlled Data & Backups:\n" +
                                "Your financial records stay on your phone. You can create encrypted backups and export CSV/PDF reports via Android's Storage Access Framework anytime.",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Wipe Data Confirmation Dialog
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Erase All Data?", color = PaisaExpenseRed) },
            text = {
                Text("This will permanently delete all your transactions, custom categories, budgets, debt records, and settings. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeAllData()
                        showWipeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PaisaExpenseRed),
                    modifier = Modifier.testTag("btn_confirm_wipe_all")
                ) {
                    Text("Permanently Erase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
