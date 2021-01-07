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
        var result: Int = -1;
        if (expDate > 0) {
            val currentTime: Long = System.currentTimeMillis
            val count: Int = ((expDate - currentTime) / millisecondsPerDay).toInt
            if (count > 0) {
                result = count
            }
        }

        result
    }

    def nextDate(): Long = {
        val currentTime: Long = System.currentTimeMillis
        currentTime + (millisecondsPerDay * 30)
    }

    def parseProfile(info: ResearcherInfo, user: User, title: String): FireCloudProfile = {
        FireCloudProfile(
            firstName = Some(user.displayName.getOrElse("")),
            lastName = Some(user.displayName.getOrElse("")),
            title = Some(title),
            contactEmail = Some(user.email.getOrElse("")),
            institute = Some(info.institution.getOrElse("n/a")),
            institutionalProgram = Some("n/a"),
            programLocationCity = Some("n/a"),
            programLocationState = Some("n/a"),
            programLocationCountry = Some("n/a"),
            pi = if (info.havePI.getOrElse("false") == "true") Some(info.piName.getOrElse("")) else if (info.isThePI.getOrElse("false") == "true") Some(user.displayName.getOrElse("")) else Some("n/a"),
            nonProfitStatus = Some("n/a")
        )
    }
}