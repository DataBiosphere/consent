(function () {
    'use strict';

    angular.module('cmUser', [])
        .factory('GetUserResource', function($resource, apiUrl){
            return $resource(apiUrl+"dacuser/:email")
        })
        .factory('UserResource', function($resource, apiUrl){
            return $resource(apiUrl+"dacuser", {}, {
                post: {method: 'POST'},
                update: {method: 'PUT'},
                List: {method:'GET',isArray:true}
            })})
})();

