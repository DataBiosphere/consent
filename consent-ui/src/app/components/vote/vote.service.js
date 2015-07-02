(function () {
    'use strict';

    angular.module('cmVote')
        .service('cmVoteService', cmVoteService);

    /* ngInject */
    function cmVoteService(VoteResource, GetAllVotesResource) {

        /**
         * Find all votes for the election related to the consentId sent as a parameter
         * @param consentId
         */
        function findAllVotesByConsentId(id){
            return GetAllVotesResource.query({consentId: id}).$promise;
        }

        return{
            getAllVotes: function(id){
                return findAllVotesByConsentId(id);
            }
        }
    }

})();
