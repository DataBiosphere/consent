'use strict';

angular.module('myApp.chair_console', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/chair_console', {
    templateUrl: 'chair_console/chair_console.html',
    controller: 'chair_consoleCtrl'
  });
}])

.controller('chair_consoleCtrl', [function() {

}]);
