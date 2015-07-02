'use strict';

// Declare app level module which depends on views, and components
angular.module('myApp', [
  'ngResource',
  'ngRoute',
  'myApp.chair_console',
  'myApp.dac_console',
  'myApp.dul_review',
  'myApp.dul_review_results',
  'myApp.access_review',
  'myApp.access_review_results',
  'myApp.version'
]).
config(['$routeProvider', function($routeProvider) {
  $routeProvider.otherwise({redirectTo: '/dac_console'});
}]).
config(['$httpProvider', function($httpProvider) {
        $httpProvider.defaults.useXDomain = true;
        delete $httpProvider.defaults.headers.common['X-Requested-With'];
    }
]);
