(function () {
    'use strict';

    angular.module('cmDULModal')
        .controller('DULModal', Modal);


    /* ngInject */
    function Modal($modalInstance, cmConsentService, $scope, consent) {

        var vm = this;
        $scope.data = '';
        $scope.disableButton = false;

        if (consent !== undefined) {
            $scope.consent = consent;
            $scope.file = new Object();
            $scope.file.name = consent.dulName
        }

        $scope.$on("fileSelected", function (event, arg) {
            $scope.$apply(function () {
                $scope.file = arg.file;
            });
        });

        vm.ok = function (consent) {
            $scope.disableButton = true;
            cmConsentService.postConsent(consent).$promise.then(
                function (value) {
                    cmConsentService.postDul($scope.file, value.consentId).$promise.then(
                        function (value) {
                            $modalInstance.close();
                        },
                        function (reason) {
                            fileUploadErrorAlert(0);
                            $scope.disableButton = false;
                        });
                },

                function (reason) {
                    $scope.duplicateEntryAlert(0, reason.data.cause.localizedMessage);
                    $scope.disableButton = false;
                });
        };

        vm.edit = function (consent) {
            $scope.disableButton = true;
            cmConsentService.updateConsent(consent).$promise.then(
                function (value) {
                    if ($scope.file.type !== undefined) {
                        cmConsentService.postDul($scope.file, consent.consentId).$promise.then(
                            function () {
                                $modalInstance.close();
                            }, function () {
                                fileUploadErrorAlert(0);
                                $scope.disableButton = false;
                            }
                        )
                    } else {
                        $modalInstance.close();
                    }
                },
                function (reason) {
                    $scope.duplicateEntryAlert(0, reason.data.cause.localizedMessage);
                    $scope.disableButton = false;
                }
            )
        }

        vm.cancel = function () {
            $modalInstance.dismiss('cancel');
        };


        /*****ALERTS*****/

        $scope.alerts = [];

        $scope.duplicateEntryAlert = function (index, message) {
            $scope.alerts.splice(index, 1);
            var tle = 'Conflicts to resolve!';
            if (message.indexOf("PRIMARY") > -1) {
                message = "There is a Data Use Limitation already registered with this Consent Id. ";
            } else if (message.indexOf("name") > -1) {
                message = "There is a Data Use Limitation already registered with this name. ";
            } else {
                tle = "Error , unable to create a new Data Use Limitation! ";
                message = "Internal Server Error ";
            }

            $scope.alerts.push({
                type: 'danger',
                title: tle,
                msg: message,
                alertType: 1
            });
        };

        $scope.fileUploadErrorAlert = function (index) {
            $scope.alerts.splice(index, 1);
            $scope.alerts.push({
                type: 'danger',
                title: 'Server Error',
                msg: 'Problem with the file UpLoad.',
                alertType: 1
            });
        };

        $scope.closeAlert = function (index) {
            $scope.alerts.splice(index, 1);
        };

    }
})();
