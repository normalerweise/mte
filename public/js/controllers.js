/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.MyCtrl1 = function($scope, $http, $loading, toaster, $state) {
  $scope.companyResourceUris = [];
  $scope.settlementResourceUris = [];
  $scope.americanFootballPlayerResourceUris = [];

  $scope.getCompanies = function(){
     $loading.start('dbPediaCompanies');
     $http.get('/api/v1/sparql/query/predefined/resources-of-type-dbpedia-company/cached-result ').success(function(companies){
       $scope.companyResourceUris = companies;
       $loading.finish('dbPediaCompanies');
     });
  };

    $scope.getSettlements = function(){
       $loading.start('dbPediaCompanies');
       $http.get('/api/v1/sparql/query/predefined/resources-of-type-dbpedia-settlement/cached-result ').success(function(settlements){
         $scope.settlementResourceUris = settlements;
         $loading.finish('dbPediaCompanies');
       });
    };

        $scope.getAmericanFootballPlayers = function(){
           $loading.start('dbPediaCompanies');
           $http.get('/api/v1/sparql/query/predefined/resources-of-type-dbpedia-americanfootballplayer/cached-result ').success(function(players){
             $scope.americanFootballPlayerResourceUris = players;
             $loading.finish('dbPediaCompanies');
           });
        };

  $scope.queryCompanies = function(){
    $http.get('/api/v1/sparql/query/predefined/resources-of-type-dbpedia-company/trigger').success(function(){
      $loading.start('dbPediaCompanies');
      toaster.pop('success', "Query", "Triggered company query!", 3000);
      // simply reset -> new data will be received later
      $scope.companyResourceUris = [];
    });
  };

    $scope.querySettlements = function(){
      $http.get('/api/v1/sparql/query/predefined/resources-of-type-dbpedia-settlement/trigger').success(function(){
        $loading.start('dbPediaCompanies');
        toaster.pop('success', "Query", "Triggered settlement query!", 3000);
        // simply reset -> new data will be received later
        $scope.settlementResourceUris = [];
      });
    };

        $scope.queryAmericanFootballPlayers = function(){
          $http.get('/api/v1/sparql/query/predefined/resources-of-type-dbpedia-americanfootballplayer/trigger').success(function(){
            $loading.start('dbPediaCompanies');
            toaster.pop('success', "Query", "Triggered AmericanFootballPlayer query!", 3000);
            // simply reset -> new data will be received later
            $scope.americanFootballPlayerResourceUris = [];
          });
        };

  $scope.$on('serverEvent:executedSparqlQuery', function(event) {
    if(event.details.queryId = 'resources-of-type-dbpedia-company')
      $scope.getCompanies();
    else if(event.details.queryId = 'resources-of-type-dbpedia-settlement')
      $scope.getSettlements();
  });

  $scope.$on('serverEvent:exception', function() {
    $loading.finish('dbPediaCompanies');
  });

  $scope.currentPage = 0;
  $scope.pageSize = 500;
  $scope.numberOfPages =function() {
    return Math.ceil($scope.companyResourceUris.length/$scope.pageSize);
  };

  // execute on init
  if($state.current.url === "/query-company-resources") {
    $scope.getCompanies();
  }else if($state.current.url === "/query-settlement-resources") {
    $scope.getSettlements();
  }else if($state.current.url === "/query-americanfootballplayer-resources") {
       $scope.getAmericanFootballPlayers();
     }else{
     console.error("unknown get for state: " + $state.current.url)
     }

}
controllers.MyCtrl1.$inject = ['$scope', '$http', '$loading', 'toaster', '$state'];


