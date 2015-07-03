'use strict';

angular.module('myApp.rp_application', ['ngResource', 'ui.router' ])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/rp_application', {
    templateUrl: 'rp_application/rp_application.html',
    controller: 'rp_applicationCtrl'
  });
}])

.config(function($stateProvider, $urlRouterProvider) {
    
	$urlRouterProvider.otherwise("/dac_consent");
	
    $stateProvider
    
        // route to show our basic form (/form)
        .state('rp_application', {
        	name: 'rp_application',
            url: '/rp_application',
            templateUrl: 'rp_application.html',
            controller: 'rp_applicationCtrl'
        })
        
        // nested states 
        // each of these sections will have their own view
        // url will be nested (/form/profile)
        .state('rp_application-step1', {
            url: '/rp_application/step1',
            templateUrl: 'rp_application_f1.html'
        })
        
        // url will be /form/interests
        .state('rp_application-step2', {
            url: '/rp_application/step2',
            templateUrl: 'rp_application/rp_application_f2.html'
        })
        
        // url will be /form/interests
        .state('rp_application-step3', {
            url: '/rp_application/step3',
            templateUrl: 'rp_application_f3.html'
        })
        
        // url will be /form/payment
        .state('rp_application.step4', {
            url: '/rp_application/step4',
            templateUrl: 'rp_application_f4.html'
        });
       
})

.controller('rp_applicationCtrl', ['$scope', '$resource', function($scope, $resource) {

	// we will store all of our form data in this object
    $scope.formData = {};
    
	var Vote = $resource('http://localhost:8180/consent/:consentId/vote', {consentId:'e7db828c-eef4-4340-86d0-75162010f1ee'});

	$scope.dacUserId = 1;
    $scope.DULVoteStatus = "Pending";
    $scope.vote = false;

    $scope.DULVote = function( ) {
	    $scope.DULVoteStatus = "Check"; 

	    var vote = new Vote();
	    vote.vote = $scope.vote;
	    vote.dacUserId = $scope.dacUserId;
	    vote.rationale = $scope.inputRationale;
   	
	    vote.$save({consentId:'94af0714-ae4a-493c-83f9-534b76412f46'}, function() {
	    		alert("volvi");
	    });
    };
    
    // function to process the form
    $scope.processForm = function() {
        alert('awesome!');  
    };


}]);


