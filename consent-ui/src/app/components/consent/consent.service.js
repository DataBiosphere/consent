(function () {
    'use strict';

    angular.module('cmConsent')
        .service('cmConsentService', cmConsentService);

    /* ngInject */
    function cmConsentService(ConsentResource, ConsentDulResource) {

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

        return{
            findConsent: function(id) {
                return findConsentById(id);
            },
            findDataUseLetterForConsent: function(id){
                return findDulForConsent(id);
            }
        }
    }

})();
