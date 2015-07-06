(function () {

    'use strict';

    angular.module('cmUserConsole')
        .controller('UserConsole', UserConsole);

    /* ngInject */
    function UserConsole($http, cmPaginatorService) {

        var lists = {'dul': [], 'access': []};

        var vm = this;
        vm.activePage = {'dul': 0, 'access': 0};
        vm.currentPages = {'dul': [], 'access': []};
        vm.electionsList = {'dul': [], 'access': []};
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
                lists['dul'] = response.data['dul_review'];
                lists['access'] = response.data['access_review'];
                vm.changePage('dul', 0);
                vm.changePage('access', 0);
            });
        }
    }

})();
