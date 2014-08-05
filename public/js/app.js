/*global define, angular */

'use strict';

require(['angular', './controllers' ,'./directives', './filters', './services', 'angular-animate', 'angular-ui-router', 'ui-bootstrap-tpls' ],
  function(angular, controllers) {
    require(['./lib/angular-toaster', './lib/angular-loading'], function() {
    // Declare app level module which depends on filters, and services
    
    angular.module('myApp', ['myApp.filters', 'myApp.services', 'myApp.directives', 'ui.router', 'ui.bootstrap', 'toaster', 'darthwade.dwLoading'])
      .config(function($stateProvider, $urlRouterProvider) { $stateProvider
        .state('extraction', {
          url: "/extraction",
          templateUrl: "partials/extraction.html"

        })
        .state('extraction.semistructured', {
          url: "/semistructured",
          views: {
            "": {
              templateUrl: "partials/extraction_semistructured.html",
              controller: controllers.SemistructuredExtraction
            },
            "events": {
              templateUrl: "partials/events.html",
              controller: controllers.Events
            }
          }
        })
        .state('extraction.semistructured.overview', {
          url: "/overview",
          templateUrl: "partials/extraction_semistructured_overview.html"
        })
        .state('extraction.semistructured.queryDBpedia', {
          url: "/querydbpedia",
          templateUrl: 'partials/partial1.html',
          controller: controllers.MyCtrl1
        })
        .state('extraction.semistructured.generateSample', {
          url: "/sample",
          templateUrl: 'partials/partial2.html',
          controller: controllers.MyCtrl2
        })
        .state('extraction.semistructured.downloadWikiRevisions', {
          url: "/download",
          templateUrl: 'partials/partial3.html',
          controller: controllers.MyCtrl3
        })
        .state('extraction.semistructured.extract', {
          url: "/extract",
          templateUrl: 'partials/partial4.html',
          controller: controllers.MyCtrl4
        })
        .state('extraction.semistructured.stats', {
          url: "/stats",
          templateUrl: 'partials/partial5.html',
          controller: controllers.MyCtrl5
        })

        $urlRouterProvider.otherwise("/extraction/semistructured/overview");
    });

     //We already have a limitTo filter built-in to angular,
      //let's make a startFrom filter
      angular.module('myApp').filter('startFrom', function() {
          return function(input, start) {
              start = +start; //parse to int
              return input.slice(start);
          }
      });

      
    angular.bootstrap(document, ['myApp']);
});
});
