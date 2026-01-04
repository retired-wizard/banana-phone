package com.bananaphone.core.llm

/**
 * PromptTemplate - THIS IS THE MAIN ITERATION POINT
 * 
 * This file is designed for frequent iteration. Modify the instructions here
 * to improve HTML generation quality.
 * 
 * Key areas for iteration:
 * 1. System Instructions - How the LLM should interpret requests
 * 2. Format Requirements - HTML structure, CSS/JS placement
 * 3. Examples - Optional examples to guide the LLM
 * 4. Constraints - What NOT to do, limitations
 * 5. Tone/Style - UI design guidance, creativity level
 */
object PromptTemplate {
    
    /**
     * Creates the prompt sent to the LLM.
     * 
     * @param userRequest The transcribed speech input from the user
     * @return Pair of (systemMessage, userMessage) strings
     */
    fun createPrompt(userRequest: String): Pair<String, String> {
        val systemMessage = """
            You are an HTML generator for a mobile app interface. Your task is to generate standalone, complete HTML that implements the user's request.
            
            REQUIREMENTS:
            - Generate complete, standalone HTML (include <!DOCTYPE html>, <html>, <head>, <body> tags)
            - Include all CSS inline in <style> tags within the <head>
            - Include all JavaScript inline in <script> tags (can be in <head> or before </body>)
            - NO external dependencies (no CDN links, no external files, no external APIs)
            - HTML should be complete and functional when loaded in a WebView
            - Mobile-friendly design: touch targets at least 48dp, responsive viewport, works on small screens
            - Make the interface intuitive and functional
            
            INTERPRETATION:
            - Interpret user requests creatively - infer functionality from natural language
            - If the user asks for something vague, create a reasonable implementation
            - Make the interface visually appealing and modern
            - Use appropriate HTML5 elements and semantic markup
            
            OUTPUT:
            - Return ONLY the HTML code, nothing else
            - Do not include markdown code blocks (no ```html or ```)
            - Do not include explanations or comments outside the HTML
        """.trimIndent()
        
        val userMessage = "Generate HTML that allows the user to: $userRequest"
        
        return Pair(systemMessage, userMessage)
    }
}

