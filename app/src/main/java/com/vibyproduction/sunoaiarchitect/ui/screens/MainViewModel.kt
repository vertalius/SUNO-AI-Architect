package com.vibyproduction.sunoaiarchitect.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibyproduction.sunoaiarchitect.data.GeminiRepository
import com.vibyproduction.sunoaiarchitect.data.SettingsRepository
import com.vibyproduction.sunoaiarchitect.domain.AppSettings
import com.vibyproduction.sunoaiarchitect.domain.CleanBlockResult
import com.vibyproduction.sunoaiarchitect.domain.GenerationMode
import com.vibyproduction.sunoaiarchitect.domain.UiState
import com.vibyproduction.sunoaiarchitect.util.AudioExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Main ViewModel for SUNO AI Architect
 * Version: 0.1 alpha
 * Developer: ViBy Production
 * Author: Vitalii Bychkov
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val geminiRepo = GeminiRepository()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _idea = MutableStateFlow("")
    val idea: StateFlow<String> = _idea.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _selectedMode = MutableStateFlow(GenerationMode.FREE)
    val selectedMode: StateFlow<GenerationMode> = _selectedMode.asStateFlow()

    private val _lastResult = MutableStateFlow<CleanBlockResult?>(null)
    val lastResult: StateFlow<CleanBlockResult?> = _lastResult.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { _settings.value = it }
        }
    }

    fun updateIdea(text: String) { _idea.value = text }
    fun updateUrl(text: String) { _url.value = text }
    fun selectMode(mode: GenerationMode) { _selectedMode.value = mode }

    fun saveSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepo.saveSettings(newSettings)
        }
    }

    fun generateFromText() {
        viewModelScope.launch {
            val s = _settings.value
            if (s.geminiApiKey.isBlank()) {
                _uiState.value = UiState.Error("Укажите Gemini API Key в настройках")
                return@launch
            }
            if (_idea.value.isBlank()) {
                _uiState.value = UiState.Error("Введите описание идеи")
                return@launch
            }

            _uiState.value = UiState.Generating
            val result = geminiRepo.generateFromText(
                apiKey = s.geminiApiKey,
                model = s.selectedModel,
                userIdea = _idea.value,
                optionalUrl = _url.value.ifBlank { null },
                mode = _selectedMode.value,
                sunoVersion = s.sunoVersion,
                maxMode = s.maxModeDefault || _selectedMode.value == GenerationMode.MAX_MODE
            )

            result.fold(
                onSuccess = {
                    _lastResult.value = it
                    _uiState.value = UiState.Success(it)
                },
                onFailure = {
                    _uiState.value = UiState.Error(it.message ?: "Ошибка генерации")
                }
            )
        }
    }

    fun generateFromFile(uri: Uri, mimeType: String?) {
        viewModelScope.launch {
            val s = _settings.value
            if (s.geminiApiKey.isBlank()) {
                _uiState.value = UiState.Error("Укажите Gemini API Key в настройках")
                return@launch
            }

            _uiState.value = UiState.ExtractingAudio
            val audioFile = AudioExtractor.extractAudioIfNeeded(getApplication(), uri, mimeType)
            if (audioFile == null) {
                _uiState.value = UiState.Error("Не удалось обработать файл")
                return@launch
            }

            _uiState.value = UiState.Analyzing
            val result = geminiRepo.generateFromAudio(
                apiKey = s.geminiApiKey,
                model = s.selectedModel,
                audioFile = audioFile,
                userIdea = _idea.value.ifBlank { null },
                optionalUrl = _url.value.ifBlank { null },
                mode = _selectedMode.value,
                sunoVersion = s.sunoVersion,
                maxMode = s.maxModeDefault || _selectedMode.value == GenerationMode.MAX_MODE
            )

            // Clean up temp file
            try { audioFile.delete() } catch (_: Exception) {}

            result.fold(
                onSuccess = {
                    _lastResult.value = it
                    _uiState.value = UiState.Success(it)
                },
                onFailure = {
                    _uiState.value = UiState.Error(it.message ?: "Ошибка анализа аудио")
                }
            )
        }
    }

    fun clearResult() {
        _lastResult.value = null
        _uiState.value = UiState.Idle
    }

    fun resetToIdle() {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.Idle
        }
    }
}
