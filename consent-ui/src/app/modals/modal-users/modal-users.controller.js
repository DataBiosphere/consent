(function () {
    'use strict';

    angular.module('cmModalUsers')
        .controller('ModalUsers', ModalUsers);

    /* ngInject */
    function ModalUsers($modalInstance,cmUserService, $scope, user) {

        var vm = this;
        $scope.user = new Object();
        $scope.user.roles=[];

                if (user !== undefined){
                    console.log(user);
                    $scope.user=user;
                    $scope.user.roles=user.roles;
                    alert("1 "+user.roles);
                    $scope.user.roles=[];
                    alert("2 "+user.roles);
                }

        vm.ok = function (user) {
            cmUserService.postUser(user).$promise.then(
                function (value) {
                    $modalInstance.close();
                });
        };

         vm.edit = function (user) {
            cmUserService.updateUser(user);
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
                msg: 'If Chairperson is replaced, every open election will be canceled and re-opened with the new Chairperson assigned. Besides, the previous Chairperson is going to become an Alumni.',
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


    }

})();





