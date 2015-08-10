(function () {
    'use strict';

    angular.module('cmModalUsers')
        .directive('addRole', addRoleRadioDirective);

    /* ngInject */
    function addRoleRadioDirective() {

        return {
           restrict: "EA",
           scope: false,
                    link: function (scope, element, attributes) {
                        element.bind("change", function () {

                            if(element.context.checked){
                                var rol =
                                rol = new Object();
                                rol.name=element.context.id;
                                scope.user.roles.push(rol);
                                console.log(scope.user.roles);
                        }else{
                             var i = scope.user.roles.length;
                             while(i--){
                               console.log(scope.user.roles[i].name);
                               console.log(element.context.id);

                                  if( scope.user.roles[i]
                                      && scope.user.roles[i].name===element.context.id){
                                      scope.user.roles.splice(i,1);
                                      console.log(scope.user.roles);
                                  }
                               }
                             }
                        });
                  }
              }
          }


})();
