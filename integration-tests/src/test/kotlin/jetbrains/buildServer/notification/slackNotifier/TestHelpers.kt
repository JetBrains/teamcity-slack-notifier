

package jetbrains.buildServer.notification.slackNotifier


@Suppress("TestFunctionName")
infix fun <T> T.And(other: T): List<T> {
    return listOf(this, other)
}

@Suppress("TestFunctionName")
infix fun <T> List<T>.And(other: T): List<T> {
    return this + other
}