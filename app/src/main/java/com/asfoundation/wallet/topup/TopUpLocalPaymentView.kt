package com.asfoundation.wallet.topup

import android.os.Bundle
import io.reactivex.Observable

interface TopUpLocalPaymentView {
  fun showProcessingLoading()
  fun hideLoading()
  fun showCompletedPayment(bundle: Bundle)
  fun showPendingUserPayment()
  fun showGenericError()
  fun showNetworkError()
  fun errorDismisses(): Observable<Any>
  fun errorCancels(): Observable<Any>
  fun errorPositiveClicks(): Observable<Any>
}