controllers.MyCtrl2 = function($scope, $http, $loading, $filter, toaster, ngTableParams) {

  $scope.formData = {
    size: 100,
  };

      $scope.tableParams = new ngTableParams({
              page: 1,            // show first page
              count: 10,          // count per page
              sorting: {
                  createdOn: 'desc'     // initial sorting
              }
          }, {
              total: $scope.extractionRun.resources.length, // length of data
              getData: function ($defer, params) {
                  // use build-in angular filter
                  var filteredData = params.filter() ?
                          $filter('filter')($scope.extractionRun.resources, params.filter()) :
                          $scope.extractionRun.resources;
                  var orderedData = params.sorting() ?
                          $filter('orderBy')(filteredData, params.orderBy()) :
                          filteredData;

                  params.total(orderedData.length); // set total for recalc pagination
                  $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
              }
          });

   $scope.uploadComplete = function (content) {
      $loading.start('sample');
      toaster.pop('success', "Generate Sample", "Uploaded resource file!", 3000);
   };


  $scope.generateCompanySample = function(){
    var extractionRunId = $scope.extractionRunId();
    $http.get('/api/v1/extractionruns/' + extractionRunId + '/resources-random-sample',
      { params: {size: $scope.formData.size, queryId: 'resources-of-type-dbpedia-company'} }).success(function(sample){
        $loading.start('sample');
        toaster.pop('success', "Generate Sample", "Triggered sample generation!", 3000);
      // simply reset -> new data will be received later
      //$scope.sample.elements = [];
    });
  };

    $scope.generateSettlementSample = function(){
      var extractionRunId = $scope.extractionRunId();
      $http.get('/api/v1/extractionruns/' + extractionRunId + '/resources-random-sample',
        { params: {size: $scope.formData.size, queryId: 'resources-of-type-dbpedia-settlement'} }).success(function(sample){
          $loading.start('sample');
          toaster.pop('success', "Generate Sample", "Triggered sample generation!", 3000);
        // simply reset -> new data will be received later
        //$scope.sample.elements = [];
      });
    };

        $scope.generateAmericanFootballPlayerSample = function(){
          var extractionRunId = $scope.extractionRunId();
          $http.get('/api/v1/extractionruns/' + extractionRunId + '/resources-random-sample',
            { params: {size: $scope.formData.size, queryId: 'resources-of-type-dbpedia-americanfootballplayer'} }).success(function(sample){
              $loading.start('sample');
              toaster.pop('success', "Generate Sample", "Triggered sample generation!", 3000);
            // simply reset -> new data will be received later
            //$scope.sample.elements = [];
          });
        };

  $scope.$on('serverEvent:generatedSample', function() {
    $scope.updateExtractionRun();
  });

  $scope.$on('event:updatedExtractionRun', function() {
     $scope.tableParams.reload();
     $loading.finish('sample');
  });

  $scope.$on('serverEvent:exception', function() {
    $loading.finish('sample');
  });

}
controllers.MyCtrl2.$inject = ['$scope', '$http', '$loading', '$filter', 'toaster', 'ngTableParams'];

controllers.MyCtrl3 = function($scope, $http, $loading, toaster, ngTableParams, $filter) {
    var data = [];

    $scope.tableParams = new ngTableParams({
            page: 1,            // show first page
            count: 10,          // count per page
            sorting: {
                createdOn: 'desc'     // initial sorting
            }
        }, {
            total: data.length, // length of data
            getData: function ($defer, params) {
                // use build-in angular filter
                var filteredData = params.filter() ?
                        $filter('filter')(data, params.filter()) :
                        data;
                var orderedData = params.sorting() ?
                        $filter('orderBy')(filteredData, params.orderBy()) :
                        filteredData;

                params.total(orderedData.length); // set total for recalc pagination
                $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
            }
        });


  $scope.downloadSample = function(){
    var id = $scope.extractionRunId()
    $http.get('/api/v1/extractionruns/' + id + '/download').success(function(returnStatement){
      $scope.alert = "Started to download files: Press 'List' to update the view ";
    });
  };

  $scope.convertToTextSample = function(){
      var id = $scope.extractionRunId()
      $http.get('/api/v1/extractionruns/' + id + '/convert').success(function(returnStatement){
        $scope.alert = "Started to convert revisions: Press 'List' to update the view ";
      });
    };



    $scope.updateData = function(){
      $http.get('/api/v1/pages/download').success(function(returnStatement){
        $scope.alert = "Started to update all pages";
      });
    };

  $scope.listDownloadedData = function(){
     var id = $scope.extractionRunId();
     $loading.start('downloadedData');
     $http.get('/api/v1/extractionruns/' + id + '/page-data').success(function(ddata){
       data = ddata;
       $scope.dataLength = data.length;
       $scope.tableParams.reload();
       $loading.finish('downloadedData');
     });
  };

  $scope.$on('serverEvent:exception', function() {
    $loading.finish('downloadedData');
  });

   $scope.listDownloadedData()

}
controllers.MyCtrl3.$inject = ['$scope', '$http', '$loading', 'toaster', 'ngTableParams', '$filter'];

