package com.example.utils

class KeyStabilizer(
    private val windowSize: Int = 4,
    private val threshold: Int = 3
) {
    private val history = mutableListOf<String>()

    @Synchronized
    fun feed(key: String): String? {
        history.add(key)
        if (history.size > windowSize) {
            history.removeAt(0)
        }
        val counts = history.groupingBy { it }.eachCount()
        for ((candidate, count) in counts) {
            if (count >= threshold) {
                history.clear()
                return candidate
            }
        }
        return null
    }

    @Synchronized
    fun clear() {
        history.clear()
    }
}
