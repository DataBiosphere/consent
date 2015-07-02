(function () {
    'use strict';

    angular.module('cmElection', ['ngResource'])
        .factory('ElectionResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId/election")
        })
        .factory('ElectionUpdateResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId/election/:electionId", {}, {
                update:{method: 'PUT', params: {consentId: '@consentId', electionId: '@electionId'}}});
        })
})();
