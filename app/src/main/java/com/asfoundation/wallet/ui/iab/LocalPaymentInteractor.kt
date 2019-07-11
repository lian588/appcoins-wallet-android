package com.asfoundation.wallet.ui.iab

import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.appcoins.wallet.bdsbilling.Billing
import com.appcoins.wallet.bdsbilling.WalletService
import com.appcoins.wallet.bdsbilling.repository.entity.Transaction
import com.appcoins.wallet.bdsbilling.repository.entity.Transaction.Status.*
import com.appcoins.wallet.billing.BillingMessagesMapper
import com.asfoundation.wallet.billing.partners.AddressService
import com.asfoundation.wallet.billing.purchase.InAppDeepLinkRepository
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.Function3

class LocalPaymentInteractor(private val deepLinkRepository: InAppDeepLinkRepository,
                             private val walletService: WalletService,
                             private val partnerAddressService: AddressService,
                             private val inAppPurchaseInteractor: InAppPurchaseInteractor,
                             private val billing: Billing,
                             private val billingMessagesMapper: BillingMessagesMapper
) {

  fun getPaymentLink(domain: String, skuId: String?,
                     originalAmount: String?, originalCurrency: String?,
                     paymentMethod: String, developerAddress: String): Single<String> {

    return walletService.getWalletAddress()
        .flatMap { address ->
          Single.zip(
              walletService.signContent(address),
              partnerAddressService.getStoreAddressForPackage(domain),
              partnerAddressService.getOemAddressForPackage(domain),
              Function3 { signature: String, storeAddress: String, oemAddress: String ->
                DeepLinkInformation(signature, storeAddress, oemAddress)
              })
              .flatMap {
                deepLinkRepository.getDeepLink(domain, skuId, address, it.signature, originalAmount,
                    originalCurrency, paymentMethod, developerAddress, it.storeAddress,
                    it.oemAddress)
              }
        }
  }

  fun getTransaction(uri: Uri): Observable<Transaction> {
    return inAppPurchaseInteractor.getTransaction(uri.lastPathSegment)
        .filter {
          isEndingState(it.status, it.type)
        }
        .distinctUntilChanged { transaction -> transaction.status }
  }

  private fun isEndingState(status: Transaction.Status, type: String): Boolean {
    Log.d("LocalPaymentInteractor", "TransactionStatus: $status")
    return (status == PENDING_USER_PAYMENT && type == "TOPUP") || (status == COMPLETED && (type == "INAPP" || type == "INAPP_UNMANAGED")) || status == FAILED || status == CANCELED || status == INVALID_TRANSACTION
  }

  fun getCompletePurchaseBundle(isInApp: Boolean, merchantName: String, sku: String?,
                                scheduler: Scheduler,
                                orderReference: String?, hash: String?): Single<Bundle> {
    Log.d("LocalPaymentInteractor", "CompletedPurchase")
    Log.d("LocalPaymentInteractor", "merchantName: " + merchantName + "sku: " + sku +
        "orderReference: " + orderReference + "hash: " + hash)
    return if (isInApp && sku != null) {
      Log.d("LocalPaymentInteractor", "Getting InApp Purchase...")
      billing.getSkuPurchase(merchantName, sku, scheduler)
          .map {
            if (it == null) Log.d("LocalPaymentInteractor", "Null Purchase") else {
              Log.d("LocalPaymentInteractor",
                  "Purchase: packageName: " + it.packageName.toString() + "status: " + it.status + "uid: " + it.uid + "productName: " + it.product.name + "signature: " + it.signature)
            }
            billingMessagesMapper.mapPurchase(it,
                orderReference)
          }
          .doOnSuccess { Log.d("LocalPaymentInteractor", "Purchase mapping success") }
          .doOnError { Log.d("LocalPaymentInteractor", "Purchase mapping error " + it.message) }
    } else {
      Log.d("LocalPaymentInteractor", "Getting Not InApp Purchase...")
      Single.just(billingMessagesMapper.successBundle(hash))
    }
  }

  private data class DeepLinkInformation(val signature: String, val storeAddress: String,
                                         val oemAddress: String)
}
