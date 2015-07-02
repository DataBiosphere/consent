'use strict';

angular.module('myApp.dul_review_results', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/dul_review_results', {
    templateUrl: 'dul_review_results/dul_review_results.html',
    controller: 'dul_review_resultsCtrl'
  });
}])

.controller('dul_review_resultsCtrl', [function() {

}]);
