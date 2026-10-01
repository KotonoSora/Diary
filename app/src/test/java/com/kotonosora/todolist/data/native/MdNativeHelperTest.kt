package com.kotonosora.todolist.data.native

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MdNativeHelperTest {

    private val diaryContent = """
        ---
        uid: 202603011200
        type: diary
        date: 2026-03-01 09:00
        tags: [diary, journal]
        emotion: HAPPY
        actions: [WORK, EXERCISE]
        ---
        # Diary: Good day

        ## Mood & Energy
        - **Mood Stamp**: Happy
    """.trimIndent()

    @Test
    fun `parseEmotionName reads emotion from frontmatter`() {
        assertEquals("HAPPY", MdNativeHelper.parseEmotionName(diaryContent))
    }

    @Test
    fun `parseActionNames reads actions from frontmatter`() {
        assertEquals(listOf("WORK", "EXERCISE"), MdNativeHelper.parseActionNames(diaryContent))
    }

    @Test
    fun `stamp parsers ignore body text outside frontmatter`() {
        val content = "# Notes\n\nemotion: HAPPY\nSomeone wrote actions: [WORK] in prose."
        assertNull(MdNativeHelper.parseEmotionName(content))
        assertTrue(MdNativeHelper.parseActionNames(content).isEmpty())
    }

    @Test
    fun `stamp parsers return empty when no frontmatter`() {
        assertNull(MdNativeHelper.parseEmotionName("# Plain note"))
        assertTrue(MdNativeHelper.parseActionNames("# Plain note").isEmpty())
    }

    @Test
    fun `parseActionNames handles empty actions list`() {
        val content = "---\nactions: []\n---\n# Title"
        assertTrue(MdNativeHelper.parseActionNames(content).isEmpty())
    }

    @Test
    fun `parseFrontmatterTags reads bracket list normalized to hash form`() {
        assertEquals(
            listOf("#diary", "#journal"),
            MdNativeHelper.parseFrontmatterTags(diaryContent)
        )
    }

    @Test
    fun `parseFrontmatterTags handles single value quoted and hashed entries`() {
        assertEquals(
            listOf("#focus"),
            MdNativeHelper.parseFrontmatterTags("---\ntags: focus\n---\n# T")
        )
        assertEquals(
            listOf("#a", "#b"),
            MdNativeHelper.parseFrontmatterTags("---\ntags: [\"#a\", '#b']\n---\n# T")
        )
        assertEquals(
            emptyList<String>(),
            MdNativeHelper.parseFrontmatterTags("---\ntags: []\n---\n# T")
        )
    }

    @Test
    fun `parseFrontmatterTags ignores body tags and invalid names`() {
        val content = "---\ntags: [ok]\n---\n# T\n\n#bodytag not: [bad tag!]"
        assertEquals(listOf("#ok"), MdNativeHelper.parseFrontmatterTags(content))
        assertTrue(MdNativeHelper.parseFrontmatterTags("# Plain #note").isEmpty())
    }
}
