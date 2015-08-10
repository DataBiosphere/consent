(function () {
    'use strict';

    angular
        .module('cmReviewedCases')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider, USER_ROLES) {
        $stateProvider
            .state('reviewed_cases', {
                name: 'reviewed_cases',
                url: '/reviewed_cases',
                templateUrl: 'app/reviewed-cases/reviewed-cases.html',
                controller: 'ReviewedCases',
                controllerAs: 'ReviewedCases',
                data: {
                    authorizedRoles: [USER_ROLES.admin]
                }

        });
    }

})();
