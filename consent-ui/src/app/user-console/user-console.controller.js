(function () {

    'use strict';

    angular.module('cmUserConsole')
        .controller('UserConsole', UserConsole);

    /* ngInject */

    function UserConsole(apiUrl, $http, cmPaginatorService, cmPendingCaseService) {


        var lists = {'dul': [], 'access': []};

        var vm = this;
        vm.activePage = {'dul': 0, 'access': 0};
        vm.currentPages = {'dul': [], 'access': []};
        vm.electionsList = {'dul': [], 'access': []};
        vm.totalDulPendingVotes = 0;
        vm.totalAccessPendingVotes = 0;
        // changePage function from the service with the first 2 parameters locked
        vm.changePage = _.partial(cmPaginatorService.changePage,
            // first parameter to lock from changePage
            lists,
            // second parameter to lock from changePage
            {
                activePage: vm.activePage,
                currentPages: vm.currentPages,
                electionsList: vm.electionsList
            }
        );

        init();

        function init() {
            $http.get('json/cm_user.json').then(function (response) {
                cmPendingCaseService.findConsentPendingCasesByChairPerson($http,lists,2,vm, apiUrl);
                cmPendingCaseService.findDataRequestPendingCasesByChairPerson($http, lists, 2, vm, apiUrl);
            });
        }
    }

})();
