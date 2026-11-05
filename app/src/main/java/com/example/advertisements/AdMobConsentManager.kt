package com.example.advertisements

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Requests the latest UMP consent state on every app launch.
 *
 * Ads remain unavailable until UMP says canRequestAds() is true.
 * On an update error, a previously valid UMP state may still permit requests;
 * the decision is always delegated to UMP rather than a locally cached flag.
 */
class AdMobConsentManager(activity: Activity) {
    private val appContext = activity.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)

    fun gatherConsent(onComplete: (canRequestAds: Boolean) -> Unit) {
        val requestParameters = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            appContextActivity(),
            requestParameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    appContextActivity()
                ) {
                    onComplete(consentInformation.canRequestAds())
                }
            },
            {
                // If the network update fails, UMP may still have a valid prior-session state.
                onComplete(consentInformation.canRequestAds())
            }
        )
    }

    fun isPrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            onComplete()
        }
    }

    // Retain only the Activity needed by UMP form presentation; never retain it in a singleton.
    private fun appContextActivity(): Activity = activityReference
        ?: throw IllegalStateException("Consent flow requires an active Activity")

    private var activityReference: Activity? = activity
}
