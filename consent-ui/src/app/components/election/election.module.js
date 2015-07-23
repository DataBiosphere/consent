(function () {
    'use strict';

    angular.module('cmElection', ['ngResource'])
        .factory('ElectionResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId/election", {}, {
                get:{method: 'GET', params: {consentId: '@consentId', electionId: '@electionId'}}});
        })
        .factory('ElectionUpdateResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId/election/:electionId", {}, {
                update:{method: 'PUT', params: {consentId: '@consentId', electionId: '@electionId'}}});
        })
        .factory('ElectionReviewConsent', function($resource, apiUrl){
            return $resource(apiUrl+"electionReview/consent/:consentId", {}, {
                get:{method: 'GET', params: {consentId: '@consentId'}}});
        })
})();
