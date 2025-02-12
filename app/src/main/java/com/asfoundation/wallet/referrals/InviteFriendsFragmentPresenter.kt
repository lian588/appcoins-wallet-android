package com.asfoundation.wallet.referrals

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.math.BigDecimal

class InviteFriendsFragmentPresenter(private val view: InviteFriendsFragmentView,
                                     private val activity: InviteFriendsActivityView?,
                                     private val disposable: CompositeDisposable,
                                     private val referralInteractor: ReferralInteractorContract) {

  fun present() {
    handleInfoButtonClick()
    handleShareClicks()
    handleAppsGamesClicks()
    handlePendingNotification()
  }

  private fun handlePendingNotification() {
    disposable.add(
        referralInteractor.getPendingBonusNotification()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { view.showNotificationCard(it.pendingAmount, it.symbol) }
            .doOnComplete { view.showNotificationCard(BigDecimal.ZERO, "") }
            .doOnError { handlerError(it) }
            .subscribe()
    )
  }

  private fun handleShareClicks() {
    disposable.add(view.shareLinkClick()
        .doOnNext { view.showShare() }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleAppsGamesClicks() {
    disposable.add(view.appsAndGamesButtonClick()
        .doOnNext { view.navigateToAptoide() }
        .subscribe({}, { it.printStackTrace() }))
  }

  private fun handleInfoButtonClick() {
    activity?.let {
      disposable.add(it.getInfoButtonClick()
          .doOnNext { view.changeBottomSheetState() }
          .subscribe())
    }
  }

  private fun handlerError(throwable: Throwable) {
    throwable.printStackTrace()
    if (isNoNetworkException(throwable)) {
      activity?.showNetworkErrorView()
    }
  }

  private fun isNoNetworkException(throwable: Throwable): Boolean {
    return throwable is IOException || throwable.cause != null && throwable.cause is IOException
  }

  fun stop() {
    disposable.clear()
  }
}