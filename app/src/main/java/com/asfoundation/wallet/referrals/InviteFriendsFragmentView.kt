package com.asfoundation.wallet.referrals

import io.reactivex.Observable

interface InviteFriendsFragmentView {
  fun shareLinkClick(): Observable<Any>
  fun appsAndGamesButtonClick(): Observable<Any>
  fun showShare()
  fun navigateToAptoide()
  fun showNotificationCard(pendingAmount: String, symbol: String)
  fun changeBottomSheetState()
  fun hideNotificationCard()
}