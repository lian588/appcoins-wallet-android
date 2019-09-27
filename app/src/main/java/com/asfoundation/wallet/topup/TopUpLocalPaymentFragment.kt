package com.asfoundation.wallet.topup

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asf.wallet.R
import com.asfoundation.wallet.navigator.UriNavigator
import dagger.android.support.DaggerFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class TopUpLocalPaymentFragment : DaggerFragment(), TopUpLocalPaymentView {

  @Inject
  lateinit var interactor: TopUpLocalPaymentInteractor
  private lateinit var topUpActivity: TopUpActivityView
  private lateinit var navigator: TopUpLocalPaymentNavigator
  lateinit var presenter: TopUpLocalPaymentPresenter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    navigator = TopUpLocalPaymentNavigator(activity as UriNavigator, topUpActivity)
    presenter =
        TopUpLocalPaymentPresenter(this, packageName, amount, currentCurrency, paymentMethod,
            interactor, navigator, CompositeDisposable(), savedInstanceState,
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
    presenter.present()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_top_up, container, false)
  }

  override fun showProcessingLoading() {
    Log.d("TAG123", "HERE, Processing stuff")
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun lockRotation() {
    topUpActivity.lockOrientation()
  }

  override fun hideLoading() {
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun showCompletedPayment() {
    Log.d("TAG123", "HERE, Completing stuff")
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun showPendingUserPayment() {
    Log.d("TAG123", "HERE, Pendinging stuff")
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun showError() {
    Log.d("TAG123", "HERE, Showing error stuff")
    TODO(
        "not implemented") //To change body of created functions use File | Settings | File Templates.
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
