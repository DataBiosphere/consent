(function () {
    'use strict';

    angular
        .module('cmReviewResults')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider,USER_ROLES) {
        $stateProvider
            // route to show our basic form (/form)
            .state('dul_review_results', {
                name: 'dul_review_results',
                url: '/dul_review_results',
                params: {
                    consentId: null,
                },
                templateUrl: 'app/review-results/dul-review-results.html',

                controller: 'DulReviewResults',
                controllerAs: 'DulReviewResults',
                data: {
                    authorizedRoles: [USER_ROLES.chairperson]
                },
                resolve: {
                    election: function($stateParams, cmElectionService){
                        console.log($stateParams.consentId);
                        return cmElectionService.findElection($stateParams.consentId).$promise;
                    }
                }
            })
            // route to show our basic form (/form)
            .state('access_review_results', {
                name: 'access_review_results',
                url: '/access_review_results',
                templateUrl: 'app/review-results/access-review-results.html',
                controller: 'ReviewResults',
                controllerAs: 'ReviewResults',
                data: {
                    authorizedRoles: [USER_ROLES.chairperson]
                }
            });

    }
})();
