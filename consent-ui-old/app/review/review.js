'use strict';

angular.module('myApp.review', ['ngResource', 'ngAnimate', 'ui.router', 'cmFocus'])

        .controller('reviewCtrl', [function () {

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

        }]);
