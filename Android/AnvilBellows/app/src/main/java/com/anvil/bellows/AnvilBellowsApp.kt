package com.anvil.bellows

import android.app.Application
import com.anvil.bellows.data.local.db.DatabaseInitializer
import com.anvil.bellows.data.local.db.ProviderBackupManager
import com.anvil.bellows.server.ServerManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AnvilBellowsApp : Application() {

    @Inject lateinit var databaseInitializer: DatabaseInitializer

    /** Manages the BELLOWS NanoHTTPD local API server on port 4141. */
    @Inject lateinit var serverManager: ServerManager

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // 1) DB initialisieren / nach Wipe aus Snapshot wiederherstellen.
            databaseInitializer.initializeIfNeeded()
            // 2) ERST DANACH den laufenden Snapshot-Schutz starten — sonst könnte
            //    ein leerer Zwischenzustand der noch nicht geseedeten DB beobachtet
            //    werden. startSnapshotting() ignoriert leere Listen ohnehin, aber
            //    die sequentielle Reihenfolge macht die Absicht eindeutig.
            providerBackupManager.startSnapshotting(appScope)
        }
        // Start the local OpenAI-compatible server after DI is complete.
        // The server requires EncryptedPrefsManager (for token auth) and
        // LlmRepository (for provider routing) — both are available at this point.
        serverManager.startIfNeeded()
    }
}
