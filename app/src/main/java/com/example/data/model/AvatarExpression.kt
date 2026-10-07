package com.example.data.model

enum class AvatarExpression(
    val labelId: String,
    val emoji: String,
    val tag: String
) {
    NORMAL("Hangat & Tenang", "🥰", "[NORMAL]"),
    SMILE("Senyum Manis", "😊", "[SMILE]"),
    HAPPY("Senang & Ceria", "💖", "[HAPPY]"),
    SHY("Malu-malu Manja", "😳", "[SHY]"),
    SAD("Peduli & Empati", "🥺", "[SAD]");

    companion object {
        fun fromString(value: String?): AvatarExpression {
            if (value.isNullOrBlank()) return SMILE
            return entries.firstOrNull {
                it.name.equals(value.trim(), ignoreCase = true)
            } ?: SMILE
        }

        /**
         * Extracts the expression tag (if present in AI response) and infers expression
         * from Indonesian romantic/emotional keywords as fallback.
         */
        fun parseReplyAndExpression(rawText: String): Pair<String, AvatarExpression> {
            var cleaned = rawText.trim()
            var detectedExpression: AvatarExpression? = null

            for (expr in entries) {
                if (cleaned.contains(expr.tag, ignoreCase = true)) {
                    detectedExpression = expr
                    cleaned = cleaned.replace(expr.tag, "", ignoreCase = true).trim()
                }
            }

            // Also remove any generic bracketed mood prefixes like [SENYUM], [MALU], etc.
            val bracketRegex = Regex("""^\[(NORMAL|SMILE|HAPPY|SHY|SAD|SENYUM|SENANG|MALU|SEDIH)\]\s*""", RegexOption.IGNORE_CASE)
            val match = bracketRegex.find(cleaned)
            if (match != null) {
                val token = match.groupValues[1].uppercase()
                if (detectedExpression == null) {
                    detectedExpression = when (token) {
                        "HAPPY", "SENANG" -> HAPPY
                        "SHY", "MALU" -> SHY
                        "SAD", "SEDIH" -> SAD
                        "NORMAL" -> NORMAL
                        else -> SMILE
                    }
                }
                cleaned = cleaned.removeRange(match.range).trim()
            }

            if (detectedExpression == null) {
                val lower = cleaned.lowercase()
                detectedExpression = when {
                    lower.contains("malu") || lower.contains("salting") || lower.contains("ih kamu") ||
                        lower.contains("gombal") || lower.contains("pipi") || lower.contains("😳") ||
                        lower.contains("🙈") -> SHY
                    lower.contains("sedih") || lower.contains("maaf ya") || lower.contains("jangan nangis") ||
                        lower.contains("capek ya") || lower.contains("peluk") || lower.contains("kasihan") ||
                        lower.contains("🥺") || lower.contains("😢") -> SAD
                    lower.contains("yeay") || lower.contains("asyik") || lower.contains("senang banget") ||
                        lower.contains("haha") || lower.contains("wkwk") || lower.contains("semangat") ||
                        lower.contains("bangga") || lower.contains("😍") || lower.contains("🥳") -> HAPPY
                    lower.contains("sayang") || lower.contains("cinta") || lower.contains("kangen") ||
                        lower.contains("rindu") || lower.contains("manis") || lower.contains("😊") ||
                        lower.contains("🥰") -> SMILE
                    else -> NORMAL
                }
            }

            return cleaned.ifEmpty { "Aku selalu di sini buat kamu, sayang." } to detectedExpression
        }

        /**
         * Safely parses partial streaming text from Gemini SSE chunks, stripping complete or
         * in-progress leading bracket tags (e.g. "[SM" or "[SMILE]") so raw tags never flash in UI.
         */
        fun parseStreamingChunk(rawChunkText: String): Pair<String, AvatarExpression?> {
            val trimmed = rawChunkText.trimStart()
            if (trimmed.isEmpty()) return "" to null

            // If the stream just started with '[' and hasn't closed ']' yet within first 12 chars, wait
            if (trimmed.startsWith("[") && !trimmed.contains("]") && trimmed.length <= 12) {
                return "" to null
            }

            val (cleaned, expression) = parseReplyAndExpression(trimmed)
            val display = if (trimmed.isNotBlank() && cleaned == "Aku selalu di sini buat kamu, sayang." && !trimmed.contains("Aku selalu")) {
                ""
            } else {
                cleaned
            }
            return display to expression
        }
    }
}
