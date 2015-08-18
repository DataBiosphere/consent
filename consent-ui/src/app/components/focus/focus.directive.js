(function () {
    'use strict';

    angular.module('cmFocus')
        .directive('cmFocus', cmFocusDirective);

    /* ngInject */
    function cmFocusDirective($timeout) {

        return {
            restrict: 'A',
            link: cmFocusLink
        };

        function cmFocusLink(scope, element, attrs) {

            scope.$watch(condition, focusElement);

            function condition() {
                return attrs.cmFocus;
            }

            function focusElement() {
                $timeout(function () {
                    if (attrs.cmFocus == "true") element[0].focus();
                }, 0);
            }

        }
    }

})();
