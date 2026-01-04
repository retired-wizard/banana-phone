package com.bananaphone.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bananaphone.R
import com.bananaphone.core.llm.OpenRouterClient
import com.bananaphone.core.speech.SpeechRecognitionCallback
import com.bananaphone.core.speech.SpeechRecognitionHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var speechHelper: SpeechRecognitionHelper
    private lateinit var voiceButton: FloatingActionButton
    private lateinit var listeningIndicator: View
    private lateinit var pulsingCircle: View
    private var pulseAnimator: ObjectAnimator? = null
    private val openRouterClient = OpenRouterClient()
    private val androidInterface = AndroidInterface()
    private var isListening = false
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Microphone permission granted")
        } else {
            Log.w(TAG, "Microphone permission denied")
            Toast.makeText(
                this,
                "Microphone permission is required to use voice input",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        voiceButton = findViewById(R.id.voiceButton)
        listeningIndicator = findViewById(R.id.listeningIndicator)
        pulsingCircle = findViewById(R.id.pulsingCircle)
        
        setupSpeechRecognition()
        setupWebView()
        setupVoiceButton()
        loadInitialUI()
        
        // Request microphone permission if not granted
        if (!speechHelper.hasPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
    private fun setupVoiceButton() {
        voiceButton.setOnClickListener {
            if (isListening) {
                // If already listening, cancel
                speechHelper.cancel()
                hideListeningIndicator()
            } else {
                // Start listening
                startListening()
            }
        }
    }
    
    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        
        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            allowFileAccess = false
            allowContentAccess = false
        }
        
        // Add JavaScript interface for microphone button
        webView.addJavascriptInterface(androidInterface, "AndroidInterface")
        
        // WebView client for error handling
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView error: ${error?.description}")
                showError("Failed to load content: ${error?.description}")
            }
        }
    }
    
    private fun setupSpeechRecognition() {
        speechHelper = SpeechRecognitionHelper(this)
    }
    
    private fun loadInitialUI() {
        try {
            val inputStream = resources.openRawResource(R.raw.microphone_ui)
            val html = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
            
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading initial UI", e)
            showError("Failed to load initial UI")
        }
    }
    
    private fun showLoadingAnimation() {
        try {
            val inputStream = resources.openRawResource(R.raw.loading_animation)
            var html = BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
            
            // Load banana icon and convert to base64
            try {
                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.banana_icon)
                
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64Icon = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                val dataUri = "data:image/png;base64,$base64Icon"
                
                // Replace placeholder with actual icon
                html = html.replace("BANANA_ICON_PLACEHOLDER", dataUri)
            } catch (e: Exception) {
                Log.w(TAG, "Could not load banana icon for loading animation", e)
                // Fallback: remove the img tag if icon loading fails
                html = html.replace("<img class=\"banana\" src=\"BANANA_ICON_PLACEHOLDER\" alt=\"Loading...\" />", "")
            }
            
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading loading animation", e)
        }
    }
    
    private fun startListening() {
        if (!speechHelper.hasPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        isListening = true
        showListeningIndicator()
        updateButtonState()
        
        speechHelper.startListening(object : SpeechRecognitionCallback {
            override fun onSpeechStart() {
                Log.d(TAG, "Speech recognition started")
            }
            
            override fun onSpeechBeginning() {
                Log.d(TAG, "User started speaking")
                // Hide indicator when user starts speaking
                hideListeningIndicator()
            }
            
            override fun onSpeechEnd() {
                Log.d(TAG, "Speech recognition ended")
                isListening = false
                hideListeningIndicator()
                updateButtonState()
            }
            
            override fun onSpeechResult(text: String) {
                Log.d(TAG, "Speech recognized: $text")
                isListening = false
                hideListeningIndicator()
                updateButtonState()
                processUserRequest(text)
            }
            
            override fun onSpeechError(error: Int, message: String) {
                Log.e(TAG, "Speech recognition error: $message")
                isListening = false
                hideListeningIndicator()
                updateButtonState()
                
                when (error) {
                    android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    android.speech.SpeechRecognizer.ERROR_NETWORK,
                    android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        Toast.makeText(this@MainActivity, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                    android.speech.SpeechRecognizer.ERROR_NO_MATCH,
                    android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Don't show toast for timeout - it's expected behavior
                        if (error != android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            Toast.makeText(this@MainActivity, "No speech detected. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        Toast.makeText(this@MainActivity, "Error: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    
    private fun showListeningIndicator() {
        listeningIndicator.visibility = View.VISIBLE
        startPulseAnimation()
    }
    
    private fun hideListeningIndicator() {
        listeningIndicator.visibility = View.GONE
        stopPulseAnimation()
    }
    
    private fun startPulseAnimation() {
        stopPulseAnimation() // Stop any existing animation
        
        pulseAnimator = ObjectAnimator.ofFloat(pulsingCircle, "scaleX", 1f, 1.5f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
        
        val scaleYAnimator = ObjectAnimator.ofFloat(pulsingCircle, "scaleY", 1f, 1.5f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
        
        val alphaAnimator = ObjectAnimator.ofFloat(pulsingCircle, "alpha", 0.6f, 0.2f, 0.6f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
        }
        
        pulseAnimator?.start()
        scaleYAnimator.start()
        alphaAnimator.start()
    }
    
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        pulsingCircle.scaleX = 1f
        pulsingCircle.scaleY = 1f
        pulsingCircle.alpha = 0.6f
    }
    
    private fun updateButtonState() {
        if (isListening) {
            voiceButton.setImageResource(android.R.drawable.ic_media_pause)
            voiceButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.voice_button_listening)
        } else {
            voiceButton.setImageResource(android.R.drawable.ic_btn_speak_now)
            voiceButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.voice_button_color)
        }
    }
    
    private fun processUserRequest(text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, "No speech detected", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoadingAnimation()
        
        lifecycleScope.launch {
            val result = openRouterClient.generateHTML(text)
            
            result.onSuccess { html ->
                // Load generated HTML in WebView (on main thread)
                runOnUiThread {
                    try {
                        // Clean up HTML - remove markdown code blocks if present
                        val cleanHtml = html
                            .removePrefix("```html")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()
                        
                        webView.loadDataWithBaseURL(null, cleanHtml, "text/html", "UTF-8", null)
                        Log.d(TAG, "Successfully loaded generated HTML")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading HTML into WebView", e)
                        showError("Failed to load generated content")
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Error generating HTML", error)
                runOnUiThread {
                    when {
                        error.message?.contains("API key") == true -> {
                            showError("API key not configured. Please set OPENROUTER_API_KEY in local.properties")
                        }
                        error.message?.contains("network", ignoreCase = true) == true -> {
                            showError("Network error. Please check your connection and try again.")
                        }
                        else -> {
                            showError("Failed to generate content: ${error.message}")
                        }
                    }
                }
            }
        }
    }
    
    private fun showError(message: String) {
        val errorHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: #f5f5f5;
                        padding: 20px;
                    }
                    .error {
                        background: white;
                        padding: 30px;
                        border-radius: 10px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        text-align: center;
                        max-width: 400px;
                    }
                    .error-title {
                        color: #d32f2f;
                        font-size: 24px;
                        margin-bottom: 15px;
                    }
                    .error-message {
                        color: #666;
                        margin-bottom: 20px;
                        line-height: 1.5;
                    }
                    button {
                        background: #FFC107;
                        color: #333;
                        border: none;
                        padding: 12px 24px;
                        border-radius: 5px;
                        font-size: 16px;
                        cursor: pointer;
                    }
                    button:active {
                        opacity: 0.8;
                    }
                </style>
            </head>
            <body>
                <div class="error">
                    <div class="error-title">⚠️ Error</div>
                    <div class="error-message">$message</div>
                    <button onclick="location.reload()">Try Again</button>
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
    }
    
    override fun onPause() {
        super.onPause()
        webView.onPause()
        speechHelper.cancel()
        isListening = false
        hideListeningIndicator()
        updateButtonState()
    }
    
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechHelper.destroy()
        webView.destroy()
    }
    
    // JavaScript interface for microphone button (called from HTML)
    inner class AndroidInterface {
        @android.webkit.JavascriptInterface
        fun startListening() {
            runOnUiThread {
                this@MainActivity.startListening()
            }
        }
    }
    
    init {
        // Add JavaScript interface after WebView is created
        // This will be called in setupWebView after webView is initialized
    }
}

