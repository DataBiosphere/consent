(function () {
    'use strict';

    angular.module('cmConsent')
        .service('cmConsentService', cmConsentService);

    /* ngInject */
    function cmConsentService(ConsentResource, ConsentDulResource, ConsentManageResource, AllConsentResource, CreateDulResource, UpdateConsentResource) {

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

        function findConsentManage(lists, vm) {
            ConsentManageResource.List().$promise.then(
                function (data) {
                    lists['dul'] = data;
                    vm.changePage('dul', 0);
                });
        }

        function postConsent(consent){
             consent.requiresManualReview=true;
             consent.useRestriction= new Object();
             consent.useRestriction.type = "nothing"
             return AllConsentResource.put({},consent);

        }

        function updateConsent(consent){
             consent.requiresManualReview=true;
             consent.useRestriction= new Object();
             consent.useRestriction.type = "nothing"
             return UpdateConsentResource.update({consentId: consent.consentId},consent);
        }


        function postDul(dul,consentId){
            var postObject = new Object();
            postObject.file=dul;
            return CreateDulResource.post({consentId: consentId},dul);
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
            },
            postConsent: function(consent){
                return postConsent(consent);
            },
            postDul: function(dul,consentId){
                return  postDul(dul,consentId);
            },

            updateConsent: function(consent){
                 return  updateConsent(consent);
            }

        }
    }

})();
