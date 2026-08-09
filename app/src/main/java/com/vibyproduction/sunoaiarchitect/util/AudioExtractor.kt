package com.vibyproduction.sunoaiarchitect.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Extracts audio track from video files using MediaExtractor + MediaMuxer.
 * SUNO AI Architect v0.1 alpha | ViBy Production | Vitalii Bychkov
 */
object AudioExtractor {

    private const val TAG = "AudioExtractor"

    /**
     * Extracts the first audio track from a video (or returns the original file if already audio).
     * Returns a temporary .m4a file in cache dir, or null on failure.
     */
    suspend fun extractAudioIfNeeded(context: Context, uri: Uri, mimeType: String?): File? =
        withContext(Dispatchers.IO) {
            try {
                val isVideo = mimeType?.startsWith("video/") == true ||
                        uri.toString().lowercase().let {
                            it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".webm") ||
                                    it.endsWith(".mov") || it.endsWith(".avi")
                        }

                if (!isVideo) {
                    // Already audio – copy to cache for consistent handling
                    return@withContext copyToCache(context, uri, "source_audio")
                }

                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)

                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null

                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = format
                        break
                    }
                }

                if (audioTrackIndex < 0 || audioFormat == null) {
                    Log.e(TAG, "No audio track found")
                    extractor.release()
                    return@withContext null
                }

                extractor.selectTrack(audioTrackIndex)

                val outputFile = File(context.cacheDir, "extracted_audio_${System.currentTimeMillis()}.m4a")
                val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(audioFormat)
                muxer.start()

                val buffer = ByteBuffer.allocate(1024 * 1024)
                val bufferInfo = MediaCodec.BufferInfo()

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
                extractor.release()

                Log.i(TAG, "Audio extracted to ${outputFile.absolutePath}")
                outputFile
            } catch (e: Exception) {
                Log.e(TAG, "Extraction failed", e)
                null
            }
        }

    private fun copyToCache(context: Context, uri: Uri, prefix: String): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val outFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.tmp")
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
            input.close()
            outFile
        } catch (e: Exception) {
            Log.e(TAG, "Copy failed", e)
            null
        }
    }
}
