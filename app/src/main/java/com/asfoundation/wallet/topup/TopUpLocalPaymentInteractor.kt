package com.asfoundation.wallet.topup

import android.net.Uri
import com.appcoins.wallet.bdsbilling.WalletService
import com.appcoins.wallet.bdsbilling.repository.entity.Transaction
import com.asfoundation.wallet.billing.share.ShareLinkRepository
import com.asfoundation.wallet.ui.iab.InAppPurchaseInteractor
import io.reactivex.Observable
import io.reactivex.Single

class TopUpLocalPaymentInteractor(private val bdsShareLinkRepository: ShareLinkRepository,
                                  private val walletService: WalletService,
                                  private val inAppPurchaseInteractor: InAppPurchaseInteractor) {

  fun getPaymentLink(packageName: String, amount: String, currency: String,
                     method: String): Single<String> {
    return walletService.getWalletAddress()
        .flatMap {
          bdsShareLinkRepository.getLink(packageName, null, null, it, amount,
              currency, method)
        }
  }

  fun getTransaction(uri: Uri): Observable<Transaction> {
    return inAppPurchaseInteractor.getTransaction(uri.lastPathSegment)
        .filter { isEndingState(it.status, it.type) }
        .distinctUntilChanged { transaction -> transaction.status }
  }

  private fun isEndingState(status: Transaction.Status, type: String): Boolean {
    return (status == Transaction.Status.PENDING_USER_PAYMENT && type == "TOPUP") || (status == Transaction.Status.COMPLETED && (type == "INAPP" || type == "INAPP_UNMANAGED")) || status == Transaction.Status.FAILED || status == Transaction.Status.CANCELED || status == Transaction.Status.INVALID_TRANSACTION
  }

}
