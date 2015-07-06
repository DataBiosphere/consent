'use strict';

describe('myApp.review module', function() {

  beforeEach(module('myApp.review'));

  describe('review controller', function(){

    it('should ....', inject(function($controller) {
      //spec body
      var access_reviewCtrl = $controller('reviewCtrl');
      expect(access_reviewCtrl).toBeDefined();
    }));

  });
});
