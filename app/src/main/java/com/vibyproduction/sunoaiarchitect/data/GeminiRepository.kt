package com.vibyproduction.sunoaiarchitect.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.vibyproduction.sunoaiarchitect.domain.CleanBlockResult
import com.vibyproduction.sunoaiarchitect.domain.GeminiModel
import com.vibyproduction.sunoaiarchitect.domain.GenerationMode
import com.vibyproduction.sunoaiarchitect.domain.SunoVersion
import com.vibyproduction.sunoaiarchitect.domain.TrackMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Gemini integration for SUNO AI Architect
 * Version: 0.1 alpha
 * Developer: ViBy Production
 * Author: Vitalii Bychkov
 *
 * Supported models: Gemini 3.6 Lite / Gemini 3.1 Pro
 */
class GeminiRepository {

    companion object {
        private const val TAG = "GeminiRepository"
    }

    private fun buildSystemPrompt(
        mode: GenerationMode,
        sunoVersion: SunoVersion,
        maxMode: Boolean
    ): String {
        return """
You are Suno AI Architect v2.0 — an elite music producer and prompt engineer optimized for advanced multimodal models.
Your sole task is to transform abstract user ideas (and/or audio references) into precise Semantic Narratives (Style) and Structural Code (Lyrics) for Suno AI.

APP CONTEXT:
- Application: SUNO AI Architect v0.1 alpha
- Developer: ViBy Production
- Author: Vitalii Bychkov

CORE RULES:
- Design sound, not just text.
- Always follow the Clean Block Protocol for output.
- Prefer technical descriptors over vague emotions.
- Use positive framing. Avoid negative prompts for vocals.
- Target Suno version: ${sunoVersion.displayName}
- MAX MODE: ${if (maxMode) "ENABLED" else "optional"}
- Current generation mode: ${mode.title} — ${mode.description}

OUTPUT FORMAT — Clean Block Protocol (MANDATORY):
Every response must contain exactly these three parts:

### 1. Analysis (plain text)
- Brief rationale for settings
- Recommended sliders (Weirdness / Style Influence / Strength / Lyrics Strength)
- Target version justification
- Techniques used
- If audio was provided: full recognized metadata (artist, title, lyrics if present, BPM, key, genre, mood, instruments, duration) + Acoustic Deconstruction

### 2. Style Block (code block only)
- Content for the "Style of Music" field only
- Include MAX MODE tags at the top when used:
  [Is_MAX_MODE: MAX](MAX)
  [QUALITY: MAX](MAX)
  [REALISM: MAX](MAX)
  [REAL_INSTRUMENTS: MAX](MAX)
- No extra headers inside the code block
- First 30 tokens have maximum weight — put critical descriptors first and repeat key ones at the end

### 3. Lyrics Block (code block only)
- Content for the "Lyrics" field only
- Start with ///*****/// when using MAX MODE or when lyrics follow
- Include all structural tags, chords, ad-libs
- For instrumentals use [Instrumental]

WHEN AUDIO/VIDEO REFERENCE IS PROVIDED:
1. Attempt to identify the track (artist, title, album).
2. Transcribe lyrics if present (or note instrumental).
3. Estimate BPM, musical key, genre, mood, main instruments.
4. Perform Acoustic Deconstruction:
   - Timbre (Dark/Bright, Warm/Metallic, spectral characteristics)
   - Dynamics (ADSR, compression, transients)
   - Groove (swing, pocket, syncopation, density)
   - Spatial (depth, panning, reverb space, stereo field)
5. Translate into technical descriptors.
6. Create a copyright-safe style clone (vibe + production, NOT the exact melody).

KEY RULES:
- Character Persona > generic "sad female vocal". Use 3–7 rich contextual adjectives + scene.
- Positive framing only for vocals.
- Hybrid genres require a bridge genre.
- Negative prompts: max 1–2 instrumental exclusions. Never for gender/vocals.
- Avoid artifact-trigger words: artifact, glitches, clipping, background noise, shimmer, hiss.
- Prefer short test generations (15–30 s) before full tracks.

Respond ONLY with the three Clean Block sections. No extra commentary outside them.
""".trimIndent()
    }

