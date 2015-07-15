(function () {
    'use strict';

    angular.module('cmPendingCase', ['ngResource'])
        .factory('DataRequestPendingCases', function($resource, apiUrl){
            return $resource(apiUrl+"dataRequest/cases/pending/:userId", {},
                {
                    List: {method:'GET',isArray:true}
                });

        })

        .factory('ConsentPendingCases', function($resource, apiUrl){
            return $resource(apiUrl+"consent/cases/pending/:userId", {},
                {
                    List: {method:'GET',isArray:true}
                });
        })

        .factory('ConsentSummaryCases', function($resource, apiUrl){
            return $resource(apiUrl+"consent/cases/summary");
        })

        .factory('DataRequestSummaryCases', function($resource, apiUrl){
            return $resource(apiUrl+"dataRequest/cases/summary");
        })

        .factory('ConsentSummaryFile', function($resource, apiUrl){
            return $resource(apiUrl+"consent/cases/summary/file");
        })

})();
