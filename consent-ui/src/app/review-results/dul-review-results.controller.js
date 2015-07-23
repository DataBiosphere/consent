(function () {
    'use strict';

    angular.module('cmReviewResults')
        .controller('DulReviewResults', DulReviewResults);

    function DulReviewResults($scope, $state, cmElectionService, electionReview){

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
        $scope.election = electionReview.election;

        if(electionReview.election.finalRationale === 'null'){
            $scope.election.finalRationale = '';
        }

        $scope.dul = electionReview.dataUseLetter;
        $scope.positiveVote = positiveVote;
        $scope.logVote = logVote;
        $scope.sendReminder = sendReminder;
        // Final vote variables
        $scope.isFormDisabled = $scope.chartData.dul[3][1] > 0 || $scope.status != 'Open';
        $scope.finalRationale = electionReview.election.finalRationale;
        $scope.status = electionReview.election.status;
        $scope.finalVote = electionReview.election.finalVote;
        $scope.voteList = chunk(electionReview.reviewVote, 2);
        $scope.chartData = getGraphData(electionReview.reviewVote);



        $scope.$watch('chartData.dul', function(){
            if($scope.chartData.dul != 'undefined') {
                $scope.isFormDisabled = $scope.chartData.dul[3][1] > 0 || $scope.status != 'Open'
            } else {
                $scope.isFormDisabled = false;
            }
        });

        function positiveVote() {
            $scope.election.finalRationale = null;
        }

        function logVote() {
            $scope.election.status = 'Closed';
            cmElectionService.postElection($scope.election).$promise.then(
                //success
                function(){
                    alert("Final Vote updated")
                    $state.go('chair_console');
                },
                //error
                function(){ alert("Error while updating final vote.");});
        }

        function sendReminder(voteId) {
            alert("Reminder sent to: " + getEmailFromVoteList(voteId, electionReview.reviewVote));
        }

        function getEmailFromVoteList(voteId, reviewVote){
            for (var i=0; i<reviewVote.length; i++) {
                if (reviewVote[i].vote.voteId === voteId){
                    return reviewVote[i].email;
                }
            }
        }


    };

    function chunk(arr, size) {
        var newArr = [];
        for (var i=0; i<arr.length; i+=size) {
            newArr.push(arr.slice(i, i+size));
        }
        return newArr;
    }

    function getGraphData(reviewVote){
        var yes = 0, no = 0, empty = 0;
        for (var i=0; i<reviewVote.length; i++) {
            switch(reviewVote[i].vote.vote) {
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

