(function () {
    'use strict';

    angular
        .module('cmSummaryVotes')
        .config(routeConfig);

    /* ngInject */
    function routeConfig($stateProvider) {
        $stateProvider
            .state('summary_votes', {
                name: 'summary_votes',
                url: '/summary_votes',
                templateUrl: 'app/summary-votes/summary-votes.html',
                controller: 'SummaryVotes',
                controllerAs: 'SummaryVotes'
            });
    }

})();
