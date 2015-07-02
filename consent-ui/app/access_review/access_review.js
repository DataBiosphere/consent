'use strict';

angular.module('myApp.access_review', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/access_review', {
    templateUrl: 'access_review/access_review.html',
    controller: 'access_reviewCtrl'
  });
}])

.controller('access_reviewCtrl', [function() {

}]);
