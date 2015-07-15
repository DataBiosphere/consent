(function () {
    'use strict';

    angular.module('cmReviewResults')
        .controller('DulReviewResults', DulReviewResults);

    function DulReviewResults($scope, $state, cmVoteService, cmConsentService, cmElectionService, election){

        $scope.chartData = {
            'dul': [
                ['Results', 'Votes'],
                ['Yes', 0],
                ['No', 0],
                ['Pending', 0]
            ]
        };

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

        console.log(election.referenceId);
        $scope.consent = cmConsentService.findConsent(election.referenceId);
        $scope.positiveVote = positiveVote;
        $scope.logVote = logVote;
        $scope.sendReminder = sendReminder;
        // Final vote variables
        $scope.isFormDisabled = $scope.chartData.dul[3][1] > 0 || $scope.status != 'Open';
        $scope.finalRationale = election.finalRationale;
        $scope.status = election.status;
        $scope.finalVote = election.finalVote;

        $scope.$watch('chartData.dul', function(){
            if($scope.chartData.dul != 'undefined') {
                $scope.isFormDisabled = $scope.chartData.dul[3][1] > 0 || $scope.status != 'Open'
            } else {
                $scope.isFormDisabled = false;
            }
        });

        cmVoteService.getAllVotes(election.referenceId).then(function(data){
            $scope.voteList = chunk(data, 2);
            $scope.chartData = getGraphData(data);

        });

        function positiveVote() {
            election.finalRationale = '---';
        }

        function logVote() {
            election.status = 'Closed';
            election.finalRationale = $scope.finalRationale;
            election.finalVote = $scope.finalVote;
            cmElectionService.postElection(election).$promise.then(
                //success
                function(){
                    alert("Election updated.");
                    $state.go('chair_console');
                },
                //error
                function(){alert("Error updating election.")});
        }

        function sendReminder() {
            alert("Reminder sent.");
            $scope.reminder.text = 'Reminder sent';
            $scope.reminder.sent = true;
        }
    };


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
                    break;
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