controllers.MyCtrl4 = function($scope, $http, ngTableParams, $filter, toaster) {
   var sample = [];
    $scope.tableParams = new ngTableParams({
            page: 1,            // show first page
            count: 10,          // count per page
            sorting: {
                createdOn: 'desc'     // initial sorting
            }
        }, {
            total: sample.length, // length of data
            getData: function ($defer, params) {
                // use build-in angular filter
                var filteredData = params.filter() ?
                        $filter('filter')(sample, params.filter()) :
                        sample;
                var orderedData = params.sorting() ?
                        $filter('orderBy')(filteredData, params.orderBy()) :
                        filteredData;

                params.total(orderedData.length); // set total for recalc pagination
                $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
            }
        });


  $scope.extractSample = function(){
    var id = $scope.extractionRunId();
    $http.get('/api/v1/extractionruns/' + id + '/infobox-extraction').success(function(returnStatement){
      $scope.alert = "Started to extract files: Press 'List' to update the view ";
    });
  };

  $scope.listExtractedData = function(){
     var id = $scope.extractionRunId();
     $http.get('/api/v1/extractionruns/' + id + '/list-results').success(function(newSample){
       sample = newSample;
       $scope.sampleSize = sample.length
       $scope.tableParams.reload();
     });
  };

   $scope.convertToRDF = function() {
     var id = $scope.extractionRunId();
     $http.get('/api/v1/extractionruns/' + id + '/quads-to-rdf').success(function(res){
       toaster.pop('success', "Process Results", "Triggered RDF conversion!", 3000);
     });
   }

      $scope.convertToDistinctRDF = function() {
        var id = $scope.extractionRunId();
        $http.get('/api/v1/extractionruns/' + id + '/distinct-quads-to-rdf').success(function(res){
          toaster.pop('success', "Process Results", "Triggered distinct RDF conversion!", 3000);
        });
      }

   $scope.extractSampleOccurences = function() {
   var id = $scope.extractionRunId();
        $http.get('/api/v1/extractionruns/' + id + '/find-samples').success(function(res){
          toaster.pop('success', "Process Results", "Triggered Sample Occurence extraction!", 3000);
        });
   }

  $scope.listExtractedData();

}
controllers.MyCtrl4.$inject = ['$scope', '$http','ngTableParams', '$filter', 'toaster'];

controllers.MyCtrl5 = function($scope, $http, ngTableParams, $filter, $loading) {
  var statsPerPage = [];

    $scope.tableParams = new ngTableParams({
              page: 1,            // show first page
              count: 10,          // count per page
              sorting: {
                  createdOn: 'desc'     // initial sorting
              }
          }, {
              total: statsPerPage.length, // length of data
              getData: function ($defer, params) {
                  // use build-in angular filter
                  var filteredData = params.filter() ?
                          $filter('filter')(statsPerPage, params.filter()) :
                          statsPerPage;
                  var orderedData = params.sorting() ?
                          $filter('orderBy')(filteredData, params.orderBy()) :
                          filteredData;

                  params.total(orderedData.length); // set total for recalc pagination
                  $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
              }
          });


  $scope.getSampleStats= function(){
    var id = $scope.extractionRunId();
    $loading.start('stats');
    $http.get('/api/v1/extractionruns/' + id + '/stats').success(function(stats){
      statsPerPage = stats.perPage
      $scope.stats = stats.aggregated;
      $scope.noOfPages = stats.noOfPages;
      $scope.tableParams.reload();
      $loading.finish('stats');
    });
  };

  $scope.getSampleStats()

}
controllers.MyCtrl5.$inject = ['$scope', '$http','ngTableParams', '$filter', '$loading'];

controllers.Events = function($rootScope, $scope, $http, toaster) {

 init();

 function init() {
   initVars();
   connect();
   registerHandlers();
 }

 function initVars() {
   $scope.events = [];
 }

 function connect() {
   $scope.source = new EventSource('/api/v1/events');
 }

 function registerHandlers() {
   $scope.source.addEventListener('message', defaultHandler, false);
   //$scope.source.addEventListener('queriedDBpdeiaCompanies', defaultHandler, false);
 }

 function defaultHandler(event) {
   event = parseData(event)
   $scope.events.unshift(event);
   var length = $scope.events.length
   if( length > 150) {
    $scope.events.splice(50,(length - 50));
   }

   popToastIfException(event)
   broadcastIfWhitelisted(event)

   $scope.$apply();
 }

 function broadcastIfWhitelisted(event) {
   if(event.type === "exception" ||
      event.type === "generatedSample" ||
      event.type === "updatedExtractionRun" ||
      event.type === "executedSparqlQuery" ) {
     $rootScope.$broadcast('serverEvent:' + event.type, event);
   }
 }

 function parseData(event) {
   var eventData = JSON.parse(event.data);
   eventData.timestamp = moment(eventData.timestamp).format();
   return eventData;
 }

 function popToastIfException(event) {
   if(event.type === "exception"){
     toaster.pop('error', "Exception", event.description, 7000);
   }
 }

}
controllers.Events.$inject = ['$rootScope','$scope', '$http', 'toaster'];


