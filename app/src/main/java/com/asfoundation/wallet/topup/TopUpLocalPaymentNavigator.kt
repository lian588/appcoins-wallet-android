package com.asfoundation.wallet.topup

import android.net.Uri
import android.os.Bundle
import com.asfoundation.wallet.navigator.UriNavigator
import com.asfoundation.wallet.ui.iab.Navigator
import io.reactivex.Observable

class TopUpLocalPaymentNavigator(private val uriNavigator: UriNavigator,
                                 private val activityView: TopUpActivityView) : Navigator {

  override fun popView(bundle: Bundle) {
    activityView.finish(bundle)
  }

  override fun popViewWithError() {
    activityView.close()
  }

  override fun navigateToUriForResult(redirectUrl: String) {
    uriNavigator.navigateToUri(redirectUrl)
  }

  override fun uriResults(): Observable<Uri> {
    return uriNavigator.uriResults()
  }

}
