'use strict';

angular.module('myApp.dac_console', ['ngRoute'])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/dac_console', {
    templateUrl: 'dac_console/dac_console.html',
    controller: 'dac_consoleCtrl'
  });
}])

.controller('dac_consoleCtrl', [function() {

}]);
