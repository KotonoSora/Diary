package com.kotonosora.todolist.data.native

/**
 * Pure Kotlin helper (historically "native"; now 100% Kotlin).
 */
object MainNativeHelper {

    /**
     * Filters an array of strings by a case-insensitive prefix.
     */
    fun filterByPrefix(arr: Array<String>, prefix: String): Array<String> {
        if (prefix.isEmpty()) return arr
        return arr.filter { it.startsWith(prefix, ignoreCase = true) }.toTypedArray()
    }
}
