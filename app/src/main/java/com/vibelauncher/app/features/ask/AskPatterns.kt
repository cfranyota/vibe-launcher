package com.vibelauncher.app.features.ask

/** The small fixed set of question shapes Vibe Bar's on-device '?' assistant recognizes.
 *  No LLM, no network - just pattern matching against data already available locally. */
internal enum class AskQuestionType { TODAY_EVENTS, NEXT_EVENT }

/** Matches [query] (the text typed after '?') against a known question pattern, or returns
 *  null if it isn't one - callers should fall back to normal app search on null, exactly as
 *  '?' already behaves today. */
internal fun matchQuestionPattern(query: String): AskQuestionType? {
    val q = query.trim().lowercase().trimEnd('?')
    return when {
        q == "what's on my calendar" || q == "whats on my calendar" ||
            q == "what's on my calendar today" || q == "whats on my calendar today" ||
            q == "what do i have" || q == "what do i have today" -> AskQuestionType.TODAY_EVENTS
        q == "what's my next event" || q == "whats my next event" ||
            q == "what's next" || q == "whats next" -> AskQuestionType.NEXT_EVENT
        else -> null
    }
}
