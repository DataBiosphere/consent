(function () {
    'use strict';

    angular.module('cmReview')
        .controller('Review', Review);

    /* ngInject */
    function Review() {

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

        function logVote() {

        }

    }

})();
