(function () {
    'use strict';

    angular.module('cmUser')
        .service('cmLoginUserService', cmLoginUserService);

    /* ngInject */
    function cmLoginUserService($rootScope,$location,UserResource,USER_ROLES,$state) {


        function loginUser(email) {
                    UserResource.get({email: email},function(data){
                                   $rootScope.setCurrentUser(data);
                                     var i=0;
                                     angular.forEach(USER_ROLES, function(value, key) {
                                         if(data.memberStatus.toUpperCase()===value)
                                            {
                                                           if(data.memberStatus.toUpperCase() === USER_ROLES.chairperson) {
                                                                       $state.go('chair_console');
                                                            }else if(data.memberStatus.toUpperCase() === USER_ROLES.dacmember) {
                                                                       $state.go('user_console');
                                                            }else if(data.memberStatus.toUpperCase() === USER_ROLES.admin) {
                                                                       $state.go('admin_console');
                                                               }
                                               i=1;
                                            }
                                     });
                                   if(i===0){
                                    alert(data.memberStatus.toUpperCase()+" is not a valid Role");
                                    logoutUser();
                                   }
                    }, function(error){
                     if(error.status === 404) {
                        alert(email+" is not a DacMember");
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
