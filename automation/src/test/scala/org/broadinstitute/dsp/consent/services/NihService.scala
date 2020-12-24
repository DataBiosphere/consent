package org.broadinstitute.dsp.consent.services

import java.time.LocalDateTime
import org.broadinstitute.dsp.consent.models.{ResearcherInfo, FireCloudProfile, User}

object NihService {
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

    def parseProfile(info: ResearcherInfo, user: User, title: String): FireCloudProfile = {
        FireCloudProfile(
            Some(user.displayName),
            Some(user.displayName),
            Some(title),
            Some(user.email),
            Some(info.institution.getOrElse("n/a")),
            Some("n/a"),
            Some("n/a"),
            Some("n/a"),
            Some("n/a"),
            if (info.havePI.getOrElse("false") == "true") Some(info.piName.getOrElse("")) else if (info.isThePI.getOrElse("false") == "true") Some(user.displayName) else Some("n/a"),
            Some("n/a")
        )
    }
}