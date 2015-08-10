(function () {

    'use strict';

    angular.module('ConsentManagement')
        .controller('ApplicationController', ApplicationController);


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
          $rootScope.loadScript = function(url, type, charset) {
                if (type===undefined) type = 'text/javascript';
                   if (url) {
                      var script = document.querySelector("script[src*='"+url+"']");
                   if (!script) {
                          var heads = document.getElementsByTagName("head");
                     if (heads && heads.length) {
                         var head = heads[0];
                         if (head) {
                             script = document.createElement('script');
                             script.setAttribute('src', url);
                             script.setAttribute('type', type);
                             if (charset) script.setAttribute('charset', charset);
                             head.appendChild(script);
                         }
                     }
                 }
                 return script;
             }
         };
 }


angular.module('ConsentManagement').run(function ($location,$rootScope,$state, cmAuthenticateService) {
  $rootScope.$on('$stateChangeStart', function (event,next, toState, toParams, fromState, fromParams) {
    var authorizedRoles = next.data.authorizedRoles;

      if($state.current.name==="")
           {
             $location.path("/login" );
           }else if($rootScope.currentUser===null){
                     event.preventDefault();
                     alert("null");
                    }else if ($state.current.name!=="login"){
                             if (!cmAuthenticateService.isAuthorized(authorizedRoles,$rootScope.currentUser.roles)) {
                                     event.preventDefault();
                               }
                             }

        });
    })
})();
