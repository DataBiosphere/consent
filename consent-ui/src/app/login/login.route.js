(function () {
    'use strict';

    angular
        .module('cmLogin')
        .config(routeConfig);


    /* ngInject */
    function routeConfig($stateProvider,USER_ROLES) {
        $stateProvider
            .state('login', {
                name: 'login',
                url: '/login',
                templateUrl: 'app/login/login.html',
                controller: 'Login',
                controllerAs:'Login',
                     data: {
                     authorizedRoles: [USER_ROLES.chairperson,USER_ROLES.member,USER_ROLES.admin]
                           }
            });
    }
})();
