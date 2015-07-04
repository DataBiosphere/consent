'use strict';

// Declare app level module which depends on views, and components
angular.module('myApp', [
  'ngResource',
  'ngAnimate',
  'ui.router',
  'myApp.chair_console',
  'myApp.dac_console',
  'myApp.dul_review',
  'myApp.dul_review_results',
  'myApp.access_review',
  'myApp.access_review_results',
  'myApp.rp_application',  
  'myApp.version'
])
.config(function($stateProvider, $urlRouterProvider) {
    
	$urlRouterProvider.otherwise("/dac_console");
	
    $stateProvider
    
        // route to show our basic form (/form)
        .state('dac_console', {
            name: 'dac_console',
            url: '/dac_console',
            templateUrl: 'dac_console/dac_console.html',
            controller: 'dac_consoleCtrl'
})
	
	        // route to show our basic form (/form)
        .state('chair_console', {
            name: 'chair_console',
            url: '/chair_console',
            templateUrl: 'chair_console/chair_console.html',
            controller: 'chair_consoleCtrl'
})
	
	        // route to show our basic form (/form)
        .state('dul_review', {
            name: 'dul_review',
            url: '/dul_review',
            templateUrl: 'dul_review/dul_review.html',
            controller: 'dul_reviewCtrl'
})

	       // route to show our basic form (/form)
        .state('dul_review_results', {
            name: 'dul_review_results',
            url: '/dul_review_results',
            templateUrl: 'dul_review_results/dul_review_results.html',
            controller: 'dul_review_resultsCtrl'
})
	       // route to show our basic form (/form)
        .state('access_review', {
            name: 'access_review',
            url: '/access_review',
            templateUrl: 'access_review/access_review.html',
            controller: 'access_reviewCtrl'
})
	        // route to show our basic form (/form)
        .state('access_review_results', {
            name: 'access_review_results',
            url: '/access_review_results',
            templateUrl: 'access_review_results/access_review_results.html',
            controller: 'access_review_resultsCtrl'
})
	        // route to show our basic form (/form)
       .state('rp_application', {
            name: 'rp_application',
            url: '/rp_application',
            templateUrl: 'rp_application/rp_application.html',
            controller: 'rp_applicationCtrl'
        })
        
        // nested states 
        // each of these sections will have their own view
        // url will be nested (/form/profile)
        .state('rp_application.step1', {
            url: '/rp_application/step1',
            templateUrl: 'rp_application/rp_application_f1.html'
        })
        
        // url will be /form/interests
        .state('rp_application.step2', {
            url: '/rp_application/step2',
            templateUrl: 'rp_application/rp_application_f2.html'
        })
        
        // url will be /form/interests
        .state('rp_application.step3', {
            url: '/rp_application/step3',
            templateUrl: 'rp_application/rp_application_f3.html'
        })
        
        // url will be /form/payment
        .state('rp_application.step4', {
            url: '/rp_application/step4',
            templateUrl: 'rp_application/rp_application_f4.html'
        })
        
        ;
	
})
.config(['$httpProvider', function($httpProvider) {
        $httpProvider.defaults.useXDomain = true;
        delete $httpProvider.defaults.headers.common['X-Requested-With'];
    }
]);
