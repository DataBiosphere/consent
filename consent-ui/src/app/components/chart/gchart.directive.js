(function () {
    'use strict';

    angular.module('cmChart')
        .directive('cmChart', cmChartDirective);

    google.load("visualization", "1", {packages: ["corechart"]});

    /* ngInject */
    function cmChartDirective() {

        return {
            restrict: 'A',
            scope: {
               cmChartData:'=',
               cmChartOptions: '='
            },
            link:  function cmChartLink(scope, element) {
                //google.setOnLoadCallback(drawChart);
                function drawChart() {
                    var chart = new google.visualization.PieChart(element[0]);
                    chart.draw(google.visualization.arrayToDataTable(scope.cmChartData), scope.cmChartOptions);
                }

                scope.$watch('cmChartData', function(){
                                    if(undefined !== scope.cmChartData && undefined !== scope.cmChartOptions){
                                        drawChart();
                                    }
            });
        }
    }
    }
})();
