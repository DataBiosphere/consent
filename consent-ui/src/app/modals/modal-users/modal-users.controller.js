(function () {
    'use strict';

    angular.module('cmModalUsers')
        .controller('ModalUsers', ModalUsers);

    /* ngInject */
    function ModalUsers($modalInstance, $scope) {

        var vm = this;

        vm.ok = function () {
            $modalInstance.close();//add params to handle what to do if it succeeds on admin-users.controller
        };

        vm.cancel = function () {
            $modalInstance.dismiss('cancel');
        };


        /*****ALERTS*****/

        $scope.alerts = [];

        $scope.replaceRoleAlert = function(index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Conflicts to resolve!',
                msg: 'You must delegate the user responsibilities.',
                alertType: 1
            });
        };

        $scope.newUserAlert = function(index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Conflicts to resolve!',
                msg: 'You must create a new user to delegate responsibilities to.',
                alertType: 2
            });
        };

        $scope.replaceChairpersonAlert = function(index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Conflicts to resolve!',
                msg: 'You must replace the Chairperson. ',
                warning: 'Warning: every open election will be canceled and re-opened with the new Chairperson.',
                alertType: 3
            });
        };

        $scope.changeChairpersonRoleAlert = function(index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Warning!',
                msg: 'If Chairperson is replaced, every open election will be canceled and re-opened with the new Chairperson assigned.',
                alertType: 4
            });
        };

        $scope.closeAlert = function(index) {
            $scope.alerts.splice(index, 1);
        };

        /*****DROPDOWN*****/

        $scope.status = {
            isopen: false
        };

        /*****SELECT*****/

        $scope.options = ['Verónica Vicario', 'Anabella Raccioppi', 'Santiago Saucedo', 'Nadya Lopez Zalba'];
        $scope.myselect = 'Select a user';

        $scope.options_users = ['Verónica Vicario', 'Santiago Saucedo'];
        $scope.myselect_users = 'Select a user';

        $scope.options_users2 = ['Anabella Raccioppi', 'Santiago Saucedo'];
        $scope.myselect_users2 = 'Select a user';

        $scope.options_users3 = ['Verónica Vicario', 'Santiago Saucedo'];
        $scope.myselect_users3 = 'Select a user';



    }

})();





