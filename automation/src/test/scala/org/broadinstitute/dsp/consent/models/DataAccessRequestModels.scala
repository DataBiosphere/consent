package org.broadinstitute.dsp.consent.models

import org.broadinstitute.dsp.consent.models.UserModels._
import org.broadinstitute.dsp.consent.models.DataSetModels._
import org.broadinstitute.dsp.consent.models.ElectionModels.Vote

object DataAccessRequestModels {
    case class OntologyEntry(
        id: Option[String],
        label: Option[String],
        definition: Option[String],
        synonyms: Option[Seq[String]]
    )

    case class Collaborator(
        approverStatus: Option[Boolean],
        email: Option[String],
        eraCommonsId: Option[String],
        name: Option[String],
        title: Option[String],
        uuid: Option[String]
    )

    case class DataAccessRequestDraft(
        userId: Int,
        datasets: Seq[DataSetEntry],
        datasetId: Seq[Int]
    )

    case class DataAccessRequest(
        id: Int,
        referenceId: String,
        data: Option[DataAccessRequestData] = None,
        draft: Option[Boolean] = None,
        userId: Int,
        createDate: Option[Long] = None,
        sortDate: Option[Long] = None,
        submissionDate: Option[Long] = None,
        updateDate: Option[Long] = None
    )

    case class DataAccessRequestData(
        referenceId: Option[String] = None,
        investigator: Option[String] = None,
        institution: Option[String] = None,
        department: Option[String] = None,
        division: Option[String] = None,
        address1: Option[String] = None,
        address2: Option[String] = None,
        city: Option[String] = None,
        zipCode: Option[String] = None,
        state: Option[String] = None,
        country: Option[String] = None,
        projectTitle: Option[String] = None,
        checkCollaborator: Option[Boolean] = None,
        researcher: Option[String] = None,
        userId: Option[Int] = None,
        isThePi: Option[String] = None,
        havePi: Option[String] = None,
        piEmail: Option[String] = None,
        profileName: Option[String] = None,
        pubmedId: Option[String] = None,
        scientificUrl: Option[String] = None,
        eraExpiration: Option[Boolean] = None,
        academicEmail: Option[String] = None,
        eraAuthorized: Option[Boolean] = None,
        nihUsername: Option[String] = None,
        linkedIn: Option[String] = None,
        orcid: Option[String] = None,
        researcherGate: Option[String] = None,
        rus: Option[String] = None,
        nonTechRus: Option[String] = None,
        diseases: Option[Boolean] = None,
        methods: Option[Boolean] = None,
        controls: Option[Boolean] = None,
        population: Option[Boolean] = None,
        other: Option[Boolean] = None,
        otherText: Option[String] = None,
        ontologies: Option[Seq[OntologyEntry]] = None,
        forProfit: Option[Boolean] = None,
        oneGender: Option[Boolean] = None,
        gender: Option[String] = None,
        pediatric: Option[Boolean] = None,
        illegalBehavior: Option[Boolean] = None,
        addiction: Option[Boolean] = None,
        sexualDiseases: Option[Boolean] = None,
        stigmatizedDiseases: Option[Boolean] = None,
        vulnerablePopulation: Option[Boolean] = None,
        populationMigration: Option[Boolean] = None,
        psychiatricTraits: Option[Boolean] = None,
        notHealth: Option[Boolean] = None,
        hmb: Option[Boolean] = None,
        status: Option[String] = None,
        poa: Option[Boolean] = None,
        datasets: Option[Seq[DataSetEntry]] = None,
        darCode: Option[String] = None,
        partialDarCode: Option[String] = None,
        restriction: Option[Any] = None,
        validRestriction: Option[Boolean] = None,
        translatedUseRestriction: Option[String] = None,
        createDate: Option[Long] = None,
        sortDate: Option[Long] = None,
        datasetIds: Option[Seq[Int]] = None,
        datasetDetail: Option[Seq[DataSetDetailEntry]] = None,
        anvilUse: Option[Boolean] = None,
        cloudUse: Option[Boolean] = None,
        localUse: Option[Boolean] = None,
        cloudProvider: Option[String] = None,
        cloudProviderType: Option[String] = None,
        cloudProviderDescription: Option[String] = None,
        geneticStudiesOnly: Option[Boolean] = None,
        irb: Option[Boolean] = None,
        irbDocumentLocation: Option[String] = None,
        irbDocumentName: Option[String] = None,
        irbProtocolExpiration: Option[String] = None,
        itDirector: Option[String] = None,
        signingOfficial: Option[String] = None,
        publication: Option[Boolean] = None,
        collaboration: Option[Boolean] = None,
        collaborationLetterLocation: Option[String] = None,
        collaborationLetterName: Option[String] = None,
        forensicActivities: Option[Boolean] = None,
        sharingDistribution: Option[Boolean] = None,
        labCollaborators: Option[Seq[Collaborator]] = None,
        internalCollaborators: Option[Seq[Collaborator]] = None,
        externalCollaborators: Option[Seq[Collaborator]] = None,
        dsAcknowledgement: Option[Boolean] = None,
        gsoAcknowledgement: Option[Boolean] = None,
        pubAcknowledgement: Option[Boolean] = None
    )

    case class DataAccessRequestManage(
        dar: Option[DataAccessRequest] = None,
        election: Option[ElectionModels.Election] = None,
        votes: Option[Seq[Vote]] = None,
        researcher: Option[User] = None,
        errors: Option[Seq[String]] = None
    )
}