(function () {
    'use strict';

    angular.module('cmAdminUsers')
        .controller('AdminUsers', AdminUsers);

    /* ngInject */

    function AdminUsers($http, cmPaginatorService, $modal, $scope) {

        var lists = {'dul': [], 'conflict':[]};
        var list_max_items = 10;


        var vm = this;
        vm.activePage = {'dul': 0};
        vm.currentPages = {'dul': []};
        vm.electionsList = {'dul': []};

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
        vm.addUser = addUser;
        vm.editUser = editUser;


        init();

        /*****JSON*****/

        function init() {
            $http.get('json/cm_admin_users.json').then(function (response) {
                lists['dul'] = response.data['manage_users'];
                vm.changePage('dul', 0);
            });
        }

        /*****MODALS*****/

        function addUser () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/modal-users/add-user-modal.html',
                controller: 'ModalUsers',
                controllerAs: 'ModalUsers'
            });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful
                //what to do if it was accepted
            }, function () {
                //what to do if the modal was canceled
            });
        }

        function editUser () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/modal-users/edit-user-modal.html',
                controller: 'ModalUsers',
                controllerAs: 'ModalUsers'
            });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful
                //what to do if it was accepted
            }, function () {
                //what to do if the modal was canceled
            });
        }

    }

})();
