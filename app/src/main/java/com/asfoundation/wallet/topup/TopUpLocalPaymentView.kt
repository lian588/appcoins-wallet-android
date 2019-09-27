package com.asfoundation.wallet.topup

interface TopUpLocalPaymentView {
  fun showProcessingLoading()
  fun lockRotation()
  fun hideLoading()
  fun showCompletedPayment()
  fun showPendingUserPayment()
  fun showError()
}
