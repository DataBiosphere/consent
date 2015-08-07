(function () {
    'use strict';

    angular.module('cmAdminManage')
        .controller('AdminManage', AdminManage);

    /* ngInject */
    function AdminManage(cmPaginatorService, $modal, cmConsentService, cmElectionService) {

        var lists = {'dul': []};
        var list_max_items = 10;

        var vm = this;
        vm.activePage = {'dul': 0};
        vm.currentPages = {'dul': []};
        vm.electionsList = {'dul': []};
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
        vm.openCreate = openCreate;
        vm.openCancel = openCancel;
        vm.addDul = addDul;
        vm.editDul = editDul;


        init();

        function init() {
            cmConsentService.findConsentManage(lists, vm);
        }

        function openCreate (consentId) {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/create-modal.html',
                controller: 'Modal',
                controllerAs: 'Modal',
                resolve: {
                    consentId : function(){
                        vm.selectedConsentId = consentId;
                    }
                }
            });

            modalInstance.result.then(function () {
                cmElectionService.createElection(vm.selectedConsentId).$promise.then(function() {
                    init();
                });
            });
        }

        function openCancel (election) {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/cancel-modal.html',
                controller: 'Modal',
                controllerAs: 'Modal',
                resolve: {
                    election: function(){
                        vm.selectedElection = election;
                    }
                }
            });

            modalInstance.result.then(function () {
                var electionToUpdate = new Object();
                electionToUpdate.status = 'Canceled';
                electionToUpdate.referenceId = vm.selectedElection.consentId;
                electionToUpdate.electionId = vm.selectedElection.electionId;
                cmElectionService.updateElection(electionToUpdate).$promise.then(function() {
                    init();
                });
            });
          }

        function addDul () {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/dul-modal/add-dul-modal.html',
                controller: 'DULModal',
                controllerAs: 'DULModal',
                 resolve: {
                                consent: new Object()
                          }
            });

            modalInstance.result.then(function () {//selectedItem - params to apply when the fc was successful
                //what to do if it was accepted
                init();
            }, function () {
                //what to do if the modal was canceled
            });
        }

        function editDul (consentId) {

            var modalInstance = $modal.open({
                animation: false,
                templateUrl: 'app/modals/dul-modal/edit-dul-modal.html',
                controller: 'DULModal',
                controllerAs: 'DULModal',
                resolve: {
                consent: function(cmConsentService){
                             return cmConsentService.findConsent(consentId);
                         }
                       }
             });

            modalInstance.result.then(function (selectedItem) {//selectedItem - params to apply when the fc was successful
                //what to do if it was accepted
                init();
            }, function () {
                //what to do if the modal was canceled
            });
        }
    }
})();
