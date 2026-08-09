package com.vibyproduction.sunoaiarchitect.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibyproduction.sunoaiarchitect.R
import com.vibyproduction.sunoaiarchitect.domain.GenerationMode
import com.vibyproduction.sunoaiarchitect.domain.UiState
import com.vibyproduction.sunoaiarchitect.ui.theme.SunoPrimary
import com.vibyproduction.sunoaiarchitect.ui.theme.SunoSecondary

/**
 * Main screen of SUNO AI Architect
 * Version: 0.1 alpha | Developer: ViBy Production | Author: Vitalii Bychkov
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val idea by viewModel.idea.collectAsState()
    val url by viewModel.url.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val mime = context.contentResolver.getType(it)
            viewModel.generateFromFile(it, mime)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v${stringResource(R.string.app_version)} • ${stringResource(R.string.developer)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(8.dp))

            Text("Режим генерации", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenerationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        label = { Text(mode.title) },
                        leadingIcon = if (selectedMode == mode) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = idea,
                onValueChange = viewModel::updateIdea,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                label = { Text(stringResource(R.string.hint_idea)) },
                placeholder = { Text("Например: dark cinematic trap with emotional female vocal…") },
                maxLines = 6,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = url,
                onValueChange = viewModel::updateUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.hint_url)) },
                leadingIcon = { Icon(Icons.Default.Link, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.generateFromText() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is UiState.Generating &&
                            uiState !is UiState.Analyzing &&
                            uiState !is UiState.ExtractingAudio
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_generate))
                }

                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("audio/*", "video/*")) },
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is UiState.Generating &&
                            uiState !is UiState.Analyzing &&
                            uiState !is UiState.ExtractingAudio
                ) {
                    Icon(Icons.Default.AudioFile, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Аудио/Видео")
                }
            }

            Spacer(Modifier.height(20.dp))

            when (val state = uiState) {
                is UiState.ExtractingAudio -> StatusCard("Извлечение аудио из видео…", true)
                is UiState.Analyzing -> StatusCard("Анализ аудио + распознавание…", true)
                is UiState.Generating -> StatusCard("Генерация Clean Block…", true)
                is UiState.Error -> {
                    StatusCard(state.message, false, isError = true)
                    TextButton(onClick = { viewModel.resetToIdle() }) {
                        Text("Закрыть")
                    }
                }
                else -> {}
            }

            lastResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                ResultSection(
                    result = result,
                    onCopyStyle = { copyToClipboard(context, result.styleBlock, "Style") },
                    onCopyLyrics = { copyToClipboard(context, result.lyricsBlock, "Lyrics") },
                    onCopyAll = {
                        val all = "=== ANALYSIS ===\n${result.analysis}\n\n=== STYLE ===\n${result.styleBlock}\n\n=== LYRICS ===\n${result.lyricsBlock}"
                        copyToClipboard(context, all, "Всё")
                    },
                    onClear = { viewModel.clearResult() }
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "© ${stringResource(R.string.developer)} • ${stringResource(R.string.author)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showSettings) {
        SettingsDialog(
            current = settings,
            onDismiss = { showSettings = false },
            onSave = {
                viewModel.saveSettings(it)
                showSettings = false
            }
        )
    }
}

@Composable
private fun StatusCard(text: String, loading: Boolean, isError: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
            }
            Text(text)
        }
    }
}

@Composable
private fun ResultSection(
    result: com.vibyproduction.sunoaiarchitect.domain.CleanBlockResult,
    onCopyStyle: () -> Unit,
    onCopyLyrics: () -> Unit,
    onCopyAll: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Результат", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClear) {
                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Очистить")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Analysis", style = MaterialTheme.typography.labelLarge, color = SunoSecondary)
                Spacer(Modifier.height(6.dp))
                Text(result.analysis, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Style Block", style = MaterialTheme.typography.labelLarge, color = SunoPrimary)
                    IconButton(onClick = onCopyStyle) {
                        Icon(Icons.Default.ContentCopy, "Copy Style")
                    }
                }
                Text(
                    text = result.styleBlock,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lyrics Block", style = MaterialTheme.typography.labelLarge, color = SunoPrimary)
                    IconButton(onClick = onCopyLyrics) {
                        Icon(Icons.Default.ContentCopy, "Copy Lyrics")
                    }
                }
                Text(
                    text = result.lyricsBlock,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        }

        Button(onClick = onCopyAll, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ContentCopy, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_copy_all))
        }
    }
}

@Composable
private fun SettingsDialog(
    current: com.vibyproduction.sunoaiarchitect.domain.AppSettings,
    onDismiss: () -> Unit,
    onSave: (com.vibyproduction.sunoaiarchitect.domain.AppSettings) -> Unit
) {
    var apiKey by remember { mutableStateOf(current.geminiApiKey) }
    var model by remember { mutableStateOf(current.selectedModel) }
    var sunoVersion by remember { mutableStateOf(current.sunoVersion) }
    var maxMode by remember { mutableStateOf(current.maxModeDefault) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.settings_api_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Модель Gemini", style = MaterialTheme.typography.labelLarge)
                com.vibyproduction.sunoaiarchitect.domain.GeminiModel.entries.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = model == m, onClick = { model = m })
                        Text(m.displayName)
                    }
                }

                Text("Версия Suno", style = MaterialTheme.typography.labelLarge)
                com.vibyproduction.sunoaiarchitect.domain.SunoVersion.entries.forEach { v ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sunoVersion == v, onClick = { sunoVersion = v })
                        Text(v.displayName)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = maxMode, onCheckedChange = { maxMode = it })
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_max_mode_default))
                }

                HorizontalDivider()
                Text(
                    text = stringResource(R.string.about_text),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    current.copy(
                        geminiApiKey = apiKey.trim(),
                        selectedModel = model,
                        sunoVersion = sunoVersion,
                        maxModeDefault = maxMode
                    )
                )
            }) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label скопировано", Toast.LENGTH_SHORT).show()
}
