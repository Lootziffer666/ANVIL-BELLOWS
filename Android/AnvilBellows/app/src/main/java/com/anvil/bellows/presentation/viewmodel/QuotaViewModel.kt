package com.anvil.bellows.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anvil.bellows.data.local.db.dao.ProviderConfigDao
import com.anvil.bellows.data.local.db.dao.RequestLogDao
import com.anvil.bellows.data.local.db.entity.ProviderConfigEntity
import com.anvil.bellows.domain.model.Provider
import com.anvil.bellows.domain.model.QuotaStatus
import com.anvil.bellows.util.RateLimitTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuotaViewModel @Inject constructor(
    private val providerConfigDao: ProviderConfigDao,
    private val rateLimitTracker: RateLimitTracker,
    private val requestLogDao: RequestLogDao
) : ViewModel() {

    private val _quotaList = MutableStateFlow<List<QuotaStatus>>(emptyList())
    val quotaList: StateFlow<List<QuotaStatus>> = _quotaList.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                refreshQuota()
                delay(30_000L)
            }
        }
    }

    private suspend fun refreshQuota() {
        val now     = System.currentTimeMillis()
        val dayAgo  = now - 86_400_000L

        val entities = providerConfigDao.getEnabledProviders()
        val statuses = entities.map { entity ->
            val (rpm, rpd)   = rateLimitTracker.getUsage(entity.id)
            val totalToday   = requestLogDao.countSince(entity.id, dayAgo)
            val tokensToday  = requestLogDao.sumTokensSince(entity.id, dayAgo) ?: 0L

            QuotaStatus(
                provider           = entity.toDomain(),
                rpmUsed            = rpm,
                rpdUsed            = rpd,
                totalRequestsToday = totalToday,
                totalTokensToday   = tokensToday,
                isAvailable        = rateLimitTracker.canUse(entity)
            )
        }
        _quotaList.update { statuses }
    }

    fun refresh() {
        viewModelScope.launch { refreshQuota() }
    }

    private fun ProviderConfigEntity.toDomain() = Provider(
        id            = id,
        name          = name,
        baseUrl       = baseUrl,
        rpmLimit      = rpmLimit,
        rpdLimit      = rpdLimit,
        contextWindow = contextWindow,
        maxOutput     = maxOutput,
        tier          = tier,
        isByok        = isByok,
        enabled       = enabled,
        selectedModel = selectedModel,
        authType      = authType
    )
}
