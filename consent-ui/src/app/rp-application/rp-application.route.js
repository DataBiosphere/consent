(function () {
    'use strict';

    angular
        .module('cmRPApplication')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
            // route to show our basic form (/form)
            .state('rp_application', {
                url: '/rp_application',
                templateUrl: 'app/rp-application/rp-application.html',
                controller: 'RPApplication',
                controllerAs: 'RPApplication'
            })

            // nested states
            // each of these sections will have their own view
            // url will be nested (/form/profile)
            .state('rp_application.step1', {
                url: '/step1',
                templateUrl: 'app/rp-application/rp-application-f1.html'
            })

            // url will be /form/interests
            .state('rp_application.step2', {
                url: '/step2',
                templateUrl: 'app/rp-application/rp-application-f2.html'
            })

            // url will be /form/interests
            .state('rp_application.step3', {
                url: '/step3',
                templateUrl: 'app/rp-application/rp-application-f3.html'
            })

            // url will be /form/payment
            .state('rp_application.step4', {
                url: '/step4',
                templateUrl: 'app/rp-application/rp-application-f4.html'
            })
    }

})();
