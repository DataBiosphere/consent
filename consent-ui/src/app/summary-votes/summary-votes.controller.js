(function () {
    'use strict';

    angular.module('cmSummaryVotes')
        .controller('SummaryVotes', SummaryVotes);

    /* ngInject */
    function SummaryVotes(apiUrl, cmPendingCaseService) {
        var vm = this;
        vm.downloadUrl = apiUrl + "consent/cases/summary/file";
        var data = {
            'accessTotal': [
                ['Results', 'Votes'],
                ['Reviewed cases', 0],
                ['Pending cases', 0]
            ],
            'accessReviewed': [
                ['Results', 'Votes'],
                ['Positive', 0],
                ['Negative', 0]
            ],
            'dulTotal': [
                ['Results', 'Votes'],
                ['Reviewed cases', 0],
                ['Pending cases',0]
            ],
            'dulReviewed': [
                ['Results', 'Votes'],
                ['Positive', 0],
                ['Negative', 0]
            ]
        };

        var options = {
            'accessTotal': {
                pieHole: 0.4,
                pieSliceTextStyle: {
                    color: 'white',
                    fontSize: 16
                },
                pieSliceText: 'none',
                pieSliceBorderColor: 'transparent',
                backgroundColor: 'transparent',
                chartArea: {
                    left: 0,
                    top: 10,
                    right: 0,
                    bottom: 10,
                    width:'100%',
                    height:'85%'
                },
                height: 138,
                slices: {
                    0: { color: '#603B9B' },
                    1: { color: '#777777' }
                },
                legend: {
                    position: 'right',
                    textStyle: {
                        color: '#777777',
                        bold: true,
                        fontName: 'Roboto',
                        fontSize: 14
                    },
                    alignment: 'start'
                },
                tooltip: {
                    textStyle: {
                        color: 'black',
                        fontSize: 14
                    }
                }
            },
            'accessReviewed': {
                pieHole: 0.4,
                pieSliceTextStyle: {
                    color: 'white',
                    fontSize: 16
                },
                pieSliceText: 'none',
                pieSliceBorderColor: 'transparent',
                backgroundColor: 'transparent',
                chartArea: {
                    left: 0,
                    top: 10,
                    right: 0,
                    bottom: 10,
                    width:'100%',
                    height:'85%'
                },
                height: 138,
                slices: {
                    0: { color: '#603B9B' },
                    1: { color: '#777777' }
                },
                legend: {
                    position: 'right',
                    textStyle: {
                        color: '#777777',
                        bold: true,
                        fontName: 'Roboto',
                        fontSize: 14
                    },
                    alignment: 'start'
                },
                tooltip: {
                    textStyle: {
                        color: 'black',
                        fontSize: 14
                    }
                }
            },
            'dulTotal': {
                pieHole: 0.4,
                pieSliceTextStyle: {
                    color: 'white',
                    fontSize: 16
                },
                pieSliceText: 'none',
                pieSliceBorderColor: 'transparent',
                backgroundColor: 'transparent',
                chartArea: {
                    left: 0,
                    top: 10,
                    right: 0,
                    bottom: 10,
                    width:'100%',
                    height:'85%'
                },
                height: 138,
                slices: {
                    0: { color: '#C16B0C' },
                    1: { color: '#777777' }
                },
                legend: {
                    position: 'right',
                    textStyle: {
                        color: '#777777',
                        bold: true,
                        fontName: 'Roboto',
                        fontSize: 14
                    },
                    alignment: 'start'
                },
                tooltip: {
                    textStyle: {
                        color: 'black',
                        fontSize: 14
                    }
                }
            },
            'dulReviewed': {
                pieHole: 0.4,
                pieSliceTextStyle: {
                    color: 'white',
                    fontSize: 16
                },
                pieSliceText: 'none',
                pieSliceBorderColor: 'transparent',
                backgroundColor: 'transparent',
                chartArea: {
                    left: 0,
                    top: 10,
                    right: 0,
                    bottom: 10,
                    width:'100%',
                    height:'85%'
                },
                height: 138,
                slices: {
                    0: { color: '#C16B0C' },
                    1: { color: '#777777' }
                },
                legend: {
                    position: 'right',
                    textStyle: {
                        color: '#777777',
                        bold: true,
                        fontName: 'Roboto',
                        fontSize: 14
                    },
                    alignment: 'start'
                },
                tooltip: {
                    textStyle: {
                        color: 'black',
                        fontSize: 14
                    }
                }
            }
        };
        cmPendingCaseService.findSummary(data, vm);
        vm.chartOptions = options;
    }



})();
