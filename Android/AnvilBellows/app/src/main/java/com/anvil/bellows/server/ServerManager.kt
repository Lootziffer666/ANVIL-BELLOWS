package com.anvil.bellows.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ServerManager"

/**
 * Manages the lifecycle of [BellowsHttpServer].
 *
 * Inject this singleton and call [startIfNeeded] once at app startup (e.g.
 * from [com.anvil.bellows.AnvilBellowsApp.onCreate]). The server runs until
 * [stop] is called or the process ends.
 *
 * Thread-safety: start/stop are idempotent and safe to call from any thread.
 */
@Singleton
class ServerManager @Inject constructor(
    private val server: BellowsHttpServer
) {
    /** Start the server on [BellowsHttpServer.PORT] if it is not already running. */
    fun startIfNeeded() {
        if (server.isAlive) {
            Log.d(TAG, "Server already running on port ${BellowsHttpServer.PORT}")
            return
        }
        try {
            // daemon=false keeps the server thread alive; timeout=NanoHTTPD.SOCKET_READ_TIMEOUT
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.i(TAG, "BELLOWS local server started on port ${BellowsHttpServer.PORT}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BELLOWS local server", e)
        }
    }

    /** Stop the server gracefully. */
    fun stop() {
        if (server.isAlive) {
            server.stop()
            Log.i(TAG, "BELLOWS local server stopped")
        }
    }

    val isRunning: Boolean get() = server.isAlive
}
