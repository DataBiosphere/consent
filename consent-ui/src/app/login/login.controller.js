(function () {
        'use strict';

        angular.module('cmLogin')
            .controller('Login', Login);

        /* ngInject */
        function Login(cmLoginUserService,$rootScope) {

            $rootScope.loadScript('https://apis.google.com/js/platform.js', 'text/javascript', 'utf-8');

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
