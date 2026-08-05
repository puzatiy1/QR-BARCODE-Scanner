package com.example.utils

object ProductKeyParser {

    private const val ALLOWED_CHARS = "BCDFGHJKMNPQRTVWXY2346789"

    /**
     * Extracts a Windows Product Key from recognized text.
     * A Windows product key consists of 25 alphanumeric characters,
     * grouped in 5 groups of 5 characters.
     * Returns the formatted key (XXXXX-XXXXX-XXXXX-XXXXX-XXXXX) if found, or null otherwise.
     */
    fun extractWindowsProductKey(text: String): String? {
        if (text.isBlank()) return null

        val cleanedText = cleanTextForAdjacency(text)
        val adjacentSequences = findAdjacentGroups(cleanedText)

        var bestKey: String? = null
        var minInvalidCount = Int.MAX_VALUE

        for (sequence in adjacentSequences) {
            val key = validateAndFormatKey(sequence)
            if (key != null) {
                val cleanKey = key.replace("-", "")
                val invalidCount = cleanKey.count { !ALLOWED_CHARS.contains(it) }
                if (invalidCount < minInvalidCount) {
                    minInvalidCount = invalidCount
                    bestKey = key
                }
            }
        }

        return bestKey
    }

    /**
     * Leniently extracts a Windows Product Key from recognized text.
     * Specifically optimized for static photos/gallery.
     */
    fun extractWindowsProductKeyLenient(text: String): String? {
        if (text.isBlank()) return null

        val cleanedText = cleanTextForAdjacency(text)
        val adjacentSequences = findAdjacentGroups(cleanedText)

        var bestKey: String? = null
        var minInvalidCount = Int.MAX_VALUE

        for (sequence in adjacentSequences) {
            val key = validateAndFormatKeyLenient(sequence)
            if (key != null) {
                val cleanKey = key.replace("-", "")
                val invalidCount = cleanKey.count { !ALLOWED_CHARS.contains(it) }
                if (invalidCount < minInvalidCount) {
                    minInvalidCount = invalidCount
                    bestKey = key
                }
            }
        }

        return bestKey
    }

    private fun cleanTextForAdjacency(text: String): String {
        val upper = text.uppercase()
        // Replace common noisy prefixes before the first group (like O-, 0-, Q-, *-)
        var cleaned = upper.replace(Regex("\\b[O0Q*]-"), " ")
        // Replace all non-alphanumeric characters with spaces
        cleaned = cleaned.replace(Regex("[^A-Z0-9\\s]"), " ")
        return preprocessMergedGroups(cleaned)
    }

    private fun preprocessMergedGroups(text: String): String {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val processedWords = words.map { word ->
            if (word.length in 10..12) {
                // Split merged groups of length 10-12 (e.g. PP7B6242MQ)
                val mid = word.length / 2
                val part1 = word.substring(0, mid)
                val part2 = word.substring(mid)
                "$part1 $part2"
            } else {
                word
            }
        }
        return processedWords.joinToString(" ")
    }

    private fun findAdjacentGroups(text: String): List<List<String>> {
        // Find all tokens that look like a group of length 4 to 6
        val groupRegex = Regex("[A-Z0-9]{4,6}")
        val matches = groupRegex.findAll(text).toList()
        
        val results = mutableListOf<List<String>>()
        if (matches.size < 5) return results
        
        for (i in 0..matches.size - 5) {
            var isValidSequence = true
            
            for (j in 0..3) {
                val currentMatch = matches[i + j]
                val nextMatch = matches[i + j + 1]
                
                val betweenStart = currentMatch.range.last + 1
                val betweenEnd = nextMatch.range.first
                
                if (betweenStart > betweenEnd) {
                    isValidSequence = false
                    break
                }
                
                val betweenText = text.substring(betweenStart, betweenEnd)
                // The text between must only contain whitespace/separators. No other letters or digits, and no newlines!
                if (betweenText.any { it.isLetterOrDigit() } || betweenText.contains('\n') || betweenText.contains('\r')) {
                    isValidSequence = false
                    break
                }
                // Limit the gap length to 5 characters to avoid picking up far-away noise (e.g. adjacent should only have dash or space)
                if (betweenText.length > 5) {
                    isValidSequence = false
                    break
                }
            }
            
            if (isValidSequence) {
                val groupList = (0..4).map { matches[i + it].value }
                results.add(groupList)
            }
        }
        return results
    }

