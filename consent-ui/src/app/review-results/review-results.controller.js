(function () {
    'use strict';

    angular.module('cmReviewResults')

        .controller('ReviewResults', ReviewResults);

    /* ngInject */
    function ReviewResults() {

        var data = {
            'access': [
                ['Results', 'Votes'],
                ['Yes', 4],
                ['No', 1],
                ['Pending', 1]
            ],
            'dul': [
                ['Results', 'Votes'],
                ['Yes', 3],
                ['No', 2],
                ['Pending', 1]
            ]
        };

        var options = {
            'access': {
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
                    width: '100%',
                    height: '85%'
                },
                height: 138,
                slices: {
                    0: {color: '#603B9B'},
                    1: {color: '#777777'},
                    2: {color: '#c9c9c9'}
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
            'dul': {
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
                    width: '100%',
                    height: '85%'
                },
                height: 138,
                slices: {
                    0: {color: '#C16B0C'},
                    1: {color: '#777777'},
                    2: {color: '#c9c9c9'}
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

        var vm = this;
        vm.voteForm = {
            vote: undefined,
            rationale: ''
        };
        vm.positiveVote = positiveVote;
        vm.logVote = logVote;
        vm.sendReminder = sendReminder;
        vm.reminder = {text: 'Send a reminder', sent: false};
        vm.chartData = data;
        vm.chartOptions = options;

        function positiveVote() {
            vm.voteForm.rationale = '';
        }

        function logVote() {

        }


        function sendReminder() {
            vm.reminder.text = 'Reminder sent';
            vm.reminder.sent = true;
        }
    }

})();
