'use strict';

angular.module('myApp.access_review_results', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/access_review_results', {
    templateUrl: 'access_review_results/access_review_results.html',
    controller: 'access_review_resultsCtrl'
  });
}])

.controller('access_review_resultsCtrl', [function() {

}]);