    private fun sanitizeAndTrimToken(token: String): String {
        val len = token.length
        if (len == 5) {
            return token.replace('O', 'Q').replace('0', 'Q')
        }
        if (len < 5) {
            val mapped = token.replace('O', 'Q').replace('0', 'Q')
            return mapped.padEnd(5, 'X')
        }
        
        if (len == 6) {
            val firstChar = token[0]
            val lastChar = token[5]
            val firstNoisy = firstChar == 'Q' || firstChar == 'O' || firstChar == '0' || !ALLOWED_CHARS.contains(firstChar)
            val lastNoisy = lastChar == 'Q' || lastChar == 'O' || lastChar == '0' || !ALLOWED_CHARS.contains(lastChar)
            
            if (firstNoisy && !lastNoisy) {
                return token.substring(1, 6).replace('O', 'Q').replace('0', 'Q')
            } else if (!firstNoisy && lastNoisy) {
                return token.substring(0, 5).replace('O', 'Q').replace('0', 'Q')
            }
        }
        
        // Find all contiguous 5-character substrings
        val subs = (0..len - 5).map { token.substring(it, it + 5) }
        
        var bestSub = subs[0]
        var maxValid = -1
        
        for (sub in subs) {
            val validCount = sub.count { ALLOWED_CHARS.contains(it) }
            if (validCount > maxValid) {
                maxValid = validCount
                bestSub = sub
            }
        }
        
        return bestSub.replace('O', 'Q').replace('0', 'Q')
    }

    private fun validateAndFormatKey(groups: List<String>): String? {
        if (groups.size != 5) return null
        if (groups.any { it.length !in 4..6 }) return null

        val totalLength = groups.sumOf { it.length }
        if (totalLength !in 23..27) return null

        var totalInvalidCount = 0
        for (group in groups) {
            val normalizedGroup = group.replace('O', 'Q').replace('0', 'Q')
            val invalidInGroup = normalizedGroup.count { !ALLOWED_CHARS.contains(it) }
            if (invalidInGroup > 1) {
                return null
            }
            totalInvalidCount += invalidInGroup
        }

        if (totalInvalidCount > 1) return null

        val normalizedCombined = groups.joinToString("").replace('O', 'Q').replace('0', 'Q')
        val letterCount = normalizedCombined.count { it.isLetter() }
        val digitCount = normalizedCombined.count { it.isDigit() }
        if (letterCount < 5 || digitCount < 3) return null

        val formattedGroups = groups.map { sanitizeAndTrimToken(it) }
        return formattedGroups.joinToString("-")
    }

    private fun validateAndFormatKeyLenient(groups: List<String>): String? {
        if (groups.size != 5) return null
        if (groups.any { it.length !in 4..6 }) return null

        val totalLength = groups.sumOf { it.length }
        if (totalLength !in 23..27) return null

        var totalInvalidCount = 0
        for (group in groups) {
            val normalizedGroup = group.replace('O', 'Q').replace('0', 'Q')
            val invalidInGroup = normalizedGroup.count { !ALLOWED_CHARS.contains(it) }
            if (invalidInGroup > 1) {
                return null
            }
            totalInvalidCount += invalidInGroup
        }

        if (totalInvalidCount > 2) return null

        val normalizedCombined = groups.joinToString("").replace('O', 'Q').replace('0', 'Q')
        val letterCount = normalizedCombined.count { it.isLetter() }
        val digitCount = normalizedCombined.count { it.isDigit() }
        if (letterCount < 5 || digitCount < 3) return null

        val originalCombined = groups.joinToString("")
        val vowelCount = originalCombined.count { "AEIOU".contains(it) }
        if (vowelCount > 3) return null

        val formattedGroups = groups.map { sanitizeAndTrimToken(it) }
        return formattedGroups.joinToString("-")
    }
}
