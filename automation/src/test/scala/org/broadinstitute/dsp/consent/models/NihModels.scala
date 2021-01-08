package org.broadinstitute.dsp.consent.models

object NihModels {
    case class FireCloudProfile(
        firstName: Option[String] = None,
        lastName: Option[String] = None,
        title: Option[String] = None,
        contactEmail: Option[String] = None,
        institute: Option[String] = None,
        institutionalProgram: Option[String] = None,
        programLocationCity: Option[String] = None,
        programLocationState: Option[String] = None,
        programLocationCountry: Option[String] = None,
        pi: Option[String] = None,
        nonProfitStatus: Option[String] = None
    )

    case class NihUserAccount(
        linkedNihUsername: String,
        datasetPermissions: Option[Seq[String]] = None,
        linkExpireTime: String,
        status: Boolean
    )

    case class NihVerify(
        statusCode: Option[String]
    )
}