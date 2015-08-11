(function () {
    'use strict';

    angular.module('cmElection')
        .service('cmElectionService', cmElectionService);

    /* ngInject */
    function cmElectionService(ElectionResource, ElectionUpdateResource, ElectionReviewedConsents, ElectionReviewedDRs, ElectionReviewConsent) {

        /**
         * Find data for the election related to the consentId sent as a parameter
         * @param consentId
         */
        function findElectionByConsentId(id){
            return ElectionResource.get({consentId: id});
        }

        /**
         * Find all data needed to display an election Review for a consent
         * @param consentId
         */
        function findElectionReviewByConsentId(id){
            return ElectionReviewConsent.get({consentId: id});
        }

        /**
         * Update the election with the id sent as a parameter
         * @param consentId
         * @param electionId
         */
        function updateElection(election){
            var postObject = new Object();
            postObject.finalVote = election.finalVote;
            postObject.status = election.status;
            postObject.finalRationale = election.finalRationale;
            return ElectionUpdateResource.update({consentId: election.referenceId, electionId: election.electionId}, postObject);
        }

        /**
         * Create election for the specified consent id
         * @param consentId
         * @param electionId
         */
        function createElection(consentId){
            var postElection = new Object();
            postElection.status = 'Open';
            return ElectionResource.post({consentId: consentId}, postElection);
        }

        /**
         * Find closed elections for consents
         */
        function getReviewedConsents(){
            return ElectionReviewedConsents.List().$promise;
        }

        /**
         * Find closed elections for Data Requests
         */
        function getReviewedDRs(){
            return ElectionReviewedDRs.List().$promise;
        }

        return{
            findElection: function(id) {
                return findElectionByConsentId(id);
            },
            findElectionReview: function(id) {
                return findElectionReviewByConsentId(id);
            },
            updateElection: function(election){
                return updateElection(election);
            },

            createElection: function(consentId){
                return createElection(consentId);
            },
            findReviewedConsents: function() {
                return getReviewedConsents();
            },
            findReviewedDRs: function() {
                return getReviewedDRs();
            }
        }
    }

})();
