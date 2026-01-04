package com.bananaphone.bridge

/**
 * JavaScript Bridge Interface (Planned for v2)
 * 
 * This file defines the structure for JavaScript ↔ Native communication.
 * 
 * In v2, this will be implemented to allow generated HTML to call native Android features
 * such as:
 * - Location services
 * - Sensors (accelerometer, gyroscope)
 * - SMS/texting
 * - Camera
 * - File system access
 * - Contacts
 * - Notifications
 * - etc.
 * 
 * The bridge will use @JavascriptInterface annotation for WebView integration.
 * 
 * Example structure (for v2 implementation):
 * 
 * class JavaScriptBridge(private val context: Context) {
 *     @JavascriptInterface
 *     fun getLocation(): String { ... }
 *     
 *     @JavascriptInterface
 *     fun sendSMS(phoneNumber: String, message: String): Boolean { ... }
 *     
 *     // etc.
 * }
 */

// Placeholder - implementation deferred to v2

