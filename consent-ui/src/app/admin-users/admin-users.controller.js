(function () {
    'use strict';

    angular.module('cmAdminUsers')
        .controller('AdminUsers', AdminUsers);

    /* ngInject */

    function AdminUsers($state, cmPaginatorService, $modal, $scope, cmUserService) {

        var lists = {'dul': []};
        var list_max_items = 10;


        var vm = this;
        vm.activePage = {'dul': 0};
        vm.currentPages = {'dul': []};
        vm.usersList = {'dul': []};

        // changePage function from the service with the first 2 parameters locked
        vm.changePage = _.partial(cmPaginatorService.changePage,
            // first parameter to lock from changePage
            lists, list_max_items,
            // second parameter to lock from changePage
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

        function addUser () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/modal-users/add-user-modal.html',
                controller: 'ModalUsers',
                controllerAs: 'ModalUsers',
                  resolve: {
                                user: new Object()
                           }
            });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful
              init();
            }, function () {
                //what to do if the modal was canceled
            });
        }

        function editUser (email) {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/modal-users/edit-user-modal.html',
                controller: 'ModalUsers',
                controllerAs: 'ModalUsers',
                resolve: {
                            user: function(cmUserService){
                                             return cmUserService.findUser(email);
                                     }
                                  }
            });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful

            }, function () {
                //what to do if the modal was canceled
            });
        }

    }

})();
