package io.github.taetae98coding.jarvis.data.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ExpandHomeTest {
    @Test
    fun tildeIsTheHome() {
        assertEquals("/Users/me", expandHome("~", "/Users/me"))
    }

    @Test
    fun tildeSlashIsUnderTheHome() {
        assertEquals("/Users/me/work", expandHome("~/work", "/Users/me"))
        assertEquals("/Users/me/work", expandHome("~/work", "/Users/me/"))
    }

    @Test
    fun otherPathsStayAsTheyAre() {
        assertEquals("~other/work", expandHome("~other/work", "/Users/me"))
        assertEquals("/work/~", expandHome("/work/~", "/Users/me"))
        assertEquals("work", expandHome("work", "/Users/me"))
    }
}
