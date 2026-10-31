package com.healthmd.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthmd.R
import com.healthmd.data.health.HealthConnectManager
import com.healthmd.domain.repository.SettingsRepository
import com.healthmd.presentation.common.GeistCard
import com.healthmd.presentation.theme.AppColors
import com.healthmd.presentation.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Fork-local settings card (not in upstream): choose the single Health Connect data origin whose
 * activity data is used, so two writers recording the same movement are never mixed.
 */
@HiltViewModel
class ActivityOriginViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val healthConnect: HealthConnectManager,
) : ViewModel() {
    data class UiState(
        val preferred: String? = null,
        val detected: List<String> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.preferredActivityOrigin.collect { preferred ->
                _state.update { it.copy(preferred = preferred) }
            }
        }
        viewModelScope.launch {
            val origins = healthConnect.recentActivityOrigins()
            _state.update { it.copy(detected = origins) }
        }
    }

    fun select(packageName: String?) {
        viewModelScope.launch { settings.setPreferredActivityOrigin(packageName) }
    }
}

@Composable
fun ActivityOriginSettingCard(viewModel: ActivityOriginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val packageManager = LocalContext.current.packageManager
    fun labelFor(packageName: String): String {
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()
        return if (label.isNullOrBlank() || label == packageName) packageName else "$label ($packageName)"
    }

    // A previously chosen origin stays selectable even if it recorded nothing recently.
    val options = (state.detected + listOfNotNull(state.preferred)).distinct()

    GeistCard {
        Text(
            stringResource(R.string.activity_origin_title),
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.textPrimary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.activity_origin_description),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textMuted,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            OriginRow(
                label = stringResource(R.string.activity_origin_auto),
                selected = state.preferred == null,
                tag = "activity_origin_auto",
                onSelect = { viewModel.select(null) },
            )
            options.forEach { packageName ->
                OriginRow(
                    label = labelFor(packageName),
                    selected = state.preferred == packageName,
                    tag = "activity_origin_$packageName",
                    onSelect = { viewModel.select(packageName) },
                )
            }
        }
        if (state.detected.isEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                stringResource(R.string.activity_origin_none_detected),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted,
            )
        }
    }
}

@Composable
private fun OriginRow(label: String, selected: Boolean, tag: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = AppColors.accent,
                unselectedColor = AppColors.textMuted,
            ),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textPrimary,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}
