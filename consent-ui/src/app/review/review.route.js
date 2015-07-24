(function () {
    'use strict';

    angular
        .module('cmReview')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider,USER_ROLES) {
        $stateProvider
            // route to show our basic form (/form)
            .state('dul_review', {
                name: 'dul_review',
                url: '/dul_review',
                params: {
                    consentId: null,
                    voteId: null,
                },
                templateUrl: 'app/review/dul-review.html',

                controller: 'DulReview',
                controllerAs: 'DulReview',
                data: {
                    authorizedRoles: [USER_ROLES.dacmember, USER_ROLES.chairperson]
                },
                resolve: {
                    vote: function($stateParams, cmVoteService){
                        return cmVoteService.getVote($stateParams.consentId, $stateParams.voteId);
                    },
                    consent: function($stateParams, cmConsentService){
                        return cmConsentService.findConsent($stateParams.consentId);
                    },
                    election: function($stateParams, cmElectionService){
                        return cmElectionService.findElection($stateParams.consentId);
                    }

                }

            })
            // route to show our basic form (/form)
            .state('access_review', {
                name: 'access_review',
                url: '/access_review',
                templateUrl: 'app/review/access-review.html',
                controller: 'Review',
                controllerAs: 'Review',
                data: {
                    authorizedRoles: [USER_ROLES.dacmember, USER_ROLES.chairperson]
                }
            });
    }

})();
