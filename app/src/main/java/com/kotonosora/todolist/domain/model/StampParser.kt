package com.kotonosora.todolist.domain.model

import com.kotonosora.todolist.data.native.MdNativeHelper
import java.util.Locale

/**
 * Single owner for mood-stamp parsing (frontmatter `emotion:` / `actions:`).
 * Previously duplicated in VaultRepositoryImpl.entityToDomain + EditorScreen.
 */
object StampParser {
    fun parseEmotion(content: String): EmotionStamp? {
        return try {
            MdNativeHelper.parseEmotionName(content)
                ?.let { EmotionStamp.valueOf(it.uppercase(Locale.ROOT)) }
        } catch (_: Exception) {
            null
        }
    }

    fun parseActions(content: String): List<ActionStamp> {
        return try {
            MdNativeHelper.parseActionNames(content).mapNotNull {
                try {
                    ActionStamp.valueOf(it.uppercase(Locale.ROOT))
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
