(function () {
    'use strict';

    angular
        .module('cmUserConsole')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
            .state('user_console', {
                name: 'user_console',
                url: '/user_console',
                templateUrl: 'app/user-console/user-console.html',
                controller: 'UserConsole',
                controllerAs: 'UserConsole'
            })
    }

})();
