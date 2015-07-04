'use strict';

angular.module('myApp.dul_review', ['ngResource', 'ngAnimate','ui.router'])
.controller('dul_reviewCtrl', ['$scope', '$resource', function($scope, $resource) {

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

}]);


