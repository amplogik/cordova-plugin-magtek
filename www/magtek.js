/*global cordova*/

var connected = false;

// Functions to wire native callbacks to JavaScript
var connectedCallback = function() {
    connected = true;
    if (module.exports.onConnected) {
        module.exports.onConnected();
    } else {
        console.log('onConnected');
    }
};

var disconnectedCallback = function() {
    connected = false;
    if (module.exports.onDisconnected) {
        module.exports.onDisconnected();
    } else {
        console.log('onDisconnected');
    }
};

var errorCallback = function(error) {
    if (module.exports.onError) {
        module.exports.onError(error);
    } else {
        console.log('onError ' + error);
    }
};

var cardSwipeDetectedCallback = function(json) {
    if (module.exports.onCardSwipeDetected) {
        module.exports.onCardSwipeDetected(json);
    } else {
        console.log('onCardSwipeDetected ' + JSON.stringify(json, null, 4));
    }
};

// Error callback generator for register functions
var _failure = function(name) {
    return function() {
        console.log("Failed to add " + name);
    };
};

// Code to wire Cordova callbacks to JavaScript callbacks
cordova.exec(
    connectedCallback,
    _failure('registerConnectedCallback'),
    'MagTek', 'registerConnectedCallback', []);

cordova.exec(
    disconnectedCallback,
    _failure('registerDisconnectedCallback'),
    'MagTek', 'registerDisconnectedCallback', []);

cordova.exec(
    errorCallback,
    _failure('registerErrorCallback'),
    'MagTek', 'registerErrorCallback', []);

cordova.exec(
    cardSwipeDetectedCallback,
    _failure('registerCardSwipeCallback'),
    'MagTek', 'registerCardSwipeCallback', []
);

// check permissions is required for Android to initialize the plugin
// also calling on iOS for consistent behavior when permission denied
cordova.exec(
    function() { console.log("MagTek plugin initialized"); },
    function() { errorCallback("Record audio permission denied"); },
    "MagTek", "checkPermissions", []
);

module.exports = {

    // users should override these functions
    onCardSwipeDetected: function(data) {},
    onConnected: function() {},
    onDisconnected: function() {},
    onError: function(error) {},

    checkPermissions: function (success, failure) {
        cordova.exec(success, failure, "MagTek", "checkPermissions", []);
    },

    isConnected: function() {
        return connected;
    },

    connected: function(success, failure) {
        cordova.exec(success, failure, "MagTek", "connected", []);
    },

    start: function (success, failure) {
        cordova.exec(success, failure, "MagTek", "start", []);
    },

    stop: function (success, failure) {
        cordova.exec(success, failure, "MagTek", "stop", []);
    }, 

    restart: function (success, failure) {

        magtek.stop(
            function() {
                setTimeout(magtek.start, 300, success, failure);
            },
            failure
        );
    },

    // this function only exists for compatibilty with the ROAM plugin
    // it always calls the success callback
    waitForCardSwipe: function (success, failure) {
        success();
    },

    // this function only exists for compatibilty with the ROAM plugin
    // it always calls the success callback
    stopWaitingForCardSwipe: function (success, failure) {
        success();
    }

};
