package com.asfoundation.wallet.topup

import android.os.Bundle
import com.appcoins.wallet.bdsbilling.repository.entity.Transaction
import com.appcoins.wallet.billing.BillingMessagesMapper
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import java.io.IOException
import java.net.UnknownHostException

class TopUpLocalPaymentPresenter(private val view: TopUpLocalPaymentView,
                                 private val packageName: String,
                                 private val amount: String,
                                 private val currency: String,
                                 private val bonus: String,
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
    handleErrorDismissEvent()
  }

  private fun onViewCreatedRequestLink() {
    disposables.add(
        interactor.getPaymentLink(packageName, amount, currency, paymentId)
            .filter { !waitingResult }
            .observeOn(viewScheduler)
            .doOnSuccess {
              navigator.navigateToUriForResult(it)
              waitingResult = true
            }
            .subscribeOn(networkScheduler)
            .observeOn(viewScheduler)
            .subscribe({ }, { showError(it) }))
  }

  private fun showError(throwable: Throwable) {
    if (isNoNetworkException(throwable)) {
      view.showNetworkError()
    } else {
      view.showGenericError()
    }
  }

  private fun handlePaymentRedirect() {
    disposables.add(navigator.uriResults()
        .doOnNext { view.showProcessingLoading() }
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
      Transaction.Status.COMPLETED -> view.showCompletedPayment(createTopUpBundle())
      Transaction.Status.PENDING_USER_PAYMENT -> view.showPendingUserPayment()
      else -> view.showGenericError()
    }
  }

  private fun handleErrorDismissEvent() {
    disposables.add(
        Observable.merge(view.errorDismisses(), view.errorCancels(), view.errorPositiveClicks())
            .subscribe({ navigator.popViewWithError() }, { it.printStackTrace() }))
  }

  private fun isNoNetworkException(throwable: Throwable): Boolean {
    return throwable is IOException ||
        throwable.cause != null && throwable.cause is IOException ||
        throwable is UnknownHostException
  }

  fun stop() {
    disposables.clear()
  }

  private fun createTopUpBundle(): Bundle {
    val bundle = Bundle()
    bundle.putString(BillingMessagesMapper.TOP_UP_AMOUNT, amount)
    bundle.putString(BillingMessagesMapper.TOP_UP_CURRENCY, currency)
    bundle.putString(BillingMessagesMapper.BONUS, bonus)
    return bundle
  }

  companion object {
    private const val WAITING_RESULT = "WAITING_RESULT"
  }
}
