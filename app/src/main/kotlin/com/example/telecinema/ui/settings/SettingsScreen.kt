package com.example.telecinema.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecinema.data.local.AppPreferences
import com.example.telecinema.theme.getThemeAccent
import com.example.telecinema.util.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    locale: String,
    onNavigateToStats: () -> Unit
) {
    val context = LocalContext.current
    val dataVersion by AppPreferences.dataVersion.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val themes = listOf(
        "gold" to "ذهبي",
        "blue" to "أزرق",
        "green" to "أخضر",
        "purple" to "بنفسجي",
        "red" to "أحمر"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = Lang.t("settings", locale),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Language Section
            SettingsHeader(title = Lang.t("language", locale))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = AppPreferences.locale == "ar",
                    onClick = { AppPreferences.locale = "ar" },
                    label = { Text(Lang.t("arabic", locale)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = AppPreferences.locale == "en",
                    onClick = { AppPreferences.locale = "en" },
                    label = { Text(Lang.t("english", locale)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Appearance & Accent Themes
            SettingsHeader(title = Lang.t("appearance", locale))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(themes) { (themeKey, name) ->
                    val isSelected = AppPreferences.theme == themeKey
                    val color = getThemeAccent(themeKey)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { AppPreferences.theme = themeKey }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. View & Display Settings
            SettingsHeader(title = Lang.t("viewMode", locale))
            SwitchPreference(
                title = Lang.t("list", locale),
                subtitle = "عرض الأفلام كقائمة تفصيلية",
                checked = AppPreferences.viewMode == "list",
                onCheckedChange = { AppPreferences.viewMode = if (it) "list" else "grid" }
            )
            SwitchPreference(
                title = Lang.t("hideWatched", locale),
                subtitle = "إخفاء الأفلام التي تمت مشاهدتها من الرئيسية",
                checked = AppPreferences.hideWatched,
                onCheckedChange = { AppPreferences.hideWatched = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Privacy & Smart Modes
            SettingsHeader(title = "الخصوصية والأوضاع الذكية 🛡️")
            SwitchPreference(
                title = Lang.t("incognito", locale),
                subtitle = Lang.t("incognitoHint", locale),
                checked = AppPreferences.incognito,
                onCheckedChange = { AppPreferences.incognito = it }
            )
            SwitchPreference(
                title = Lang.t("kidsMode", locale),
                subtitle = Lang.t("kidsModeHint", locale),
                checked = AppPreferences.kidsMode,
                onCheckedChange = { AppPreferences.kidsMode = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Secret Vault
            SettingsHeader(title = Lang.t("vault", locale))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(if (AppPreferences.vaultPin.isEmpty()) Lang.t("setPin", locale) else "تغيير رمز PIN") },
                        leadingContent = { Icon(Icons.Default.Pin, contentDescription = null) },
                        modifier = Modifier.clickable {
                            pinInput = ""
                            showPinDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Statistics & Taste
            SettingsHeader(title = Lang.t("stats", locale))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text(Lang.t("stats", locale)) },
                    supportingContent = { Text("ساعات المشاهدة والذوق السينمائي") },
                    leadingContent = { Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToStats() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Backup & Restore
            SettingsHeader(title = Lang.t("backup", locale))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(Lang.t("exportData", locale)) },
                        supportingContent = { Text("نسخ القنوات والمفضلة كبيانات JSON") },
                        leadingContent = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val json = AppPreferences.exportAllJson()
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("TeleCinema Backup", json)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ النسخة الاحتياطية إلى الحافظة", Toast.LENGTH_LONG).show()
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                    ListItem(
                        headlineContent = { Text(Lang.t("importData", locale)) },
                        supportingContent = { Text("استعادة البيانات من نص النسخة الاحتياطية") },
                        leadingContent = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                        modifier = Modifier.clickable {
                            importJsonText = ""
                            showImportDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Set PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(Lang.t("setPin", locale)) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    placeholder = { Text("رمز أرقام (4-6 أرقام)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            AppPreferences.vaultPin = pinInput
                            showPinDialog = false
                            Toast.makeText(context, "تم حفظ رمز القبو", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(Lang.t("cancel", locale))
                }
            }
        )
    }

    // Import Backup Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(Lang.t("importData", locale)) },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    placeholder = { Text("الصق كود النسخة الاحتياطية هنا...") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = AppPreferences.importAllJson(importJsonText.trim())
                        if (success) {
                            Toast.makeText(context, Lang.t("importSuccess", locale), Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "فشل استيراد البيانات، تأكد من صحة النص", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text(Lang.t("importData", locale))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(Lang.t("cancel", locale))
                }
            }
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}