    suspend fun generateFromText(
        apiKey: String,
        model: GeminiModel,
        userIdea: String,
        optionalUrl: String?,
        mode: GenerationMode,
        sunoVersion: SunoVersion,
        maxMode: Boolean
    ): Result<CleanBlockResult> = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = model.apiName,
                apiKey = apiKey,
                systemInstruction = content { text(buildSystemPrompt(mode, sunoVersion, maxMode)) }
            )

            val prompt = buildString {
                appendLine("USER REQUEST:")
                appendLine(userIdea)
                if (!optionalUrl.isNullOrBlank()) {
                    appendLine()
                    appendLine("OPTIONAL REFERENCE URL (use for metadata if possible):")
                    appendLine(optionalUrl)
                }
                appendLine()
                appendLine("Generate the Clean Block output now.")
            }

            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return@withContext Result.failure(Exception("Empty response from Gemini"))

            Result.success(parseCleanBlock(text))
        } catch (e: Exception) {
            Log.e(TAG, "generateFromText failed", e)
            Result.failure(e)
        }
    }

    suspend fun generateFromAudio(
        apiKey: String,
        model: GeminiModel,
        audioFile: File,
        userIdea: String?,
        optionalUrl: String?,
        mode: GenerationMode,
        sunoVersion: SunoVersion,
        maxMode: Boolean
    ): Result<CleanBlockResult> = withContext(Dispatchers.IO) {
        try {
            val generativeModel = GenerativeModel(
                modelName = model.apiName,
                apiKey = apiKey,
                systemInstruction = content { text(buildSystemPrompt(mode, sunoVersion, maxMode)) }
            )

            val audioBytes = audioFile.readBytes()
            val mime = when {
                audioFile.name.endsWith(".mp3", true) -> "audio/mp3"
                audioFile.name.endsWith(".wav", true) -> "audio/wav"
                audioFile.name.endsWith(".m4a", true) -> "audio/mp4"
                audioFile.name.endsWith(".ogg", true) -> "audio/ogg"
                audioFile.name.endsWith(".flac", true) -> "audio/flac"
                else -> "audio/mp4"
            }

            val promptText = buildString {
                appendLine("AUDIO REFERENCE PROVIDED (analyze the attached audio).")
                appendLine("File size: ${audioBytes.size} bytes, mime: $mime")
                if (!userIdea.isNullOrBlank()) {
                    appendLine()
                    appendLine("ADDITIONAL USER INSTRUCTIONS:")
                    appendLine(userIdea)
                }
                if (!optionalUrl.isNullOrBlank()) {
                    appendLine()
                    appendLine("OPTIONAL URL for extra metadata:")
                    appendLine(optionalUrl)
                }
                appendLine()
                appendLine("Perform full recognition + Acoustic Deconstruction + generate Clean Block.")
            }

            val response = try {
                generativeModel.generateContent(
                    content {
                        text(promptText)
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "Multimodal attempt failed, falling back to text", e)
                generativeModel.generateContent(promptText)
            }

            val text = response.text ?: return@withContext Result.failure(Exception("Empty response from Gemini"))
            Result.success(parseCleanBlock(text))
        } catch (e: Exception) {
            Log.e(TAG, "generateFromAudio failed", e)
            Result.failure(e)
        }
    }

    private fun parseCleanBlock(raw: String): CleanBlockResult {
        val analysis = extractSection(raw, "### 1. Analysis", "### 2. Style Block")
            ?: extractSection(raw, "Analysis", "Style Block")
            ?: raw.take(800)

        val style = extractCodeBlockAfter(raw, "Style Block")
            ?: extractCodeBlockAfter(raw, "Style of Music")
            ?: ""

        val lyrics = extractCodeBlockAfter(raw, "Lyrics Block")
            ?: extractCodeBlockAfter(raw, "Lyrics")
            ?: ""

        return CleanBlockResult(
            analysis = analysis.trim(),
            styleBlock = style.trim(),
            lyricsBlock = lyrics.trim(),
            metadata = null,
            recommendedSliders = null
        )
    }

    private fun extractSection(text: String, startMarker: String, endMarker: String): String? {
        val start = text.indexOf(startMarker, ignoreCase = true)
        if (start < 0) return null
        val from = start + startMarker.length
        val end = text.indexOf(endMarker, fromIndex = from, ignoreCase = true)
        return if (end > from) text.substring(from, end) else text.substring(from)
    }

    private fun extractCodeBlockAfter(text: String, marker: String): String? {
        val idx = text.indexOf(marker, ignoreCase = true)
        if (idx < 0) return null
        val after = text.substring(idx + marker.length)
        val codeStart = after.indexOf("```")
        if (codeStart < 0) return after.trim().take(1500)
        val contentStart = codeStart + 3
        val firstNewline = after.indexOf('\n', contentStart)
        val realStart = if (firstNewline > contentStart && firstNewline - contentStart < 20) firstNewline + 1 else contentStart
        val codeEnd = after.indexOf("```", realStart)
        return if (codeEnd > realStart) after.substring(realStart, codeEnd) else after.substring(realStart)
    }
}
