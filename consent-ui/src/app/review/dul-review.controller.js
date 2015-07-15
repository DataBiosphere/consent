(function () {
    'use strict';

    angular.module('cmReview')
        .controller('DulReview', DulReview);


    function DulReview($scope, $state, $rootScope, USER_ROLES, vote, consent, election, cmVoteService)
    {
        $scope.consentDulUrl = consent.dataUseLetter;
        $scope.voteStatus = vote.vote;
        $scope.isFormDisabled = (election.status == 'Closed');
        $scope.rationale = vote.rationale;

        $scope.positiveVote = function(){
            $scope.rationale = '';
        }

        $scope.alerts = [
            {type: 'danger', msg: 'Please check your vote.'},
            {type: 'success', msg: 'Vote successfully logged.'}
        ];

        $scope.closeAlert = function (index) {
            $scope.alerts.splice(index, 1);
        };

        $scope.logVote = function() {
            if((vote.vote != $scope.voteStatus)||($scope.rationale != vote.rationale)){
                vote.vote = $scope.voteStatus;
                vote.rationale = $scope.rationale;
                var result;
                if(vote.createDate == null){
                    result = cmVoteService.postVote(consent.consentId, vote).$promise
                } else {
                    result = cmVoteService.updateVote(consent.consentId, vote).$promise
                }
                result.then(
                    //success
                    function(){
                        alert("Vote updated.");
                        if($rootScope.currentUser.memberStatus === USER_ROLES.chairperson){
                            $state.go('chair_console');
                        }else {
                            $state.go('user_console');
                        }

                        console.log();
                    },
                    //error
                    function(){alert("Error updating vote.")});
            } else  {
                alert("The vote hasn't changed. It won't be updated")
            }
        }
    }
})();
