(function () {
    'use strict';

    angular
        .module('cmAdminConsole')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider, USER_ROLES) {
        $stateProvider
            .state('admin_console', {
                name: 'admin_console',
                url: '/admin_console',
                templateUrl: 'app/admin-console/admin-console.html',
                controller: 'AdminConsole',
                controllerAs: 'AdminConsole',
                data: {
                    authorizedRoles: [USER_ROLES.admin]
                }
            });
    }

})();
