package com.asfoundation.wallet.topup

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asf.wallet.R
import com.asfoundation.wallet.navigator.UriNavigator
import com.asfoundation.wallet.view.rx.RxAlertDialog
import dagger.android.support.DaggerFragment
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class TopUpLocalPaymentFragment : DaggerFragment(), TopUpLocalPaymentView {

  @Inject
  lateinit var interactor: TopUpLocalPaymentInteractor
  private lateinit var topUpActivity: TopUpActivityView
  private lateinit var navigator: TopUpLocalPaymentNavigator
  private lateinit var genericErrorDialog: RxAlertDialog
  private lateinit var networkErrorDialog: RxAlertDialog
  private lateinit var presenter: TopUpLocalPaymentPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navigator = TopUpLocalPaymentNavigator(activity as UriNavigator, topUpActivity)
    presenter =
        TopUpLocalPaymentPresenter(this, packageName, amount, currentCurrency, bonus,
            paymentMethod, interactor, navigator, CompositeDisposable(), savedInstanceState,
            AndroidSchedulers.mainThread(), Schedulers.io())
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    check(
        context is TopUpActivityView) { "Top Up Local payment fragment must be attached to TopUp Activity" }
    topUpActivity = context
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    genericErrorDialog = RxAlertDialog.Builder(context)
        .setMessage(R.string.unknown_error)
        .setPositiveButton(R.string.ok)
        .build()

    networkErrorDialog =
        RxAlertDialog.Builder(context)
            .setMessage(R.string.notification_no_network_poa)
            .setPositiveButton(R.string.ok)
            .build()
    presenter.present()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_top_up, container, false)
  }

  override fun showProcessingLoading() {
    topUpActivity.lockOrientation()
  }

  override fun hideLoading() {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun showCompletedPayment(bundle: Bundle) {
    topUpActivity.finish(bundle)
  }

  override fun showPendingUserPayment() {
    Log.d("TAG123", "HERE, Pendinging stuff")
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun showGenericError() {
    if (!genericErrorDialog.isShowing) {
      topUpActivity.lockOrientation()
      genericErrorDialog.show()
    }
  }

  override fun showNetworkError() {
    if (!networkErrorDialog.isShowing) {
      topUpActivity.lockOrientation()
      networkErrorDialog.show()
    }
  }

  override fun errorDismisses(): Observable<Any> {
    return Observable.merge<DialogInterface>(networkErrorDialog.dismisses(),
        genericErrorDialog.dismisses())
        .doOnNext { topUpActivity.unlockRotation() }
        .map { Any() }
  }

  override fun errorCancels(): Observable<Any> {
    return Observable.merge<DialogInterface>(networkErrorDialog.cancels(),
        genericErrorDialog.cancels())
        .doOnNext { topUpActivity.unlockRotation() }
        .map { Any() }
  }

  override fun errorPositiveClicks(): Observable<Any> {
    return Observable.merge<DialogInterface>(networkErrorDialog.positiveClicks(),
        genericErrorDialog.positiveClicks())
        .doOnNext { topUpActivity.unlockRotation() }
        .map { Any() }
  }

  val packageName: String by lazy {
    if (arguments!!.containsKey(PACKAGE_NAME)) {
      arguments!!.getString(PACKAGE_NAME)
    } else {
      throw IllegalArgumentException("Package name data not found")
    }
  }

  val paymentMethod: String by lazy {
    if (arguments!!.containsKey(PAYMENT_METHOD)) {
      arguments!!.getString(PAYMENT_METHOD)
    } else {
      throw IllegalArgumentException("payment method data not found")
    }
  }

  val amount: String by lazy {
    if (arguments!!.containsKey(AMOUNT)) {
      arguments!!.getString(AMOUNT)
    } else {
      throw IllegalArgumentException("amount not found")
    }
  }

  val currentCurrency: String by lazy {
    if (arguments!!.containsKey(PAYMENT_CURRENT_CURRENCY)) {
      arguments!!.getString(PAYMENT_CURRENT_CURRENCY)
    } else {
      throw IllegalArgumentException("current currency data not found")
    }
  }

  val bonus: String by lazy {
    if (arguments!!.containsKey(BONUS)) {
      arguments!!.getString(BONUS)
    } else {
      throw IllegalArgumentException("bonus data not found")
    }
  }

  companion object {

    private const val PACKAGE_NAME = "package_name"
    private const val PAYMENT_METHOD = "payment_method"
    private const val AMOUNT = "amount"
    private const val PAYMENT_CURRENT_CURRENCY = "currentCurrency"
    private const val BONUS = "bonus"

    fun newInstance(packageName: String, paymentMethod: String,
                    fiatValue: String, selectedCurrency: String,
                    bonusValue: String): TopUpLocalPaymentFragment {
      val bundle = Bundle()
      bundle.putString(PACKAGE_NAME, packageName)
      bundle.putString(PAYMENT_METHOD, paymentMethod)
      bundle.putSerializable(AMOUNT, fiatValue)
      bundle.putString(PAYMENT_CURRENT_CURRENCY, selectedCurrency)
      bundle.putString(BONUS, bonusValue)
      val fragment = TopUpLocalPaymentFragment()
      fragment.arguments = bundle
      return fragment
    }
  }
}
