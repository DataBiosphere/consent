(function () {
    'use strict';

    angular.module('cmModalUsers')
        .controller('ModalUsers', ModalUsers);

    /* ngInject */
    function ModalUsers($modalInstance, cmUserService, $scope, user) {

        var vm = this;
        $scope.user = new Object();
        $scope.user.roles = [];

        if (user !== undefined) {
            $scope.user = user;
            $scope.user.roles = user.roles;
            $scope.user.roles = [];
        }

           $scope.$on("chairpersonAlert", function (event, arg) {
                                                     $scope.$apply(function () {
                                                     if(arg.alert){
                                                       $scope.changeChairpersonRoleAlert();
                                                     }else{
                                                       $scope.closeAlert();
                                                     }
                                                    });
                                                   });

        vm.ok = function (user) {
            cmUserService.postUser(user).$promise.then(
                function (value) {
                    $modalInstance.close();
                }, function (value) {
                    $scope.duplicateNameAlert(0);
                });

        };

        vm.edit = function (user) {
            cmUserService.updateUser(user);
            $modalInstance.close();
        };


        vm.cancel = function () {
            $modalInstance.dismiss('cancel');
        };


        /*****ALERTS*****/

        $scope.alerts = [];

        $scope.replaceRoleAlert = function (index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Conflicts to resolve!',
                msg: 'You must delegate the user responsibilities.',
                alertType: 1
            });
        };

        $scope.newUserAlert = function (index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Conflicts to resolve!',
                msg: 'You must create a new user to delegate responsibilities to.',
                alertType: 2
            });
        };

        $scope.replaceChairpersonAlert = function (index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Conflicts to resolve!',
                msg: 'You must replace the Chairperson. ',
                warning: 'Warning: every open election will be canceled and re-opened with the new Chairperson.',
                alertType: 3
            });
        };

        $scope.changeChairpersonRoleAlert = function (index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Warning!',
                msg: 'If Chairperson is replaced, every open election will be canceled and re-opened with the new Chairperson assigned. Besides, the previous Chairperson is going to become an Alumni.',
                alertType: 4
            });
        };

        $scope.duplicateNameAlert = function (index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Conflicts to resolve!',
                msg: 'There is a user already registered with this google account.',
                alertType: 5
            });
        };

        $scope.closeAlert = function (index) {
            $scope.alerts.splice(index, 1);
        };

        /*****DROPDOWN*****/

        $scope.status = {
            isopen: false
        };


    }

})();





