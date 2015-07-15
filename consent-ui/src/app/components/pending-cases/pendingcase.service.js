(function () {
    'use strict';

    angular.module('cmPendingCase')
        .service('cmPendingCaseService', cmPendingCaseService);

    /* ngInject */
    function cmPendingCaseService(DataRequestPendingCases, ConsentPendingCases, ConsentSummaryCases, DataRequestSummaryCases, ConsentSummaryFile) {

        /**
         * Finding data request pending cases for the specified user id
         * @param userId
         * @param lists
         * @param vm
         */
        function findDataRequestPendingCasesByUser(lists, userId, vm){
            DataRequestPendingCases.List({userId: userId}).$promise.then(
                function(data){
                    lists['access'] = data;
                    vm.changePage('access', 0);
                    lists['access'].forEach(
                        function countCollectVotes(access) {
                            if (access.alreadyVoted == false) {
                                vm.totalAccessPendingVotes += 1;
                            }
                        }
                    );
                });
         }
        /**
         * Finding consent pending cases  for the specified user id
         * @param userId
         * @param lists
         * @param vm
         */
        function findConsentPendingCasesByUser(lists, userId, vm){
            ConsentPendingCases.List({userId: userId}).$promise.then(
                function(data) {
                    lists['dul'] = data;
                    vm.changePage('dul', 0);
                    lists['dul'].forEach(
                        function countCollectVotes(dul) {
                            if (dul.alreadyVoted == false) {
                                vm.totalDulPendingVotes += 1;
                            }
                        }
                    );
                });
        }

        /**
         * Finding consent summary
         */
        function findSummary(data, vm){
          ConsentSummaryCases.get().$promise.then(function(access) {
                // reviewed cases
                data['dulTotal'][1][1] = access.reviewedPositiveCases + access.reviewedNegativeCases;
                // pending cases
                data['dulTotal'][2][1] = access.pendingCases;
                // positive cases
                data['dulReviewed'][1][1] = access.reviewedPositiveCases;
                // negative cases
                data['dulReviewed'][2][1] = access.reviewedNegativeCases;
                // find data request summary
                DataRequestSummaryCases.get().$promise.then(function(dul) {
                    // reviewed cases
                    data['accessTotal'][1][1] = dul.reviewedPositiveCases + dul.reviewedNegativeCases;
                    // pending cases
                    data['accessTotal'][2][1] = dul.pendingCases;
                    // positive cases
                    data['accessReviewed'][1][1] = dul.reviewedPositiveCases;
                    // negative cases
                    data['accessReviewed'][2][1] = dul.reviewedNegativeCases;
                    vm.chartData = data;

                });
            });
        }


        return{
            findDataRequestPendingCasesByUser: function(lists, userId, vm) {
                return findDataRequestPendingCasesByUser(lists, userId, vm);
            },
            findConsentPendingCasesByUser: function(lists, userId, vm){
                return findConsentPendingCasesByUser(lists, userId, vm);
            },
            findSummary: function(data, vm){
                return findSummary(data, vm);
            }
        }
    }

})();
