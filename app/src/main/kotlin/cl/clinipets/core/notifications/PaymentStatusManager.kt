package cl.clinipets.core.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentStatusManager @Inject constructor() {

    private val _paymentConfirmedFlow = MutableSharedFlow<String>()
    val paymentConfirmedFlow = _paymentConfirmedFlow.asSharedFlow()

    suspend fun notifyPaymentSuccess(citaId: String) {
        _paymentConfirmedFlow.emit(citaId)
    }
}
