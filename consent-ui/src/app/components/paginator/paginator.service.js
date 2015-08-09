(function () {
    'use strict';

    angular.module('cmPaginator')
        .service('cmPaginatorService', cmPaginatorService);

    /* ngInject */
    function cmPaginatorService() {

        var PAGINATOR_MAX_ITEMS = 9;

        var cmPaginatorService = this;
        cmPaginatorService.changePage = changePage;

        return cmPaginatorService;

        /**
         * Changes the page
         * @param lists
         * @param id
         * @param page
         * @param options: should have activePage, currentPages and electionsList attributes
         */
        function changePage(lists, list_max_items, options, id, page) {

            var amountOfElements = lists[id].length;
            var isValidPage = ((amountOfElements / list_max_items) > page) && (page >= 0);

            if (!isValidPage) return;

            options.activePage[id] = page;

            generatePaginator(lists, list_max_items, options, id, page);
            generateListItemsHtml(lists, list_max_items, options, id, page);

        }

        function generatePaginator(lists, list_max_items, options, id, page) {

            var delta = Math.floor(PAGINATOR_MAX_ITEMS / 2);
            var numberOfElements = lists[id].length;
            var numberOfRanges = Math.ceil(numberOfElements / list_max_items);
            var floorPosition = page - delta > 0 ? page - delta : 0;
            var roofPosition;

            if (floorPosition + PAGINATOR_MAX_ITEMS < numberOfRanges) {
                roofPosition = floorPosition + PAGINATOR_MAX_ITEMS;
            } else {
                var auxPagesMissingFromFloor = floorPosition + PAGINATOR_MAX_ITEMS - numberOfRanges;
                floorPosition = floorPosition - auxPagesMissingFromFloor >= 0 ? floorPosition - auxPagesMissingFromFloor : 0;
                roofPosition = numberOfRanges;
            }

            options.currentPages[id] = _.range(floorPosition, roofPosition);
        }

        function generateListItemsHtml(lists, list_max_items, options, id, page) {

            var floorPosition = page * list_max_items;
            var roofPosition = floorPosition + list_max_items;
            options.electionsList[id] = _.filter(lists[id], function (election, electionPosition) {
                return _.inRange(electionPosition, floorPosition, roofPosition);
            });

        }

    }

})();
