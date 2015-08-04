(function () {
    'use strict';

    angular.module('cmAdminConsole')
        .controller('AdminConsole', AdminConsole);

    /* ngInject */
    function AdminConsole($http, $modal) {

        var vm = this;
        vm.addDul = addDul;
        vm.addUser = addUser;

        function addDul () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/add-dul-modal.html',
                controller: 'Modal',
                controllerAs: 'Modal'
            });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful
                //what to do if it was accepted
            }, function () {
                //what to do if the modal was canceled
            });
        }

        function addUser () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modal-users/add-user-modal.html',
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
