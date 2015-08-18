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

