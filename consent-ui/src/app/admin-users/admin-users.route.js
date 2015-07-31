(function () {
    'use strict';

    angular
        .module('cmAdminUsers')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider, USER_ROLES) {
        $stateProvider
            .state('admin_users', {
                name: 'admin_users',
                url: '/admin_users',
                templateUrl: 'app/admin-users/admin-users.html',
                controller: 'AdminUsers',
                controllerAs: 'AdminUsers',
                data: {
                    authorizedRoles: [USER_ROLES.admin]
                }

            });
    }

})();
