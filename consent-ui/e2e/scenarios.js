'use strict';

/* https://github.com/angular/protractor/blob/master/docs/toc.md */

describe('my app', function() {


  it('should automatically redirect to /dac_console when location hash/fragment is empty', function() {
    browser.get('index.html');
    expect(browser.getLocationAbsUrl()).toMatch("/dac_console");
  });


  describe('dac_console', function() {

    beforeEach(function() {
      browser.get('index.html#/dac_console');
    });


    it('should render dac_console when user navigates to /dac_console', function() {
      expect(element.all(by.css('[ng-view] p')).first().getText()).
        toMatch(/partial for view 1/);
    });

  });


  describe('chair_console', function() {

    beforeEach(function() {
      browser.get('index.html#/chair_console');
    });


    it('should render chair_console when user navigates to /chair_console', function() {
      expect(element.all(by.css('[ng-view] p')).first().getText()).
        toMatch(/partial for view 2/);
    });

  });
});
