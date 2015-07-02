(function () {
    'use strict';

    angular.module('cmPendingCase')
        .service('cmPendingCaseService', cmPendingCaseService);

    /* ngInject */
    function cmPendingCaseService() {

        var cmPendingCaseService = this;
        cmPendingCaseService.findDataRequestPendingCasesByChairPerson = findDataRequestPendingCasesByChairPerson;
        cmPendingCaseService.findConsentPendingCasesByChairPerson = findConsentPendingCasesByChairPerson;
        return cmPendingCaseService;

        /**
         * Finding data request pending cases for chairperson for the specified user id
         * @param userId
         * @param lists
         * @param vm
         */
        function findDataRequestPendingCasesByChairPerson($http,lists, userId, vm, url){
            $http.get(url + 'dataRequest/pendingCases/' + userId)
                .then(function (response) {
                    lists['access'] = response.data;
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
         * Finding consent pending cases for chairperson for the specified user id
         * @param userId
         * @param lists
         * @param vm
         */
        function findConsentPendingCasesByChairPerson($http,lists, userId, vm, url){
            $http.get(url + 'consent/pendingCases/' + userId)
                .then(function (response) {
                    lists['dul'] = response.data;
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
    }

})();
