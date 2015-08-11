(function () {
    'use strict';

    angular.module('cmReviewedCases')
        .controller('ReviewedCases', ReviewedCases);

    /* ngInject */
    function ReviewedCases(cmPaginatorService, reviewedConsents, reviewedDRs) {

        var lists = {'dul': [], 'access': []};
        var list_max_items = 5;


        var vm = this;
        vm.activePage = {'dul': 0, 'access': 0};
        vm.currentPages = {'dul': [], 'access': []};
        vm.electionsList = {'dul': [], 'access': []};

        // changePage function from the service with the first 2 parameters locked
        vm.changePage = _.partial(cmPaginatorService.changePage,
            // first parameter to lock from changePage
            lists, list_max_items,
            // second parameter to lock from changePage
            {
                activePage: vm.activePage,
                currentPages: vm.currentPages,
                electionsList: vm.electionsList
            }
        );

        init();

        function init() {
                lists['dul'] = transformElectionResultData(reviewedConsents);
                lists['access'] = transformElectionResultData(reviewedDRs);
                vm.changePage('dul', 0);
                vm.changePage('access', 0);
        }
    }

    // We need to transform the result data to a string to be able to filter results
    function transformElectionResultData(collection){
        var dup_array = collection.slice();
        for (var i = 0; i < dup_array.length; i++) {
            if(dup_array[i].finalVote === true){
                dup_array[i].finalVoteString = 'Yes';
            }else{
                dup_array[i].finalVoteString = 'No';
            }
        }
        return dup_array;
    }

})();
