/*global define, angular */

'use strict';

require(['angular', './controllers' ,'./directives', './filters', './services', 'angular-animate', 'angular-ui-router', 'ui-bootstrap-tpls' ],
  function(angular, controllers) {
    require(['./lib/angular-toaster', './lib/angular-loading', './lib/ng-table', './lib/ng-upload'], function() {
    // Declare app level module which depends on filters, and services
    
    angular.module('myApp', ['myApp.filters', 'myApp.services', 'myApp.directives', 'ui.router', 'ui.bootstrap', 'toaster', 'darthwade.dwLoading', 'ngTable', 'ngUpload'])
      .config(function($stateProvider, $urlRouterProvider) { $stateProvider
        .state('withNavbar', {
          abstract: true,
          templateUrl: "partials/with_navbar.html"
        })
        .state('extraction', {
          parent: 'withNavbar',
          url: "/extraction?extractionRunId",
          reloadOnSearch: false,
          templateUrl: "partials/extraction.html",
          controller: controllers.ExtractionCtrl
        })
        .state('extraction.semistructured', {
          url: "/semistructured",
          views: {
            "": {
              reloadOnSearch: false,
              templateUrl: "partials/tabbed_view.html",
              controller: controllers.SemistructuredExtractionCtrl
            },
            "events": {
              templateUrl: "partials/events.html",
              controller: controllers.Events
            }
          }
        })
        .state('extraction.semistructured.overview', {
          url: "/overview",
          templateUrl: "partials/extraction_semistructured_overview.html",
          controller: controllers.ExtractionRunCtrl
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
        .state('masterdata',{
           parent: 'withNavbar',
           url: "/masterdata",
           templateUrl: "partials/masterdata.html"

        })
        .state('masterdata.dbpedia',{
          url: "/dbpedia",
          views: {
            "": {
              templateUrl: "partials/tabbed_view.html",
              controller: controllers.DBpediaMasterdata
            },
            "events": {
              templateUrl: "partials/events.html",
              controller: controllers.Events
            }
          }
        })
        .state('masterdata.dbpedia.queryCompanyResources', {
          url: "/query-company-resources",
          templateUrl: 'partials/partial1.html',
          controller: controllers.MyCtrl1
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
