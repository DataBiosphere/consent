(function () {
    'use strict';

    angular.module('cmAdminUsers')
        .controller('AdminUsers', AdminUsers);

    /* ngInject */

    function AdminUsers(cmPaginatorService, $modal, cmUserService) {

        var lists = {'dul': []};
        var list_max_items = 10;


        var vm = this;
        vm.activePage = {'dul': 0};
        vm.currentPages = {'dul': []};
        vm.usersList = {'dul': []};


        vm.changePage = _.partial(cmPaginatorService.changePage,
            lists, list_max_items,
            {
                activePage: vm.activePage,
                currentPages: vm.currentPages,
                electionsList: vm.usersList
            }
        );
        vm.addUser = addUser;
        vm.editUser = editUser;
        init();

        function init() {
            cmUserService.findUsers().then(
                function (data) {
                    lists['dul'] = data;
                    vm.changePage('dul', 0);
                });
        }

        /*****MODALS*****/

        function addUser() {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/modal-users/add-user-modal.html',
                controller: 'ModalUsers',
                controllerAs: 'ModalUsers',
                resolve: {
                    user: new Object()
                }
            });

            modalInstance.result.then(function () {
                init();
            }, function () {
            });
        }

        function editUser(email) {
            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/modal-users/edit-user-modal.html',
                controller: 'ModalUsers',
                controllerAs: 'ModalUsers',
                resolve: {
                    user: function (cmUserService) {
                        return cmUserService.findUser(email);
                    }
                }
            });

            modalInstance.result.then(function (selectedItem) {

            }, function () {
                //what to do if the modal was canceled
            });
        }

    }

})();
