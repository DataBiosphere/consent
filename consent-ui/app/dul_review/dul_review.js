'use strict';

angular.module('myApp.dul_review', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/dul_review', {
    templateUrl: 'dul_review/dul_review.html',
    controller: 'dul_reviewCtrl'
  });
}])

.controller('dul_reviewCtrl', [function() {

}]);
