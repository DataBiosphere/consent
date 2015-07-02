(function () {
    'use strict';

    angular.module('cmConsent', ['ngResource'])
        .factory('ConsentResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId")
        })
        .factory('ConsentDulResource', function($resource, apiUrl){
            return $resource(apiUrl+"consent/:consentId/dul")
        })
})();
