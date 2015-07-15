(function () {

    'use strict';

    angular.module('cmUserConsole')
        .controller('UserConsole', UserConsole);

    /* ngInject */
    function UserConsole(cmPaginatorService, cmPendingCaseService,$rootScope) {

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
              cmPendingCaseService.findConsentPendingCasesByUser(lists,$rootScope.currentUser.dacUserId,vm);
              cmPendingCaseService.findDataRequestPendingCasesByUser(lists,$rootScope.currentUser.dacUserId, vm);
        }
    }

})();
