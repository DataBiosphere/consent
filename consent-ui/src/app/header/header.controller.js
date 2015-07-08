(function () {
    'use strict';

    angular.module('cmHeader')
        .controller('Header', Header);

    /* ngInject */
    function Header($scope, $log) {

        $scope.status = {
            isopen: false
        };

    }

})();
