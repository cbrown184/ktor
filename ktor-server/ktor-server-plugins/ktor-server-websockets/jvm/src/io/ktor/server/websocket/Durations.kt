/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")

package io.ktor.websocket

import io.ktor.server.websocket.*
import io.ktor.util.cio.*
import io.ktor.utils.io.pool.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.nio.*
import java.time.*

/**
 * Ping interval or `null` to disable pinger. Please note that pongs will be handled despite this setting.
 */
public inline var DefaultWebSocketServerSession.pingInterval: Duration?
    get() = pingIntervalMillis.takeIf { it >= 0L }?.let { Duration.ofMillis(it) }
    set(newDuration) {
        pingIntervalMillis = newDuration?.toMillis() ?: -1L
    }

/**
 * A timeout to wait for pong reply to ping otherwise the session will be terminated immediately.
 * It doesn't have any effect if [pingInterval] is `null` (pinger is disabled).
 */
public inline var DefaultWebSocketServerSession.timeout: Duration
    get() = Duration.ofMillis(timeoutMillis)
    set(newDuration) {
        timeoutMillis = newDuration.toMillis()
    }

/**
 * Launch pinger coroutine on [CoroutineScope] that is sending ping every specified [period] to [outgoing] channel,
 * waiting for and verifying client's pong frames. It is also handling [timeout] and sending timeout close frame
 */
public fun CoroutineScope.pinger(
    outgoing: SendChannel<Frame>,
    period: Duration,
    timeout: Duration,
    pool: ObjectPool<ByteBuffer> = KtorDefaultPool
): SendChannel<Frame.Pong> = pinger(outgoing, period.toMillis(), timeout.toMillis(), pool)

public fun WebSockets(
    pingInterval: Duration?,
    timeout: Duration,
    maxFrameSize: Long,
    masking: Boolean
): WebSockets = WebSockets(
    pingInterval?.toMillis() ?: 0L,
    timeout.toMillis(),
    maxFrameSize,
    masking
)

public inline val WebSockets.pingInterval: Duration?
    get() = when (pingIntervalMillis) {
        0L -> null
        else -> Duration.ofMillis(pingIntervalMillis)
    }

public inline val WebSockets.timeout: Duration
    get() = Duration.ofMillis(timeoutMillis)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline var WebSockets.WebSocketOptions.pingPeriod: Duration?
    get() = when (pingPeriodMillis) {
        0L -> null
        else -> Duration.ofMillis(pingPeriodMillis)
    }
    set(new) {
        pingPeriodMillis = when (new) {
            null -> 0
            else -> new.toMillis()
        }
    }

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public inline var WebSockets.WebSocketOptions.timeout: Duration
    get() = Duration.ofMillis(timeoutMillis)
    set(new) {
        timeoutMillis = new.toMillis()
    }
