(function () {
    'use strict';

    angular.module('cmUser', ['ngResource'])
    .factory('UserResource', function($resource, apiUrl){
        return $resource(apiUrl+"dacuser/:email");
    });
})();
