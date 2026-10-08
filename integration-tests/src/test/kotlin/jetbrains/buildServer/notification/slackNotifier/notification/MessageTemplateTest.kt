

package jetbrains.buildServer.notification.slackNotifier.notification

import jetbrains.buildServer.BaseTestCase
import org.testng.annotations.Test

class MessageTemplateTest : BaseTestCase() {
    @Test
    fun `should replace known placeholders and keep unknown ones`() {
        val rendered = MessageTemplate.render("{build.emoji} {build.link} *{build.event}* {build.unknown} {not a placeholder}") {
            when (it) {
                TemplatePlaceholder.BUILD_EMOJI -> ":x:"
                TemplatePlaceholder.BUILD_LINK -> "Project / <http://tc/build/1|Build #1>"
                TemplatePlaceholder.BUILD_EVENT -> "failed"
                else -> "?"
            }
        }
        assertEquals(":x: Project / <http://tc/build/1|Build #1> *failed* {build.unknown} {not a placeholder}", rendered)
    }

    @Test
    fun `should report unknown placeholders`() {
        assertEquals(
            listOf("{build.foo}", "{bar}"),
            MessageTemplate.unknownPlaceholders("{build.link} {build.foo} {bar} {build.foo} {changes.count}")
        )
        assertEmpty(MessageTemplate.unknownPlaceholders(MessageTemplate.defaultTemplate))
    }

    @Test
    fun `should list used placeholders`() {
        assertEquals(
            setOf(TemplatePlaceholder.BUILD_LINK, TemplatePlaceholder.CHANGES),
            MessageTemplate.usedPlaceholders("{build.link} {changes} {nope}")
        )
    }

    @Test
    fun `every placeholder should have a unique key`() {
        assertEquals(TemplatePlaceholder.values().size, TemplatePlaceholder.values().map { it.key }.toSet().size)
        for (placeholder in TemplatePlaceholder.values()) {
            assertEquals(placeholder, TemplatePlaceholder.byKey(placeholder.key))
        }
    }

    @Test
    fun `should collapse multiline commit messages into one line`() {
        assertEquals(
            "[Feature]: new deploy pipeline * x: add staging target * y: wire health checks",
            CustomMessageBuilder.singleLine("[Feature]: new deploy pipeline\n* x: add staging target\r\n\n  * y: wire health checks\n", 200)
        )
        assertEquals("1234567...", CustomMessageBuilder.singleLine("1234567890123", 10))
    }

    @Test
    fun `should recognize slack user ids`() {
        assertTrue(CustomMessageBuilder.isSlackUserId("U0123456"))
        assertTrue(CustomMessageBuilder.isSlackUserId("W0123456AB"))
        assertFalse(CustomMessageBuilder.isSlackUserId("#channel"))
        assertFalse(CustomMessageBuilder.isSlackUserId("C0123456"))
        assertFalse(CustomMessageBuilder.isSlackUserId(""))
    }
}
