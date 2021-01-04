package org.broadinstitute.dsp.consent.services

import java.time.LocalDateTime
import org.broadinstitute.dsp.consent.models.ResearcherModels._
import org.broadinstitute.dsp.consent.models.UserModels._
import org.broadinstitute.dsp.consent.models.NihModels._

object NihService {
    val millisecondsPerDay: Long = {
        24 * 60 * 60 * 1000
    }
    def expirationCount(expDate: Long): Int = {
        var result = -1;
        if (expDate > 0) {
            val currentTime = System.currentTimeMillis
            val millisecondsPerDay = 24 * 60 * 60 * 1000
            var count: Int = ((expDate - currentTime) / millisecondsPerDay).toInt
            if (count > 0) {
                result = count
            }
        }

        result
    }

    def nextDate(): Long = {
        val currentTime = System.currentTimeMillis
        currentTime + (millisecondsPerDay * 30)
    }

    def parseProfile(info: ResearcherInfo, user: User, title: String): FireCloudProfile = {
        FireCloudProfile(
            Some(user.displayName.getOrElse("")),
            Some(user.displayName.getOrElse("")),
            Some(title),
            Some(user.email.getOrElse("")),
            Some(info.institution.getOrElse("n/a")),
            Some("n/a"),
            Some("n/a"),
            Some("n/a"),
            Some("n/a"),
            if (info.havePI.getOrElse("false") == "true") Some(info.piName.getOrElse("")) else if (info.isThePI.getOrElse("false") == "true") Some(user.displayName.getOrElse("")) else Some("n/a"),
            Some("n/a")
        )
    }
}