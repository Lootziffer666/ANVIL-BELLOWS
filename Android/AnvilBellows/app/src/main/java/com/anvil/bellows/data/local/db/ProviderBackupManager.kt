package com.anvil.bellows.data.local.db

import com.anvil.bellows.data.local.db.dao.ModelConfigDao
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.db.entity.ModelConfigEntity
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.data.local.prefs.EncryptedPrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Datensichere Sicherung der Provider-Konfiguration.
 *
 * Hintergrund: Bei einer destruktiven Room-Migration (fallbackToDestructiveMigration)
 * wird die DB komplett neu angelegt — alle provider_configs gehen verloren. Die
 * eigentlichen API-Keys liegen zwar in den EncryptedPrefs (api_key_<id>) und
 * überleben, ohne Provider-Zeilen sind sie aber "verwaist" und im UI unsichtbar
 * (das fühlte sich an wie "alle 40 Keys weg").
 *
 * Lösung: Wir halten einen verschlüsselten Snapshot aller Provider-/Modell-
 * Konfigurationen in den EncryptedPrefs aktuell. Ist die DB nach einem Wipe leer,
 * stellen wir daraus alles wieder her — inkl. eigener Custom-Provider und
 * Anpassungen. Die Keys passen automatisch wieder (gleiche apiKeyAlias-IDs).
 *
 * Schutzregeln (wie beim CUE-AGENT-Keystore):
 *  - Es wird NIE ein leerer Snapshot geschrieben (kein versehentliches Leeren).
 *  - Restore läuft NUR, wenn die DB tatsächlich leer ist (kein Überschreiben
 *    vorhandener, echter Daten).
 */
@Singleton
class ProviderBackupManager @Inject constructor(
    private val providerConfigDao: ProviderConfigDao,
    private val modelConfigDao: ModelConfigDao,
    private val encryptedPrefs: EncryptedPrefsManager
) {
    private val gson = Gson()

    data class Snapshot(
        val providers: List<ProviderConfigEntity> = emptyList(),
        val models: List<ModelConfigEntity> = emptyList()
    )

    /** Schreibt einen frischen Snapshot — niemals leer (Schutz vor Daten-Wipe). */
    suspend fun refreshSnapshot() = withContext(Dispatchers.IO) {
        val providers = providerConfigDao.getAll()
        if (providers.isEmpty()) return@withContext   // niemals leeren Snapshot speichern
        val models = providers.flatMap { modelConfigDao.getModelsForProvider(it.id) }
        encryptedPrefs.storeProviderSnapshot(gson.toJson(Snapshot(providers, models)))
    }

    /**
     * Stellt Provider/Modelle aus dem Snapshot wieder her — nur wenn die DB leer ist.
     * @return true, wenn etwas wiederhergestellt wurde.
     */
    suspend fun restoreFromSnapshot(): Boolean = withContext(Dispatchers.IO) {
        if (providerConfigDao.count() > 0) return@withContext false
        val json = encryptedPrefs.getProviderSnapshot() ?: return@withContext false
        val snap = try { gson.fromJson(json, Snapshot::class.java) } catch (e: Exception) { null }
            ?: return@withContext false
        if (snap.providers.isEmpty()) return@withContext false
        providerConfigDao.upsertAll(snap.providers)
        if (snap.models.isNotEmpty()) modelConfigDao.upsertAll(snap.models)
        true
    }

    /**
     * Hält den Snapshot dauerhaft aktuell: bei jeder Änderung der Provider-Liste
     * (sofern nicht leer) wird neu gesichert.
     */
    fun startSnapshotting(scope: CoroutineScope) {
        scope.launch {
            providerConfigDao.observeAll().collect { list ->
                if (list.isNotEmpty()) refreshSnapshot()
            }
        }
    }
}
