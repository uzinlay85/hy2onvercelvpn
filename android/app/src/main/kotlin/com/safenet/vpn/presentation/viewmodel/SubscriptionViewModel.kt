package com.safenet.vpn.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safenet.vpn.data.remote.SafeNetApiService
import com.safenet.vpn.data.remote.dto.PlanDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val plans: List<PlanDto> = emptyList(),
    val selectedPlan: PlanDto? = null,
    val isCheckoutPending: Boolean = false,
    val checkoutSuccess: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val apiService: SafeNetApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        fetchPlans()
    }

    fun fetchPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val res = apiService.getPlans()
                if (res.isSuccessful && res.body()?.success == true) {
                    val list = res.body()?.data ?: emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            plans = list,
                            selectedPlan = list.firstOrNull { p -> p.isPopular } ?: list.firstOrNull(),
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.body()?.message ?: "Failed to retrieve subscription tiers") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Network error fetching plans") }
            }
        }
    }

    fun selectPlan(plan: PlanDto) {
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    fun initiateCheckout() {
        val plan = _uiState.value.selectedPlan ?: return
        _uiState.update { it.copy(isCheckoutPending = true, errorMessage = null) }

        viewModelScope.launch {
            // Simulated Stripe / Google Play Billing Checkout Flow
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(isCheckoutPending = false, checkoutSuccess = true) }
        }
    }

    fun resetCheckoutState() {
        _uiState.update { it.copy(checkoutSuccess = false, isCheckoutPending = false) }
    }
}
