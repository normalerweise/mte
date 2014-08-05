/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.MyCtrl1 = function($scope, $http, $loading, toaster) {
  $scope.companyResourceUris = [];

  $scope.getCompanies = function(){
     $loading.start('dbPediaCompanies');
     $http.get('/api/v1/companies').success(function(companies){
       $scope.companyResourceUris = companies;
       $loading.finish('dbPediaCompanies');
     });
  };

  $scope.queryCompanies = function(){
    $http.get('/api/v1/query/dbpedia/companies').success(function(){
      $loading.start('dbPediaCompanies');
      toaster.pop('success', "Company Query", "Triggered company query!", 3000);
      // simply reset -> new data will be received later
      $scope.companyResourceUris = [];
    });
  };

  $scope.$on('serverEvent:queriedDBpdeiaCompanies', function() {
    $scope.getCompanies();
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
  $scope.getCompanies();

}
controllers.MyCtrl1.$inject = ['$scope', '$http', '$loading', 'toaster'];


controllers.MyCtrl2 = function($scope, $http, $loading, toaster) {
  $scope.sample = {
    size: 100,
    elements: []
  };

  $scope.getSample = function(){
     $loading.start('sample');
     $http.get('/api/v1/sample').success(function(sample){
       $scope.sample.elements = sample;
       $loading.finish('sample');
     });
  };

  $scope.generateSample = function(){
    $http.get('/api/v1/sample/generate',{ params: {size: $scope.sample.size} }).success(function(sample){
      $loading.start('dbPediaCompanies');
      toaster.pop('success', "Generate Sample", "Triggered sample generation!", 3000);
      // simply reset -> new data will be received later
      $scope.sample.elements = [];
    });
  };

  $scope.$on('serverEvent:generatedSample', function() {
    $scope.getSample();
  });

  $scope.$on('serverEvent:exception', function() {
    $loading.finish('sample');
  });

  $scope.currentPage = 0;
  $scope.pageSize = 100;
  $scope.numberOfPages= function() {
    return Math.ceil($scope.sample.elements.length/$scope.pageSize);
  };

  // execute on init
  $scope.getSample();

}
controllers.MyCtrl2.$inject = ['$scope', '$http', '$loading', 'toaster'];

controllers.MyCtrl3 = function($scope, $http, $loading, toaster) {

  $scope.downloadSample = function(){
    $http.get('/api/v1/sample/download').success(function(returnStatement){
      $scope.alert = "Started to download files: Press 'List' to update the view ";
    });
  };

  $scope.listDownloadedData = function(){
     $loading.start('downloadedData');
     $http.get('/api/v1/pages').success(function(data){
       $scope.data = data;
       $loading.finish('downloadedData');
     });
  };

  $scope.listDownloadedSampleData = function(){
    $http.get('/api/v1/sample/revdata').success(function(sample){
      $scope.sample = sample;
    });
  };

  $scope.$on('serverEvent:exception', function() {
    $loading.finish('downloadedData');
  });

   $scope.listDownloadedData()

}
controllers.MyCtrl3.$inject = ['$scope', '$http', '$loading', 'toaster'];

controllers.MyCtrl4 = function($scope, $http) {

  $scope.extractSample = function(){
    $http.get('/api/v1/sample/extract').success(function(returnStatement){
      $scope.alert = "Started to extract files: Press 'List' to update the view ";
    });
  };

  $scope.listExtractedData = function(){
     $http.get('/api/v1/sample/extrdata').success(function(sample){
       $scope.sample = sample;
     });
  };

}
controllers.MyCtrl4.$inject = ['$scope', '$http'];

controllers.MyCtrl5 = function($scope, $http) {

  $scope.getSampleStats= function(){
    $http.get('/api/v1/sample/stats').success(function(stats){
      $scope.stats = stats;
    });
  };

}
controllers.MyCtrl5.$inject = ['$scope', '$http'];

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
   if($scope.events.length > 100) {
    $scope.events.splice(-1,1);
   }

   popToastIfException(event)

   $rootScope.$broadcast('serverEvent:' + event.type, event);
   $scope.$apply();
 }

 function parseData(event) {
   var eventData = JSON.parse(event.data);
   eventData.timestamp = moment(eventData.timestamp);
   return eventData;
 }

 function popToastIfException(event) {
   if(event.type === "exception"){
     toaster.pop('error', "Exception", event.description, 7000);
   }
 }

}
controllers.Events.$inject = ['$rootScope','$scope', '$http', 'toaster'];



controllers.SemistructuredExtraction= function($scope, $state) {

$scope.tabs = [
  { state: 'overview', title: 'Overview' },
  { state: 'queryDBpedia', title: 'Query' },
  { state: 'generateSample', title: 'Sample' },
  { state: 'downloadWikiRevisions', title: 'Download' },
  { state: 'extract', title: 'Extract' },
  { state: 'stats', title: 'Statistics' }
]

$scope.isActive = function(state) {
  return $state.current.name == 'extraction.semistructured.' + state;
};

}
controllers.SemistructuredExtraction.$inject = ['$scope', '$state'];


return controllers;

});