(function () {
    'use strict';

    angular.module('cmReviewedCases')
        .controller('ReviewedCases', ReviewedCases);

    /* ngInject */
    function ReviewedCases($http, cmPaginatorService, $scope, $filter, $timeout) {

        var lists = {'dul': [], 'access':[]};
        var list_max_items = 5;


        var vm = this;
        vm.activePage = {'dul': 0, 'access': 0};
        vm.currentPages = {'dul': [], 'access': []};
        vm.electionsList = {'dul': [], 'access': []};

        // changePage function from the service with the first 2 parameters locked
        vm.changePage = _.partial(cmPaginatorService.changePage,
            // first parameter to lock from changePage
            lists, list_max_items,
            // second parameter to lock from changePage
            {
                activePage: vm.activePage,
                currentPages: vm.currentPages,
                electionsList: vm.electionsList
            }
        );

        init();

        /*****JSON*****/

        function init() {
            $http.get('json/cm_reviewed_cases.json').then(function (response) {
                lists['dul'] = response.data['dul'];
                lists['access'] = response.data['access'];
                vm.changePage('dul', 0);
                vm.changePage('access', 0);
            });
        }
    }

})();
