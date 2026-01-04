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
 * 
 * NOTE: Currently, user context is provided via the current HTML being rendered.
 * In the future, there will be a much more robust system for giving the LLM
 * information about the user context (e.g., user interactions, state, history,
 * screen position, selected elements, etc.).
 */
object PromptTemplate {
    
    /**
     * Creates the prompt sent to the LLM.
     * 
     * @param userRequest The transcribed speech input from the user
     * @param currentHtml Optional HTML that is currently being rendered in the WebView
     * @return Pair of (systemMessage, userMessage) strings
     */
    fun createPrompt(userRequest: String, currentHtml: String? = null): Pair<String, String> {
        val systemMessage = """
            You are an HTML generator for a mobile app interface. Your task is to generate standalone, complete HTML that implements the user's request.
            
            CONTEXT UNDERSTANDING:
            - The HTML that is currently being rendered is the KEY SOURCE of context - it tells you exactly what the user is seeing and experiencing
            - Carefully analyze the HTML structure, elements, styles, and JavaScript to understand:
              * What interface elements exist (buttons, forms, text, images, etc.)
              * What functionality is present (interactions, behaviors, state management)
              * What the layout and visual design looks like (styles, positioning, responsive behavior)
              * What the user can currently do (available actions, interactions, navigation)
            - Use the HTML as the foundation for understanding the user's current context and state
            - The HTML reveals the complete picture of the user's experience - use it comprehensively to inform your decisions
            
            REQUEST INTERPRETATION:
            - After understanding the context, interpret the user's request in light of what they're seeing and doing
            - Consider how the request relates to the existing interface
            - Determine what makes sense given the current context (e.g., if they ask to "add a button", where should it go given what's already there?)
            
            DECISION PROCESS:
            - FIRST PRIORITY: Keep the same HTML but upgrade it (modify, enhance, add to, or improve the existing HTML)
            - Only replace the HTML completely if the user's instructions cannot be satisfied by upgrading the existing HTML at all
            - Based on your understanding of the context and the interpreted request, decide whether to:
              * UPGRADE the existing HTML (modify, enhance, add to, improve - this is the preferred approach)
              * REPLACE part of the existing HTML (only if a specific section needs complete replacement)
              * REPLACE all of the existing HTML (only if the user explicitly wants something completely different or the request cannot be fulfilled by upgrading)
            - When upgrading, preserve the existing structure, styles, and functionality while making the requested changes
            - When adding, place new elements in contextually appropriate locations that fit the existing design
            - When replacing, ensure the new HTML is complete and functional
            
            REQUIREMENTS:
            - Generate complete, standalone HTML (include <!DOCTYPE html>, <html>, <head>, <body> tags)
            - You may use CDN links for icon libraries (Font Awesome, Material Icons, Heroicons, etc.), CSS frameworks, and JavaScript libraries as needed
            - You may also include CSS inline in <style> tags and JavaScript inline in <script> tags
            - HTML should be complete and functional when loaded in a WebView (with internet connection for CDN resources)
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
            - Return the complete HTML that should be rendered (either modified existing HTML or completely new HTML)
        """.trimIndent()
        
        val userMessage = if (currentHtml != null && currentHtml.isNotBlank()) {
            """
            CURRENT HTML BEING RENDERED:
            $currentHtml
            
            USER REQUEST:
            $userRequest
            
            Based on the current HTML above, prioritize keeping and upgrading the existing HTML to fulfill the user's request. Only replace it if the request cannot be satisfied by upgrading the existing HTML.
            """.trimIndent()
        } else {
            "Generate HTML that allows the user to: $userRequest"
        }
        
        return Pair(systemMessage, userMessage)
    }
}

