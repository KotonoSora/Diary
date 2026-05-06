package com.kotonosora.todolist.data.native

/**
 * Kotlin JNI wrapper for native C++ performance helpers.
 * The "todolist" native library is loaded once via TodoApplication.
 */
object TodoNativeHelper {

    /**
     * Sorts an array of strings alphabetically using C++ std::sort.
     */
    external fun sortStrings(arr: Array<String>): Array<String>

    /**
     * Parses the title (first "# " heading) from a Markdown string using C++.
     */
    external fun parseMdTitle(mdContent: String): String

    /**
     * Filters an array of strings by a case-insensitive prefix using C++.
     */
    external fun filterByPrefix(arr: Array<String>, prefix: String): Array<String>
}
