(function () {
    'use strict';

    angular.module('cmElection')
        .service('cmElectionService', cmElectionService);

    /* ngInject */
    function cmElectionService(ElectionResource, ElectionUpdateResource) {

        /**
         * Find data for the election related to the consentId sent as a parameter
         * @param consentId
         */
        function findElectionByConsentId(id){
            return ElectionResource.get({consentId: id});
        }

        /**
         * Post the election with the id sent as a parameter
         * @param consentId
         * @param electionId
         */
        function postElection(election){
            var postObject = new Object();
            postObject.finalVote = election.finalVote;
            postObject.status = election.status;
            postObject.finalRationale = election.finalRationale;
            return ElectionUpdateResource.update({consentId: election.referenceId, electionId: election.electionId}, postObject);
        }

        return{
            findElection: function(id) {
                return findElectionByConsentId(id);
            },

            postElection: function(election){
                return postElection(election);
            }
        }
    }

})();
