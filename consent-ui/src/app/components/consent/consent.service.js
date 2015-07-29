(function () {
    'use strict';

    angular.module('cmConsent')
        .service('cmConsentService', cmConsentService);

    /* ngInject */
    function cmConsentService(ConsentResource, ConsentDulResource, ConsentManageResource) {

        /**
         * Find data for the consent related to the consentId sent as a parameter
         * @param consentId
         */
        function findConsentById(id){
            return ConsentResource.get({consentId: id}).$promise;
        }

        /**
         * Find the data use letter for the consent related to the consentId sent as a parameter
         * @param consentId
         */
        function findDulForConsent(id){
            return ConsentDulResource.get({consentId: id}).$promise;
        }

        /**
         * Find the data use letter for the consent related to the consentId sent as a parameter
         * @param consentId
         */
        function findDulForConsent(id){
            return ConsentDulResource.get({consentId: id}).$promise;
        }

        function findConsentManage(lists, vm){
            ConsentManageResource.List().$promise.then(
                function(data){
                    lists['dul'] = data;
                    vm.changePage('dul', 0);
                });
        }

        return{
            findConsent: function(id) {
                return findConsentById(id);
            },
            findDataUseLetterForConsent: function(id){
                return findDulForConsent(id);
            },
            findConsentManage: function(lists, vm){
                return findConsentManage(lists, vm);
            }
        }
    }

})();
