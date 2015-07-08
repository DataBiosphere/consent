(function () {
    'use strict';

    angular
        .module('cmUserConsole')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider,USER_ROLES) {
        $stateProvider
            .state('user_console', {
                name: 'user_console',
                url: '/user_console',
                templateUrl: 'app/user-console/user-console.html',
                controller: 'UserConsole',
                controllerAs: 'UserConsole',
                      data: {
                                authorizedRoles: [USER_ROLES.chairperson,USER_ROLES.admin,USER_ROLES.dacmember]
                            }
            })
    }

})();
