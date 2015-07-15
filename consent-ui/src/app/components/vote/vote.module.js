(function () {
    'use strict';

    angular.module('cmVote', [])
    .factory('VoteResource', function($resource, apiUrl){
        return $resource(apiUrl+"consent/:consentId/vote/:voteId", {}, {
            get: {method: 'GET', params: {consentId: '@consentId', voteId: '@voteId'}, isArray: false},
            post: {method: 'POST', params: {consentId: '@consentId', voteId: '@voteId'}},
            update: {method: 'PUT', params: {consentId: '@consentId', voteId: '@voteId'}}
        })})
    .factory('GetAllVotesResource', function($resource, apiUrl){
        return $resource(apiUrl+"consent/:consentId/vote");
    });

})();
