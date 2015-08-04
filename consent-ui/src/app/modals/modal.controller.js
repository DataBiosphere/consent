(function () {
    'use strict';

    angular.module('cmModal')
        .controller('Modal', Modal);

    /* ngInject */
    function Modal($modalInstance) {

        var vm = this;

        vm.ok = function () {
            $modalInstance.close();//add params to handle what to do if it succeds on admin-manage.controller
        };

        vm.cancel = function () {
            $modalInstance.dismiss('cancel');
        };

        vm.singleModel = 0;
        vm.radioModel = '';
        vm.checkModel = {
            admin: false,
            researcher: false
        };

    }

})();






        ///*****ALERTS*****/
        //
        //$scope.alerts = [];
        //
        //$scope.replaceRoleAlert = function() {
        //    $scope.alerts.push({
        //        type: 'danger',
        //        title: 'Conflicts to resolve!',
        //        msg: 'You must delegate the user responsibilities.',
        //        alertType: 1
        //    });
        //};
        //
        //$scope.newUserAlert = function() {
        //    $scope.alerts.push({
        //        type: 'danger',
        //        title: 'Conflicts to resolve!',
        //        msg: 'You must create a new user to delegate responsibilities to.',
        //        alertType: 2
        //    });
        //};
        //
        //$scope.replaceChairpersonAlert = function() {
        //    $scope.alerts.push({
        //        type: 'danger',
        //        title: 'Conflicts to resolve!',
        //        msg: 'You must replace the Chairperson. ',
        //        warning: 'Warning: every open election will be canceled and re-opened with the new Chairperson.',
        //        alertType: 3
        //    });
        //};
        //
        //$scope.closeAlert = function(index) {
        //    $scope.alerts.splice(index, 1);
        //};
        //
        ///*****DROPDOWN*****/
        //
        //$scope.status = {
        //    isopen: false
        //};
