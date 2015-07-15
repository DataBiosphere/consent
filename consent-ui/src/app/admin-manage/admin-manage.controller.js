(function () {
    'use strict';

    angular.module('cmAdminManage')
        .controller('AdminManage', AdminManage);

    /* ngInject */
    function AdminManage($http, cmPaginatorService, $modal) {

        var lists = {'dul': []};

        var vm = this;
        vm.activePage = {'dul': 0};
        vm.currentPages = {'dul': []};
        vm.electionsList = {'dul': []};
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
        vm.openCreate = openCreate;
        vm.openCancel = openCancel;


        init();

        function init() {
            $http.get('json/cm_admin.json').then(function (response) {
                lists['dul'] = response.data['manage_dul'];
                vm.changePage('dul', 0);
            });
        }

        function openCreate () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/create-modal.html',
                controller: 'Modal',
                controllerAs: 'Modal'
            });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful
                //what to do if it was accepted
                }, function () {
                //what to do if the modal was canceled
            });
        }

        function openCancel () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/cancel-modal.html',
                controller: 'Modal',
                controllerAs: 'Modal'
            });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful
                //what to do if it was accepted
            }, function () {
                //what to do if the modal was canceled
            });
        }
    }

})();
