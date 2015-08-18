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

        /**
         * Find a vote for the election related to the consentId sent as a parameter
         * @param consentId(referenceId)
         * @param voteId
         */
        function findVote(referenceId, voteId){
            return VoteResource.get({consentId: referenceId, voteId: voteId}).$promise;
        }

        /**
         * Post the vote with the id sent as a parameter
         * @param vote, with the voteId included
         */
        function putVote(consentId, vote){
            var postObject = {};
            postObject.vote = vote.vote;
            postObject.dacUserId = vote.dacUserId;
            postObject.rationale = vote.rationale;
            return VoteResource.update({consentId: consentId, voteId: vote.voteId}, postObject);
        }

        /**
         * Post the vote with the id sent as a parameter
         * @param vote, with the voteId included
         */
        function postVote(consentId, vote){
            var postObject = {};
            postObject.vote = vote.vote;
            postObject.dacUserId = vote.dacUserId;
            postObject.rationale = vote.rationale;
            return VoteResource.post({consentId: consentId, voteId: vote.voteId}, postObject);
        }

        return{
            getAllVotes: function(id){
                return findAllVotesByConsentId(id);
            },
            getVote: function(referenceId, voteId){
                return findVote(referenceId, voteId);
            },
            postVote: function(consentId, vote){
                return postVote(consentId, vote);
            },
            updateVote: function(consentId, vote){
                return putVote(consentId, vote);
            }
        };
    }

})();
