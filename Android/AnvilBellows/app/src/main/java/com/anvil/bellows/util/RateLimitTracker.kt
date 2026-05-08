package com.anvil.bellows.util

import com.anvil.bellows.data.local.db.dao.ModelConfigDao
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.db.dao.RequestLogDao
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.data.remote.provider.Specialty
import kotlinx.coroutines.sync.Semaphore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimitTracker @Inject constructor(
    private val requestLogDao: RequestLogDao,
    private val providerConfigDao: ProviderConfigDao,
    private val modelConfigDao: ModelConfigDao
) {
    private val zaiSemaphore = Semaphore(1)

    suspend fun canUse(provider: ProviderConfigEntity): Boolean {
        if (!provider.enabled) return false

        val now = System.currentTimeMillis()
        val rpmCount = requestLogDao.countSince(provider.id, now - 60_000L)
        val rpdCount = requestLogDao.countSince(provider.id, now - 86_400_000L)

        val effectiveRpm = if (provider.rpmLimit == Int.MAX_VALUE) Int.MAX_VALUE else provider.rpmLimit - 1
        val effectiveRpd = if (provider.rpdLimit == Int.MAX_VALUE) Int.MAX_VALUE else provider.rpdLimit - 5

        return rpmCount < effectiveRpm && rpdCount < effectiveRpd
    }

    suspend fun getRankedAvailableProviders(specialty: Specialty? = null): List<ProviderConfigEntity> {
        val available = providerConfigDao.getEnabledProviders().filter { canUse(it) }
        val sorter = compareBy<ProviderConfigEntity>({ it.tier }, { it.rpmLimit.toLong() * -1 })

        if (specialty == null || specialty == Specialty.GENERAL) {
            return available.sortedWith(sorter)
        }

        val specialtyStr = specialty.name
        val preferred = mutableListOf<ProviderConfigEntity>()
        val fallback = mutableListOf<ProviderConfigEntity>()

        for (provider in available) {
            val models = modelConfigDao.getByProvider(provider.id)
            if (models.any { it.specialties.contains(specialtyStr) }) {
                preferred.add(provider)
            } else {
                fallback.add(provider)
            }
        }

        return preferred.sortedWith(sorter) + fallback.sortedWith(sorter)
    }

    suspend fun getUsage(providerId: String): Pair<Int, Int> {
        val now = System.currentTimeMillis()
        val rpm = requestLogDao.countSince(providerId, now - 60_000L)
        val rpd = requestLogDao.countSince(providerId, now - 86_400_000L)
        return rpm to rpd
    }

    fun getZaiSemaphore(): Semaphore = zaiSemaphore
}
