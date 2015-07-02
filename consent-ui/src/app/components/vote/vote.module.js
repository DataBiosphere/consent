(function () {
    'use strict';

    angular.module('cmVote', [])
    .factory('VoteResource', function($resource, apiUrl){
        return $resource(apiUrl+"consent/:consentId/vote/:voteId")})
    .factory('GetAllVotesResource', function($resource, apiUrl){
        return $resource(apiUrl+"consent/:consentId/vote");
    });

})();
