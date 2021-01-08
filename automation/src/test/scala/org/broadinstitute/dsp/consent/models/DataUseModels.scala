package  org.broadinstitute.dsp.consent.models

object DataUseModels {
    case class DataUse(
        hmbResearch: Option[Boolean],
        populationOriginsAncestry: Option[Boolean],
        commercialUse: Option[Boolean],
        pediatric: Option[Boolean],
        collaboratorRequired: Option[Boolean]
    )
}