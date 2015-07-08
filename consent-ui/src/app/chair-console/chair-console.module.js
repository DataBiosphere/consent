(function () {
    'use strict';

    angular.module('cmChairConsole', ['ui.router', 'cmPaginator']);

    angular.module('cmChairConsole')
            .constant('USER_ROLES', {

             admin: 'admin',
             chairperson: 'chairperson',
             dacmember: 'dacmember'
           });
    })();
