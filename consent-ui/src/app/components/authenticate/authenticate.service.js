(function () {
    'use strict';

    angular.module('cmAuthenticate')
        .service('cmAuthenticateService',cmAuthenticateService);

    /* ngInject */
    function cmAuthenticateService() {

          function isAuthorized(authorizedRoles,userRole) {

                         if (!angular.isArray(authorizedRoles)) {
                                        authorizedRoles = [authorizedRoles];
                                       }
                      return authorizedRoles.indexOf(userRole.toUpperCase()) !== -1;
                }
                return {
                isAuthorized: function(authorizedRoles,userRole) {
                                    return isAuthorized(authorizedRoles,userRole);
                                }
                }
  }
})();

