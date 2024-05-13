package com.kylecorry.trail_sense_comms.ui

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.exceptions.*
import com.kylecorry.trail_sense_comms.R
import com.kylecorry.trail_sense_comms.infrastructure.errors.FragmentDetailsBugReportGenerator

object ExceptionHandler {

    fun initialize(context: Context) {
        val handler = CustomExceptionHandler(context)
        handler.bind()

        CustomExceptionHandler.error?.let {
            Log.e("Trail Sense", it)
            val message = BugReportEmailMessage(
                context.getString(R.string.error_occurred),
                context.getString(R.string.error_occurred_message) + if (BuildConfig.DEBUG) {
                    "\n\n$it"
                } else {
                    ""
                },
                context.getString(R.string.email_developer),
                context.getString(android.R.string.cancel),
                context.getString(R.string.email),
                "Error in ${context.getString(R.string.app_name)}"
            )
            Alerts.dialog(
                context,
                message.title,
                message.description,
                okText = message.emailAction,
                cancelText = message.ignoreAction
            ) { cancelled ->
                if (!cancelled) {
                    val intent = Intents.email(
                        message.emailAddress,
                        message.emailSubject,
                        it
                    )

                    context.startActivity(intent)
                }
            }
        }

        CustomExceptionHandler.error = null
    }

}