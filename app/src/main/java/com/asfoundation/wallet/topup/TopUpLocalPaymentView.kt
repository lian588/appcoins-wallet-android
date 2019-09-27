package com.asfoundation.wallet.topup

import android.os.Bundle

interface TopUpLocalPaymentView {
  fun showProcessingLoading()
  fun lockRotation()
  fun hideLoading()
  fun showCompletedPayment(bundle: Bundle)
  fun showPendingUserPayment()
  fun showError()
}
