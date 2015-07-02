(function () {
    'use strict';

    angular.module('cmReviewResults')
        .controller('DulReview', ['$scope', 'cmVoteService', 'cmConsentService', 'cmElectionService', 'election', function($scope, cmVoteService, cmConsentService, cmElectionService, election){

            $scope.chartOptions = {
                'dul': {
                    pieHole: 0.4,
                    pieSliceTextStyle: {
                        color: 'white',
                        fontSize: 16
                    },
                    pieSliceText: 'none',
                    pieSliceBorderColor: 'transparent',
                    backgroundColor: 'transparent',
                    chartArea: {
                        left: 0,
                        top: 10,
                        right: 0,
                        bottom: 10,
                        width: '100%',
                        height: '85%'
                    },
                    height: 138,
                    slices: {
                        0: {color: '#C16B0C'},
                        1: {color: '#777777'},
                        2: {color: '#c9c9c9'}
                    },
                    legend: {
                        position: 'right',
                        textStyle: {
                            color: '#777777',
                            bold: true,
                            fontName: 'Roboto',
                            fontSize: 14
                        },
                        alignment: 'start'
                    },
                    tooltip: {
                        textStyle: {
                            color: 'black',
                            fontSize: 14
                        }
                    }
                }
            };

            $scope.consent = cmConsentService.findConsent(election.referenceId);
            $scope.positiveVote = positiveVote;
            $scope.logVote = logVote;
            $scope.sendReminder = sendReminder;

            // Final vote variables
            $scope.finalRationale = election.finalRationale;
            $scope.status = election.status;
            $scope.finalVote = election.finalVote;

            cmVoteService.getAllVotes(election.referenceId).then(function(data){
                $scope.voteList = chunk(data, 2);
                $scope.chartData = getGraphData(data);
                console.log($scope.chartData)
            });


            $scope.$watch('election.finalVote', function(finalVote) {
                console.log(finalVote);
            });

            function positiveVote() {
                election.finalRationale = '---';
            }

            function logVote() {
                election.status = 'Closed';
                election.finalRationale = $scope.finalRationale;
                election.finalVote = $scope.finalVote;
                cmElectionService.postElection(election);
                console.log("Updated Vote");
            }

            function sendReminder() {
                $scope.reminder.text = 'Reminder sent';
                $scope.reminder.sent = true;
            }
        }]);


    function chunk(arr, size) {
        var newArr = [];
        for (var i=0; i<arr.length; i+=size) {
            newArr.push(arr.slice(i, i+size));
        }
        return newArr;
    }

    function getGraphData(data){
        var yes = 0, no = 0, empty = 0;
        for (var i=0; i<data.length; i++) {
            switch(data[i].vote) {
                case true:
                    yes++;
                    break;
                case false:
                    no++;
                    break;
                default:
                    empty++;
            }
        }
        var chartData = {
            'dul': [
                ['Results', 'Votes'],
                ['Yes', yes],
                ['No', no],
                ['Pending', empty]
            ]
        };
        return chartData;
    }
})();

