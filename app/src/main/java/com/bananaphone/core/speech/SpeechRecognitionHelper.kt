package com.bananaphone.core.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

private const val TAG = "SpeechRecognitionHelper"

/**
 * Callback interface for speech recognition events
 */
interface SpeechRecognitionCallback {
    fun onSpeechResult(text: String)
    fun onSpeechError(error: Int, message: String)
    fun onSpeechStart()
    fun onSpeechEnd()
}

/**
 * Helper class for handling Android Speech Recognition
 */
class SpeechRecognitionHelper(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: SpeechRecognitionCallback? = null
    
    /**
     * Check if RECORD_AUDIO permission is granted
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start listening for speech input
     * 
     * @param callback Callback to receive recognition events
     */
    fun startListening(callback: SpeechRecognitionCallback) {
        if (!hasPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            callback.onSpeechError(
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                "Microphone permission required"
            )
            return
        }
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            callback.onSpeechError(
                SpeechRecognizer.ERROR_CLIENT,
                "Speech recognition not available"
            )
            return
        }
        
        this.callback = callback
        
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            
            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "Started listening for speech")
            callback.onSpeechStart()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            callback.onSpeechError(SpeechRecognizer.ERROR_CLIENT, e.message ?: "Unknown error")
        }
    }
    
    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        Log.d(TAG, "Stopped listening for speech")
    }
    
    /**
     * Cancel speech recognition
     */
    fun cancel() {
        speechRecognizer?.cancel()
        Log.d(TAG, "Cancelled speech recognition")
    }
    
    /**
     * Release resources
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        callback = null
        Log.d(TAG, "Released speech recognition resources")
    }
    
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech detected")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changes - can be used for visual feedback
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech detected")
                callback?.onSpeechEnd()
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error: $error"
                }
                
                Log.e(TAG, "Speech recognition error: $errorMessage")
                callback?.onSpeechError(error, errorMessage)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "Speech recognition result: $text")
                    callback?.onSpeechResult(text)
                } else {
                    Log.w(TAG, "No results from speech recognition")
                    callback?.onSpeechError(SpeechRecognizer.ERROR_NO_MATCH, "No speech detected")
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Partial results - can be used for real-time feedback
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events
            }
        }
    }
}

