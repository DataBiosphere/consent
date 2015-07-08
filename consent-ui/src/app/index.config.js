(function () {
    'use strict';

    angular
        .module('ConsentManagement')
        .config(logConfig)
        .config(routeConfig)
        .config(httpConfig)
        .constant("apiUrl", "http://localhost:8180/");

    /* ngInject */
    function logConfig($logProvider) {
        $logProvider.debugEnabled(true);
    }

    /* ngInject */
    function routeConfig($urlRouterProvider) {
        $urlRouterProvider.otherwise("/login");
    }

    /* ngInject */
    function httpConfig($httpProvider) {
        $httpProvider.defaults.useXDomain = true;
        delete $httpProvider.defaults.headers.common['X-Requested-With'];
    }

})();
