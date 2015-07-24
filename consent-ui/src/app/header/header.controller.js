(function () {
    'use strict';

    angular.module('cmHeader')
        .controller('Header', Header);

    /* ngInject */
    function Header($scope) {

        $scope.status = {
            isopen: false
        };

    }

})();
