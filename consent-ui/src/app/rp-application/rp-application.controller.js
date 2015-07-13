(function () {
    'use strict';

    angular.module('cmRPApplication')
        .controller('RPApplication', RPApplication);

    /* ngInject */
    function RPApplication($scope, $resource, $state) {

        var vm = this;

        vm.$state = $state;
        // we will store all of our form data in this object
        vm.formData = {};

        var Vote = $resource('http://localhost:8180/consent/:consentId/vote', {consentId: 'e7db828c-eef4-4340-86d0-75162010f1ee'});

        vm.dacUserId = 1;
        vm.DULVoteStatus = "Pending";
        vm.vote = false;

        vm.DULVote = function () {
            vm.DULVoteStatus = "Check";

            var vote = new Vote();
            vote.vote = $scope.vote;
            vote.dacUserId = $scope.dacUserId;
            vote.rationale = $scope.inputRationale;

            vote.$save({consentId: '94af0714-ae4a-493c-83f9-534b76412f46'}, function () {
                alert("volvi");
            });
        };

        // function to process the form
        vm.processForm = function () {
            alert('awesome!');
        };


    }

})();

