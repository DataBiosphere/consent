(function () {
    'use strict';

    angular.module('cmSession')
        .service('cmLoginUserService', cmLoginUserService);

    /* ngInject */
    function cmLoginUserService($rootScope,$location,GetUserResource,USER_ROLES,$state,cmAuthenticateService) {


        function loginUser(email) {
                    GetUserResource.get({email: email},function(data){
                                   $rootScope.setCurrentUser(data);
                                          if(cmAuthenticateService.isAuthorized(USER_ROLES.chairperson,data.roles)) {
                                                     $state.go('chair_console');
                                                }else if(cmAuthenticateService.isAuthorized(USER_ROLES.member,data.roles)) {
                                                     $state.go('user_console');
                                                }else if(cmAuthenticateService.isAuthorized(USER_ROLES.admin,data.roles)) {
                                                     $state.go('admin_console');
                                                }else if(cmAuthenticateService.isAuthorized(USER_ROLES.alumni,data.roles) || cmAuthenticateService.isAuthorized(USER_ROLES.researcher,data.roles)) {
                                                    $state.go('summary_votes');
                                                }
                                               else{
                                                alert("not valid Role");
                                                logoutUser();
                                              }

                    }, function(error){
                     if(error.status === 404) {
                        alert(email+" is not a registered user.");
                        logoutUser()
                      }
                  });
        }

        function logoutUser() {

           var auth2 = gapi.auth2.getAuthInstance();
           auth2.signOut().then(function () {
           $rootScope.logoutUser();
           $state.go("login");
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
