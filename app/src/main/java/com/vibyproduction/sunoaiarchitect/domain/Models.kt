package com.vibyproduction.sunoaiarchitect.domain

/**
 * Domain models for SUNO AI Architect
 * Version: 0.1 alpha
 * Developer: ViBy Production
 * Author: Vitalii Bychkov
 */

enum class GeminiModel(val displayName: String, val apiName: String) {
    LITE("Gemini 3.6 Lite", "gemini-3.6-flash-lite"),   // placeholder – replace with actual model id when available
    PRO("Gemini 3.1 Pro", "gemini-3.1-pro")
}

enum class SunoVersion(val displayName: String) {
    V5("Suno v5 (High Fidelity)"),
    V45("Suno v4.5 (Creative/Rhythm)"),
    V4("Suno v4 / older")
}

enum class GenerationMode(val title: String, val description: String) {
    FREE("Свободный режим", "Опиши идею любым текстом"),
    STYLE_CLONING("Style Cloning", "Клонировать стиль референса"),
    QUALITY("Quality Improvement", "Максимальное качество звука"),
    HYBRID("Hybrid Genres", "Гибрид жанров"),
    VOICE("Advanced Voice", "Детальная персона вокала"),
    INSTRUMENTAL("Instrumental", "Чистый инструментал"),
    MAX_MODE("MAX MODE", "Максимальный реализм и чистота")
}

data class TrackMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationSec: Int? = null,
    val bpm: Int? = null,
    val key: String? = null,
    val genre: String? = null,
    val mood: String? = null,
    val lyrics: String? = null,
    val chords: String? = null,
    val instruments: List<String> = emptyList(),
    val rawDescription: String? = null
)

data class CleanBlockResult(
    val analysis: String,
    val styleBlock: String,
    val lyricsBlock: String,
    val metadata: TrackMetadata? = null,
    val recommendedSliders: String? = null
)

data class AppSettings(
    val geminiApiKey: String = "",
    val selectedModel: GeminiModel = GeminiModel.PRO,
    val sunoVersion: SunoVersion = SunoVersion.V5,
    val maxModeDefault: Boolean = true
)

sealed class UiState {
    object Idle : UiState()
    object ExtractingAudio : UiState()
    object Analyzing : UiState()
    object Generating : UiState()
    data class Success(val result: CleanBlockResult) : UiState()
    data class Error(val message: String) : UiState()
}
