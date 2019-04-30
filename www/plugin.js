cordova.define("cordova-plugin-geofence-fm.plugin", function(require, exports, module) {

  var exec = require('cordova/exec');
  
  module.exports = {
    init: function init(successCallback, errorCallback){
      exec(successCallback, errorCallback, 'GeofenceFM', "init", []);
    },
    addOrUpdateFence: function addOrUpdateFence(data, successCallback, errorCallback){
      exec(successCallback, errorCallback, 'GeofenceFM', "addOrUpdateFence", [data]);
    },
    removeGeofence: function removeGeofence(data, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'GeofenceFM', 'removeGeofence', [data]);
      }
  };
  
  });
  