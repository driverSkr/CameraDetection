package com.spyfinder.hiddencamera.detectorapp.scan

import java.io.IOException
import java.util.concurrent.CancellationException

/** Distinguishes a quiet discovery channel from a request which could not be executed. */
class DiscoveryHealth {
    enum class Stage { PREPARING, SENDING, RECEIVING }
    enum class Outcome { RUNNING, COMPLETE, FAILED, CANCELLED }
    data class Channel(val stage: Stage, val outcome: Outcome, val reason: String = "")
    private val channels = sortedMapOf<String, Channel>()
    @Synchronized private fun update(name: String, value: Channel) { channels[name] = value }
    @Synchronized fun snapshot(): Map<String, Channel> = channels.toMap()

    fun run(name: String, closed: () -> Boolean, block: ((Stage) -> Unit) -> Unit) {
        var stage = Stage.PREPARING
        fun record(outcome: Outcome, reason: String = "") = update(name, Channel(stage, outcome, reason))
        record(Outcome.RUNNING)
        try {
            if (closed()) throw CancellationException()
            block { stage = it; record(Outcome.RUNNING) }
            if (closed()) throw CancellationException()
            record(Outcome.COMPLETE)
        } catch (e: CancellationException) {
            record(Outcome.CANCELLED)
            throw e
        } catch (e: Exception) {
            if (closed()) {
                record(Outcome.CANCELLED)
                throw CancellationException()
            }
            if (e !is IOException && e !is SecurityException) throw e
            record(Outcome.FAILED, if (e is SecurityException) "permission" else "network")
        }
    }

    fun warnings(): String = snapshot().filterValues { it.outcome == Outcome.FAILED }.map { (name, result) ->
        when (result.reason) {
            "permission" -> "$name discovery permission was denied. Results may be incomplete. Check app permissions and retry."
            else -> when (result.stage) {
                Stage.PREPARING -> "$name discovery could not start. Results may be incomplete. Reconnect to Wi-Fi and retry."
                Stage.SENDING -> "$name discovery request could not be sent. Results may be incomplete. Reconnect to Wi-Fi and retry."
                Stage.RECEIVING -> "$name discovery responses could not be received. Results may be incomplete. Reconnect to Wi-Fi and retry."
            }
        }
    }.joinToString("\n")
}
