(function () {
    'use strict';

    angular.module('cmDULModal')
        .directive('fileUpload', uploadFileDirective);

    /* ngInject */
    function uploadFileDirective() {

        return {
            scope: true,
            link: function (scope, element, attributes) {
                element.bind("change", function () {
                    var file = element.context.files[0];
                    scope.$emit("fileSelected", { file : file });

                });
            }
        };
    }
})();
