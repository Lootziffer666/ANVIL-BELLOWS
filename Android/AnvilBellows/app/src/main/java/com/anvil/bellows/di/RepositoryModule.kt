package com.anvil.bellows.di

import com.anvil.bellows.data.repository.ConversationRepository
import com.anvil.bellows.data.repository.LlmRepository
import com.anvil.bellows.data.repository.ProjectRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Repositories are provided via @Inject constructors with @Singleton.
// No explicit Hilt module needed - Hilt injects them automatically.
// This file is kept as a placeholder for future interface bindings if needed.
