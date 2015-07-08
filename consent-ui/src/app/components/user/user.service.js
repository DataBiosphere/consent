(function () {
    'use strict';

    angular.module('cmUser')
        .service('cmLoginUserService', cmLoginUserService);

    /* ngInject */
    function cmLoginUserService($rootScope,$location,UserResource,USER_ROLES) {


        function loginUser(email) {
                    UserResource.get({email: email},function(data){
                                   $rootScope.setCurrentUser(data);
                                    if(data.memberStatus === USER_ROLES.chairperson) {
                                                $location.path("/chair_console" );
                                     }

                                     if(data.memberStatus === USER_ROLES.dacmember) {
                                                $location.path("/user_console" );
                                     }
                    }, function(error){
                     if(error.status === 404) {

                        // Logout when LogIn with non valid Credentials ??
                        logoutUser()
                      }
                  });
        }

        function logoutUser() {

           var auth2 = gapi.auth2.getAuthInstance();
           auth2.signOut().then(function () {
           $rootScope.logoutUser();
           $location.path("/login" );
           window.location.reload();
               });
        }



      return{
                loginUser: function(email) {
                    return loginUser(email);
                },
              logoutUser: function(){
                            return logoutUser();
                        }
            }
    }
})();
