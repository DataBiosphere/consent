(function () {
    'use strict';

    angular
        .module('cmChairConsole')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
            .state('chair_console', {
                name: 'chair_console',
                url: '/chair_console',
                templateUrl: 'app/chair-console/chair-console.html',
                controller: 'ChairConsole',
                controllerAs: 'ChairConsole'
            });
    }

})();
