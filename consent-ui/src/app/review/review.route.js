(function () {
    'use strict';

    angular
        .module('cmReview')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
            // route to show our basic form (/form)
            .state('dul_review', {
                name: 'dul_review',
                url: '/dul_review',
                templateUrl: 'app/review/dul-review.html',
                controller: 'Review',
                controllerAs: 'Review'
            })
            // route to show our basic form (/form)
            .state('access_review', {
                name: 'access_review',
                url: '/access_review',
                templateUrl: 'app/review/access-review.html',
                controller: 'Review',
                controllerAs: 'Review'
            });
    }

})();
