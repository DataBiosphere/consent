(function () {
    'use strict';

    angular.module('cmElection', ['ngResource'])
        .factory('ElectionResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId/election", {}, {
                get:{method: 'GET', params: {consentId: '@consentId', electionId: '@electionId'}},
                post: {method: 'POST', params: {consentId: '@consentId'}}});
        })
        .factory('ElectionUpdateResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId/election/:electionId", {}, {
                update:{method: 'PUT', params: {consentId: '@consentId', electionId: '@electionId'}}});
        })
        .factory('ElectionReviewConsent', function($resource, apiUrl){
            return $resource(apiUrl+"electionReview/consent/:consentId", {}, {
                get:{method: 'GET', params: {consentId: '@consentId'}}});
        })
        .factory('ElectionReviewedConsents', function($resource, apiUrl){
            return $resource(apiUrl+"consent/cases/closed", {},
                {
                    List: {method:'GET', isArray:true}
                });
        })
        .factory('ElectionReviewedDRs', function($resource, apiUrl){
            return $resource(apiUrl+"dataRequest/cases/closed", {},
                {
                    List: {method:'GET', isArray:true}
                });
        })

})();
