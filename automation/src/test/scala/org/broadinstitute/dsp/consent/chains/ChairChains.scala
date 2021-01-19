package org.broadinstitute.dsp.consent.chains

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import org.broadinstitute.dsp.consent.{TestConfig}
import org.broadinstitute.dsp.consent.requests.Requests
import spray.json._
import org.broadinstitute.dsp.consent.models.MatchModels._
import org.broadinstitute.dsp.consent.models.ElectionModels._
import org.broadinstitute.dsp.consent.models.JsonProtocols
import io.netty.handler.codec.http.HttpResponseStatus._

object ChairChains {
    def loginToConsole(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.User.me(OK.code, additionalHeaders)
        )
        .pause(TestConfig.defaultPause)
        .exec(
            Requests.PendingCases.chairConsole(OK.code, "${dacUserId}", additionalHeaders)
        )
    }

    def reviewLoad(additionalHeaders: Map[String, String]): ChainBuilder = {
        exec(
            Requests.Match.findMatch(OK.code, "${consentId}", "${darReferenceId}", additionalHeaders)
        )
    }

    def submitVote(additionalHeaders: Map[String, String]): ChainBuilder = {
        exitBlockOnFail {
            exec(
                reviewLoad(additionalHeaders)
            )
            .pause(TestConfig.defaultPause)
            .exec(
                AccessReviewChains.submitVote(AccessReviewChains.electionChairpersonVotes, additionalHeaders)
            )
            .exec(
                AccessReviewChains.submitVote(AccessReviewChains.electionFinalVotes, additionalHeaders)
            )
            .exec { session =>
                implicit val matchFormat: JsonProtocols.matchFormat.type = JsonProtocols.matchFormat
                try {
                    val matchStr: String = session(Requests.Match.findMatchResponse).as[String]
                    val matchObj: Match = matchStr.parseJson.convertTo[Match] 
                    session.set("isMatch", matchObj.`match`)
                } catch {
                    case _: Throwable => session.set("isMatch", false)
                }
            }
            .exec(
                AccessReviewChains.submitVote(AccessReviewChains.electionAgreementVotes, additionalHeaders, "${isMatch}")
            )
            .exec { session =>
                implicit val electionFormat: JsonProtocols.electionFormat.type = JsonProtocols.electionFormat

                val accessElection: Election = session(AccessReviewChains.accessElection).as[Election]
                
                val newElection: Election = Election(
                    electionId = accessElection.electionId,
                    electionType = accessElection.electionType,
                    finalVote = Some(true),
                    status = Some(Status.CLOSED),
                    createDate = accessElection.createDate,
                    lastUpdate = accessElection.lastUpdate,
                    finalVoteDate = accessElection.finalVoteDate,
                    referenceId = accessElection.referenceId,
                    finalRationale = Some(""),
                    finalAccessVote = Some(true),
                    dataSetId = accessElection.dataSetId,
                    displayId = accessElection.displayId,
                    dataUseLetter = accessElection.dataUseLetter,
                    dulName = accessElection.dulName,
                    archived = accessElection.archived,
                    version = accessElection.version,
                    consentGroupName = accessElection.consentGroupName,
                    projectTitle = accessElection.projectTitle
                )

                session.set("closeElectionBody", newElection.toJson.compactPrint)
            }
            .exec(
                Requests.Election.updateElection(OK.code, "${darElectionId}", "${closeElectionBody}", additionalHeaders)
            )
        }
    }
}
