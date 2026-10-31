package com.healthmd.data.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Fork-local: the preferred activity data origin is a device-local DataStore setting. */
class SettingsRepositoryActivityOriginTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { temporaryFolder.newFile("activity-origin.preferences_pb") },
        )
        repository = SettingsRepositoryImpl(dataStore, mockk<Context>(relaxed = true))
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `defaults to automatic`() = runTest {
        assertThat(repository.getPreferredActivityOrigin()).isNull()
        assertThat(repository.preferredActivityOrigin.first()).isNull()
    }

    @Test
    fun `chosen origin round trips`() = runTest {
        repository.setPreferredActivityOrigin("nl.appyhapps.healthsync")

        assertThat(repository.getPreferredActivityOrigin()).isEqualTo("nl.appyhapps.healthsync")
        assertThat(repository.preferredActivityOrigin.first()).isEqualTo("nl.appyhapps.healthsync")
    }

    @Test
    fun `null or blank clears the preference`() = runTest {
        repository.setPreferredActivityOrigin("nl.appyhapps.healthsync")
        repository.setPreferredActivityOrigin(null)
        assertThat(repository.getPreferredActivityOrigin()).isNull()

        repository.setPreferredActivityOrigin("nl.appyhapps.healthsync")
        repository.setPreferredActivityOrigin("  ")
        assertThat(repository.getPreferredActivityOrigin()).isNull()
    }
}
