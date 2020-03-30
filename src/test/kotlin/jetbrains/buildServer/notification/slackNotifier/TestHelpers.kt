package jetbrains.buildServer.notification.slackNotifier


infix fun <T> T.And(other: T): List<T> {
    return listOf(this, other)
}

infix fun <T> List<T>.And(other: T): List<T> {
    return this + other
}