package  org.broadinstitute.dsp.consent.models

object DataUseModels {
    case class DataUse(
        hmbResearch: Option[Boolean],
        populationOriginsAncestry: Option[Boolean],
        commercialUse: Option[Boolean],
        pediatric: Option[Boolean],
        collaboratorRequired: Option[Boolean]
    )
    object DataUseBuilder {
        def empty(): DataUse = {
            DataUse(
                hmbResearch = Some(false), 
                populationOriginsAncestry = Some(false), 
                commercialUse = Some(false), 
                pediatric = Some(false), 
                collaboratorRequired = Some(false)
            )
        }
    }
}
