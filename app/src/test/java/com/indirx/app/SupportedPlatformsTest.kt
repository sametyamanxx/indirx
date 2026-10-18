package com.indirx.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedPlatformsTest {
    @Test fun acceptsSupportedLinksAndShortLinks() {
        listOf(
            "https://www.youtube.com/watch?v=test",
            "https://m.youtube.com/shorts/test",
            "https://youtube.com/playlist?list=test",
            "https://youtu.be/test",
            "https://www.instagram.com/reel/test/",
            "https://instagram.com/p/test/",
            "https://www.tiktok.com/@name/video/123",
            "https://vm.tiktok.com/test/",
            "https://vt.tiktok.com/test/",
            "https://www.tiktok.com/t/test/",
            "HTTPS://WWW.YOUTUBE.COM/watch?v=test"
        ).forEach { assertTrue(it, SupportedPlatforms.accepts(it)) }
    }

    @Test fun extractorFilterIncludesTikTokShortLinkResolver() {
        val patterns = SupportedPlatforms.extractors.split(",").map { Regex(it, RegexOption.IGNORE_CASE) }
        fun allowed(name: String) = patterns.any { it.matches(name) }
        listOf("TikTok", "tiktok:user", "vm.tiktok", "youtube", "youtube:tab", "instagram")
            .forEach { assertTrue(it, allowed(it)) }
        listOf("generic", "facebook", "twitter", "vimeo", "dailymotion", "douyin", "vmXtiktok")
            .forEach { assertFalse(it, allowed(it)) }
    }

    @Test fun rejectsOtherPlatformsAndMisleadingHosts() {
        listOf(
            "https://facebook.com/video/123", "https://x.com/name/status/123",
            "https://vimeo.com/123", "https://dailymotion.com/video/123",
            "https://example.com/video.mp4", "https://notyoutube.com/watch?v=test",
            "https://youtube.com.example.org/watch?v=test",
            "https://instagram.com.evil.test/p/123", "https://tiktok.com@evil.test/test",
            "https://evil.test@youtube.com/test", "https://youtube.com:1234/test",
            "https://youtube.com\\@evil.test/test", "ftp://youtube.com/test",
            "file:///youtube.com/test", "youtube.com/watch?v=test", "", "not a url"
        ).forEach { assertFalse(it, SupportedPlatforms.accepts(it)) }
    }
}
