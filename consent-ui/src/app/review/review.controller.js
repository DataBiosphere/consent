(function () {
    'use strict';

    angular.module('cmReview')
        .controller('Review', Review);

    /* ngInject */
    function Review($scope) {

        var vm = this;
        vm.voteForm = {
            vote: undefined,
            rationale: ''
        };
        vm.positiveVote = positiveVote;
        vm.logVote = logVote;

        function positiveVote() {
            vm.voteForm.rationale = '';
        }

        $scope.alerts = [
            { type: 'danger', msg: 'Please check your vote.' },
            { type: 'success', msg: 'Vote successfully logged.' }
        ];

        $scope.closeAlert = function(index) {
            $scope.alerts.splice(index, 1);
        };

        function logVote() {

        }

    }

})();

//
//angular.module('ui.bootstrap.demo').controller('AlertDemoCtrl', function ($scope) {
//    $scope.alerts = [
//        { type: 'danger', msg: 'Oh snap! Change a few things up and try submitting again.' },
//        { type: 'success', msg: 'Well done! You successfully read this important alert message.' }
//    ];
//
//    $scope.addAlert = function() {
//        $scope.alerts.push({msg: 'Another alert!'});
//    };
//
//    $scope.closeAlert = function(index) {
//        $scope.alerts.splice(index, 1);
//    };
//});
