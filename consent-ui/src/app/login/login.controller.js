(function () {
    'use strict';

    angular.module('cmLogin')
        .controller('Login', Login);

    /* ngInject */
    function Login(cmLoginUserService) {

           function onSignIn(googleUser) {

           var profile = googleUser.getBasicProfile();
           cmLoginUserService.loginUser(profile.getEmail());
          };

           function signOut() {
           cmLoginUserService.logoutUser();

         };
         window.signOut = signOut;
         window.onSignIn = onSignIn;
       }
    }
)();
