package com.anvil.bellows

import android.app.Application
import com.anvil.bellows.data.local.db.DatabaseInitializer
import com.anvil.bellows.server.ServerManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AnvilBellowsApp : Application() {

    @Inject lateinit var databaseInitializer: DatabaseInitializer

    /** Manages the BELLOWS NanoHTTPD local API server on port 4141. */
    @Inject lateinit var serverManager: ServerManager

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            databaseInitializer.initializeIfNeeded()
        }
        // Start the local OpenAI-compatible server after DI is complete.
        // The server requires EncryptedPrefsManager (for token auth) and
        // LlmRepository (for provider routing) — both are available at this point.
        serverManager.startIfNeeded()
    }
}
