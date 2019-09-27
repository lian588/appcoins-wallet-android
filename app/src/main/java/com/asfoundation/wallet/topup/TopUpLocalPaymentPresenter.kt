package com.asfoundation.wallet.topup

import android.os.Bundle
import com.appcoins.wallet.bdsbilling.repository.entity.Transaction
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable

class TopUpLocalPaymentPresenter(private val view: TopUpLocalPaymentView,
                                 private val packageName: String,
                                 private val amount: String,
                                 private val currency: String,
                                 private val paymentId: String,
                                 private val interactor: TopUpLocalPaymentInteractor,
                                 private val navigator: TopUpLocalPaymentNavigator,
                                 private val disposables: CompositeDisposable,
                                 private val savedInstance: Bundle?,
                                 private val viewScheduler: Scheduler,
                                 private val networkScheduler: Scheduler) {

  private var waitingResult: Boolean = false

  fun present() {
    if (savedInstance != null) {
      waitingResult = savedInstance.getBoolean(WAITING_RESULT)
    }
    onViewCreatedRequestLink()
    handlePaymentRedirect()
  }

  private fun onViewCreatedRequestLink() {
    disposables.add(
        interactor.getPaymentLink(packageName, amount, currency, paymentId)
            .filter { !waitingResult }
            .observeOn(viewScheduler)
            .doOnSuccess {
              /*analytics.sendPaymentMethodDetailsEvent(domain, skuId, amount.toString(), type,
                  paymentId)*/
              navigator.navigateToUriForResult(it)
              waitingResult = true
            }
            .subscribeOn(networkScheduler)
            .observeOn(viewScheduler)
            .subscribe({ }, { showError(it) }))
  }

  private fun showError(throwable: Throwable) {
    //TODO
  }

  private fun handlePaymentRedirect() {
    disposables.add(navigator.uriResults()
        .doOnNext { view.showProcessingLoading() }
        .doOnNext { view.lockRotation() }
        .flatMap {
          interactor.getTransaction(it)
              .subscribeOn(networkScheduler)
        }
        .observeOn(viewScheduler)
        .doOnNext { handleTransactionStatus(it) }
        .subscribe({}, { showError(it) }))
  }

  private fun handleTransactionStatus(transaction: Transaction) {
    view.hideLoading()
    when (transaction.status) {
      Transaction.Status.COMPLETED -> view.showCompletedPayment()
      Transaction.Status.PENDING_USER_PAYMENT -> view.showPendingUserPayment()
      else -> view.showError()
    }
  }

  fun stop() {
    disposables.clear()
  }

  companion object {
    private const val WAITING_RESULT = "WAITING_RESULT"
  }
}
