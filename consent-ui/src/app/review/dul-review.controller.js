(function () {
    'use strict';

    angular.module('cmReview')
        .controller('DulReview', DulReview);


    function DulReview($scope, $modal, $state, $rootScope, USER_ROLES, vote, consent, election, cmVoteService)
    {
        $scope.consentDulUrl = consent.dataUseLetter;
        $scope.voteStatus = vote.vote;
        $scope.isFormDisabled = (election.status == 'Closed');
        $scope.rationale = vote.rationale;
        $scope.isNew = null;

        $scope.positiveVote = function(){
            $scope.rationale = null;
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
                    $scope.isNew = true;
                    result = cmVoteService.postVote(consent.consentId, vote).$promise
                } else {
                    $scope.isNew = false;
                    result = cmVoteService.updateVote(consent.consentId, vote).$promise
                }
                result.then(
                    //success
                    function(){
                        var modalInstance = $modal.open({
                            animation: false,
                            templateUrl: 'app/modals/confirmation-modal.html',
                            controller: 'Modal',
                            controllerAs: 'Modal',
                            scope: $scope
                        });
                        modalInstance.result.then(function () {
                            if($rootScope.currentUser.memberStatus === USER_ROLES.chairperson){
                                   $state.go('chair_console');
                            }else {
                                   $state.go('user_console');
                                  }
                            console.log();
                        });
                    },
                    //error
                    function(){alert("Error updating vote.")});
            } else  {
                alert("Error: Your vote hasn't been changed.");
            }
        }
    }
})();
