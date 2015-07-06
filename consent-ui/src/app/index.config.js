(function () {
    'use strict';

    angular
        .module('ConsentManagement')
        .config(logConfig)
        .config(routeConfig)
        .config(httpConfig);

    /* ngInject */
    function logConfig($logProvider) {
        $logProvider.debugEnabled(true);
    }

    /* ngInject */
    function routeConfig($urlRouterProvider) {
        $urlRouterProvider.otherwise("/user_console");
    }

    /* ngInject */
    function httpConfig($httpProvider) {
        $httpProvider.defaults.useXDomain = true;
        delete $httpProvider.defaults.headers.common['X-Requested-With'];
    }

})();
