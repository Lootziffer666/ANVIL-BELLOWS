package com.anvil.bellows

import android.app.Application
import com.anvil.bellows.data.local.db.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AnvilBellowsApp : Application() {

    @Inject lateinit var databaseInitializer: DatabaseInitializer

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            databaseInitializer.initializeIfNeeded()
        }
    }
}
