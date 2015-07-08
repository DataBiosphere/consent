(function () {

    'use strict';

    angular.module('ConsentManagement')
        .controller('ApplicationController', ApplicationController);
    angular.module('ConsentManagement')
        .constant('USER_ROLES', {
        admin: 'admin',
        chairperson: 'chairperson',
        dacmember: 'dacmember'
      });

    angular.module('ConsentManagement')
       .constant('AUTH_EVENTS', {
       loginSuccess: 'auth-login-success',
       loginFailed: 'auth-login-failed',
       logoutSuccess: 'auth-logout-success',
       sessionTimeout: 'auth-session-timeout',
       notAuthenticated: 'auth-not-authenticated',
       notAuthorized: 'auth-not-authorized'
      });

    /* ngInject */
    function ApplicationController($rootScope,USER_ROLES) {


         $rootScope.currentUser = null;
         $rootScope.userRoles = USER_ROLES;
         $rootScope.setCurrentUser = function (user) {
         $rootScope.currentUser = user;
          };
         $rootScope.logoutUser = function () {
         $rootScope.currentUser = null;
         };


 }

angular.module('ConsentManagement').run(function ($location,$rootScope, AUTH_EVENTS, cmAuthenticateService) {
  $rootScope.$on('$stateChangeStart', function (event, next) {
    var authorizedRoles = next.data.authorizedRoles;
      //  alert("AuthorizedRoles: " +next.data.authorizedRoles+" UserRol: "+$rootScope.currentUser.memberStatus);

    if($rootScope.currentUser===null){
      event.preventDefault();
      $rootScope.$broadcast(AUTH_EVENTS.notAuthenticated);
      $location("/login");
    }else{
    if (!cmAuthenticateService.isAuthorized(authorizedRoles,$rootScope.currentUser.memberStatus)) {
      event.preventDefault();
      $rootScope.$broadcast(AUTH_EVENTS.notAuthorized);
       }
    }
  });
})
})();