controllers.ExtractionCtrl= function($rootScope, $scope, $state, $stateParams, $location, $http) {

  $scope.setExtractionRun = function(extractionRun) {
    extractionRun.createdOnMomentCalendar = moment(extractionRun.createdOn).calendar();
    angular.extend($scope.extractionRun, extractionRun);
    $location.search('extractionRunId', $scope.extractionRun._id)
    $rootScope.$broadcast('event:updatedExtractionRun', extractionRun);
  };

  $scope.setExtractionRunById = function(id) {
    $http.get('/api/v1/extractionruns/' + id).success(function(run){
      $scope.setExtractionRun(run)
    });
  };

  $scope.updateExtractionRun = function() {
    $scope.setExtractionRunById($scope.extractionRun._id)
  };

  $scope.extractionRunId = function() {
      if($scope.extractionRun._id) {
        return $scope.extractionRun._id
      }else if($stateParams.extractionRunId){
        return $stateParams.extractionRunId
      }else{
        return null
      }
    };


  init();

  function init() {
    $scope.extractionRun = { resources: []};
    if($stateParams.extractionRunId) {
      $scope.setExtractionRunById($stateParams.extractionRunId);
    }
  }

}
controllers.ExtractionCtrl.$inject = ['$rootScope', '$scope', '$state', '$stateParams', '$location', '$http'];


controllers.SemistructuredExtractionCtrl = function($scope, $state) {

  $scope.tabs = [
    { state: 'overview', title: 'Select Extraction Run' },
    { state: 'generateSample', title: 'Select Resources' },
    { state: 'downloadWikiRevisions', title: 'Download' },
    { state: 'extract', title: 'Extract' },
    { state: 'stats', title: 'Statistics' }
  ]

  $scope.isActive = function(state) {
    return $state.current.name == 'extraction.semistructured.' + state;
  };

}
controllers.SemistructuredExtractionCtrl.$inject = ['$scope', '$state'];


controllers.DBpediaMasterdata = function($scope, $state) {

  $scope.tabs = [
    { state: 'queryCompanyResources', title: 'Query Company Resources' },
    { state: 'querySettlementResources', title: 'Query Settlement Resources' },
    { state: 'queryAmericanFootballPlayerResources', title: 'Query American Football Player Resources' }
  ]

  $scope.isActive = function(state) {
    return $state.current.name == 'masterdata.dbpedia.' + state;
  };

}
controllers.DBpediaMasterdata.$inject = ['$scope', '$state'];

controllers.ExtractionRunCtrl = function($scope, $state, $http, $loading, $filter, $q, toaster, ngTableParams) {
  var extractionRuns = [];

  $scope.formData = { description: 'New Extraction Run'}

  $scope.tableParams = new ngTableParams({
          page: 1,            // show first page
          count: 10,          // count per page
          sorting: {
              createdOn: 'desc'     // initial sorting
          }
      }, {
          total: extractionRuns.length, // length of data
          getData: function ($defer, params) {
              // use build-in angular filter
              var filteredData = params.filter() ?
                      $filter('filter')(extractionRuns, params.filter()) :
                      extractionRuns;
              var orderedData = params.sorting() ?
                      $filter('orderBy')(filteredData, params.orderBy()) :
                      filteredData;

              params.total(orderedData.length); // set total for recalc pagination
              $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
      });

   $scope.getRuns = function(){
       $loading.start('extractionRuns');
       $http.get('/api/v1/extractionruns').success(function(runs){
         angular.forEach(runs, function(run,key) {
           parseData(run);
         }),
         extractionRuns = runs;
         $scope.tableParams.reload();
         $loading.finish('extractionRuns');
       });
    };

  $scope.create = function(description) {
   $http.post('/api/v1/extractionruns',{description: $scope.formData.description})
     .success(function(extractionRun){
        extractionRun = parseData(extractionRun);
        $scope.setExtractionRun(extractionRun);
        extractionRuns.push(extractionRun);
        $scope.tableParams.reload();
        toaster.pop('success', "Extraction Run", "Created Extraction Run!", 3000);
      });
  }

  $scope.changeSelection = function(extractionRun) {
    extractionRun.$selected = true;
    $scope.setExtractionRun(extractionRun);
    angular.forEach(extractionRuns, function(element){
      if (element._id !== extractionRun._id) {
        element.$selected = false;
      }
    });
    toaster.pop('success', "Extraction Run", "Selected Extraction Run!", 3000);
  }

   $scope.delete = function(run) {
     $http.delete('/api/v1/extractionruns/' + run._id)
       .success(function(extractionRun){
         var idx = extractionRuns.indexOf(run);
          if (idx != -1) {
             extractionRuns.splice(idx, 1); // The second parameter is the number of elements to remove.
             $scope.deleteSelection();
             $scope.tableParams.reload();
          toaster.pop('success', "Extraction Run", "Deleted Extraction Run!", 3000);
         }
        });
    }

    $scope.deleteSelection = function() {
        //$scope.setExtractionRun(null);
        angular.forEach(extractionRuns, function(element){
            element.$selected = false;
        });
      }


  function parseData(run) {
    run.createdOnMomentCalendar = moment(run.createdOn).calendar();
    return run;
  }

  // initial Load
  $scope.getRuns()

}
controllers.ExtractionRunCtrl.$inject = ['$scope', '$state', '$http', '$loading',  '$filter', '$q', 'toaster', 'ngTableParams'];

return controllers;

});