package com.asfoundation.wallet.advertise

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.appcoins.advertising.AppCoinsAdvertising
import com.asf.wallet.R

internal class AppCoinsAdvertisingBinder(
    private val packageManager: PackageManager,
    private val campaignInteract: CampaignInteract,
    private val notificationManager: NotificationManager,
    private val headsUpNotificationBuilder: NotificationCompat.Builder,
    private val context: Context) :
    AppCoinsAdvertising.Stub() {

  companion object {
    internal const val RESULT_OK = 0 // success
    internal const val RESULT_SERVICE_UNAVAILABLE = 1 // The network connection is down
    internal const val RESULT_CAMPAIGN_UNAVAILABLE = 2 // The campaign is not available

    internal const val RESPONSE_CODE = "RESPONSE_CODE"
    internal const val CAMPAIGN_ID = "CAMPAIGN_ID"
  }

  override fun getAvailableCampaign(): Bundle {
    val uid = Binder.getCallingUid()
    val pkg = packageManager.getNameForUid(uid)
    val pkgInfo = packageManager.getPackageInfo(pkg, 0)
    return campaignInteract.getCampaign(pkg, pkgInfo.versionCode)
        .doOnSuccess { handlePoaLimit(it, pkgInfo) }
        .map { mapCampaignDetails(it) }
        .blockingGet()
  }

  private fun handlePoaLimit(campaign: CampaignDetails, pkgInfo: PackageInfo) {
    if (campaign.hasReachedPoaLimit()) {
      if (campaignInteract.hasSeenPoaNotificationTimePassed()) {
        showNotification(campaign, pkgInfo)
        campaignInteract.saveSeenPoaNotification()
      }
    } else {
      campaignInteract.clearSeenPoaNotification()
    }
  }

  private fun showNotification(campaign: CampaignDetails, packageInfo: PackageInfo?) {
    val minutesRemaining = "%02d".format(campaign.minutesRemaining)

    val message = context.getString(R.string.notification_poa_limit_reached,
        campaign.hoursRemaining.toString(), minutesRemaining)
    val notificationBuilder =
        headsUpNotificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentText(message)
    packageInfo?.let {
      notificationBuilder.setContentTitle(packageManager.getApplicationLabel(it.applicationInfo))
    }
    notificationManager.notify(WalletPoAService.SERVICE_ID, notificationBuilder.build())
  }

  private fun mapCampaignDetails(details: CampaignDetails): Bundle {
    val bundle = Bundle()
    bundle.putInt(RESPONSE_CODE, mapCampaignAvailability(details.responseCode))
    bundle.putString(CAMPAIGN_ID, details.campaignId)
    return bundle
  }

  private fun mapCampaignAvailability(availabilityType: Advertising.CampaignAvailabilityType)
      : Int =
      when (availabilityType) {
        Advertising.CampaignAvailabilityType.AVAILABLE -> RESULT_OK
        Advertising.CampaignAvailabilityType.UNAVAILABLE -> RESULT_CAMPAIGN_UNAVAILABLE
        else -> RESULT_SERVICE_UNAVAILABLE

      }

}