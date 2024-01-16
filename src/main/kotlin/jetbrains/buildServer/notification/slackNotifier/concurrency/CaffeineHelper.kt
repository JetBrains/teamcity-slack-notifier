

package jetbrains.buildServer.notification.slackNotifier.concurrency

import com.github.benmanes.caffeine.cache.AsyncCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun <K : Any, V> AsyncCache<K, V>.getAsync(key: K, timeoutMs: Long, mapper: () -> V): V {
    return get(key) { _, executor ->
        val result = CompletableFuture<V>()
        executor.execute {
            try {
                result.complete(mapper())
            } catch (e: Throwable) {
                result.completeExceptionally(e)
            }
        }
        result
    }.get(timeoutMs, TimeUnit.MILLISECONDS)
}