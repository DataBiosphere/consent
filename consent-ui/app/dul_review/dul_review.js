'use strict';

angular.module('myApp.dul_review', ['ngResource', 'ngRoute' ])

.config(['$routeProvider', function($routeProvider) {
  $routeProvider.when('/dul_review', {
    templateUrl: 'dul_review/dul_review.html',
    controller: 'dul_reviewCtrl'
  });
}])

.controller('dul_reviewCtrl', ['$scope', '$resource', function($scope, $resource) {

	var ConsentAPI = $resource('http://localhost:8180/consent/:consentId', {consentId:'e7db828c-eef4-4340-86d0-75162010f1ee'});
    $scope.DULVoteStatus = "Pending";
    $scope.vote = "YES";

    $scope.DULVote = function( ) {
	    $scope.DULVoteStatus = "Check"; 

   	var consent = ConsentAPI.get({consentId:'e7db828c-eef4-4340-86d0-75162010f1ee'}, function() {
		alert(JSON.stringify(consent));
    });
    
};

 

}]);


