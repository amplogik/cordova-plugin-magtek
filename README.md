# MagTek Plugin for Apache Cordova

This plugin supports MagTek audio credit card scanner. It provides a simple JavaScript API for scanning credit cards on iOS and Android.

## Supported Platforms

* iOS
* Android (4.3 or greater)

# Installing

### Cordova

    $ cordova plugin add https://github.com/salonsuitesolutions/cordova-plugin-magtek

### PhoneGap

    $ phonegap plugin add https://github.com/salonsuitesolutions/cordova-plugin-magtek

### PhoneGap Build

Edit config.xml to install the plugin for [PhoneGap Build](http://build.phonegap.com).

    <gap:plugin name="com.salonsuitesolutions.magtek" source="pgb" />
    <preference name="phonegap-version" value="cli-6.1.0" />

### iOS 10

For iOS 10, apps will crash unless they include usage description keys for the types of data they access. For this plugin, NSMicrophoneUsageDescription must be defined.

This can be done when the plugin is installed using the MICROPHONE_USAGE_DESCRIPTION variable.

    $ cordova plugin add https://github.com/salonsuitesolutions/cordova-plugin-magtek --variable MICROPHONE_USAGE_DESCRIPTION="Your description here"

For PhoneGap Build, add the NSMicrophoneUsageDescription to config.xml.

    <gap:config-file platform="ios" parent="NSMicrophoneUsageDescription" overwrite="true">
        <string>App would like to access the microphone to use the credit card swiper.</string>
    </gap:config-file>

# API Reference

## Functions

- [magtek.onCardSwipeDetected](#oncardswipedetected)
- [magtek.onConnected](#onconnected)
- [magtek.onDisconnected](#ondisconnected)
- [magtek.onError](#onerror)
- [magtek.isConnected](#isconnected)
- [magtek.checkPermissions](#checkpermissions)
- [magtek.start](#stop)
- [magtek.stop](#start)

### start

Function `start` is used to programatically start MagTek reader.  This function must be called in order for the plugin to detect the reader being connected or disconnected.

The success callback is called one time if the reader is initialized and is ready to scan.

The failure callback is called if the reader can not be restarted.

The [onConnected](#onconnected) callback is called when reader starts.

### stop

Function `stop` is used to programatically stop MagTek reader.  The [start](#start) function must be called before scanning again. 

The success callback is called one time when the reader is disabled.

The [onDisconnected](#ondisconnected) callback is also be called when the reader stops.

### onConnected

Function `onConnected` is called when the credit card reader is connected to the phone. If the card reader is plugged in when the app launches, `onConnected` will be called.

    magtek.onConnected = function(){
        // update UI to indicate reader is available
    }

### onDisconnected

Function `onDisconnected` is called when the credit card reader is removed from the phone.

    magtek.onDisconnected = function() {
        // update UI to indicate reader is NOT available
    }

### onError

Function `onError` is called when the there is a plugin error.

If the user has denied the record audio permissions, this function will be called during plugin initialization with the message "Record audio permission denied".

This callback is optional. Your application does *not* need to implement this function.

    magtek.onError = function(message) {
        // update UI to indicating the error
    }

### isConnected

Function `isConnected` is used to determine if the card reader is connected. It returns true when the reader is connected and false when the reader is not connected.

    if (magtek.isConnected()) {
        message = "The card reader is connected."
    } else {
        message = "The card reader is NOT connected."
    }
    console.log(message);

### checkPermissions

Function `checkPermissions` is used to determine if the user granted permission for the app to record audio. The success callback is called when the permission is granted. The failure callback is called when the permissions is denied.

The MagTek libraries require the audio recording permission to interact with the reader hardware.

iOS users will be asked one time to grant audio permission. The system remembers the users choice. If the user has denied recording permission, they can reenable it in Settings > Privacy > Microphone.

Android 6 users will be prompted to grant audio recording permission. If the user denies the permission, they will be asked again each time unless they check "Never ask again." The user can re-enable permissions in Settings > Apps > *App Name* > Permissions > Microphone.

The record audio permission is always enabled for Android 4 and 5 users.

The plugin will check permissions when it starts. If permissions are denied it will call [onError](#onerror) with "Record audio permission denied". 

        magtek.checkPermissions(
            function() {
                console.log("Record audio permission granted");
            },
            function() {
                console.log("Record audio permission denied");
            }
        );

### onCardSwipeDetected

Function `onCardSwipeDetected` is called when the reader detects a card swipe. A JSON object is passed to the registered function.

    magtek.onCardSwipeDetected = function(cardDataAsJson)) {
        // update the UI with some card data
        // send encrypted card data to the server
    }

Ideally the callback receives a successful card scan. Ensure that Track1Status and Track2Status are both "0" before continuing. The read may fail for one track and work for the other. If there is an error, the JSON will contain the error message.

This plugin is wrapping the native libraries for the MagTek reader, so the MagTek documentation will be the most definitive source on how to interpret the scan data.

## Examples Scans

Successful Card Read

Note that "Track1.Status" and "Track2.Status" should be "00" after a successful scan.

    {
        "Tracks.Masked": "%B5248250003001692^COLEMAN/DON^2102101000000000000000000000000000000000000000?;5248250003001692=21021010000000000000?",
        "Track1.Encrypted": "0BEF83A7B291182B49CB379E3B52614FF410939790E973486EBC560BF954AEBBFD604237390A8D1D4729B9B442B88FCE264012CEAC6A25FB",
        "Track2.Encrypted": "C66FB5A6D2E81FB637847F496111729F5A2DAE465B4E3DA4BD62087423B5694447577A1026E97001",
        "Track3.Encrypted": "",
        "Track1.Masked": "%B5248250003001692^COLEMAN/DON^2102101000000000000000000000000000000000000000?",
        "Track2.Masked": ";5248250003001692=21021010000000000000?",
        "Track3.Masked": "",
        "MagnePrint.Encrypted": "",
        "MagnePrint.Status": "",
        "Device.Serial": "2E9CFB0430000D00",
        "Session.ID": "",
        "KSN": "9011140B24670F000179",
        "Device.Name": "MagTek aDynamo V1.0",
        "Swipe.Count": 113,
        "Cap.MagnePrint": "",
        "Cap.MagnePrintEncryption": "",
        "Cap.MagneSafe20Encryption": "",
        "Cap.MagStripeEncryption": "",
        "Cap.MSR": "",
        "Cap.Tracks": "",
        "Card.Data.CRC": 0,
        "Card.Exp.Date": "2102",
        "Card.IIN": "524825",
        "Card.Last4": "1234",
        "Card.Name": "COLEMAN/DON",
        "Card.PAN": "5248250003001234",
        "Card.PAN.Length": 16,
        "Card.Service.Code": "101",
        "Card.Status": "00",
        "HashCode": "",
        "Data.Field.Count": 0,
        "Encryption.Status": "0206",
        "Firmware": "21043017A01",
        "MagTek.Device.Serial": "",
        "Response.Type": "C101",
        "TLV.Version": "0002",
        "Track.Decode.Status": "000002",
        "Track1.Status": "00",
        "Track2.Status": "00",
        "Track3.Status": "02",
        "SDK.Version": "100.16",
        "Battery.Level": 92,
        "TLV Payload": "FA8199DFDF251032453943464230343330303030443030F48182DFDF37380BEF83A7B291182B49CB379E3B52614FF410939790E973486EBC560BF954AEBBFD604237390A8D1D4729B9B442B88FCE264012CEAC6A25FBDFDF3928C66FB5A6D2E81FB637847F496111729F5A2DAE465B4E3DA4BD62087423B5694447577A1026E97001DFDF3B00DFDF3C00DFDF3D00DFDF500A9011140B24670F000179"
    }

Bad Scan. There was no error, but most data is missing. 

Note that "Track1.Status" and "Track2.Status" are both "01" indicating a failed read.

    {
        "Tracks.Masked": "",
        "Track1.Encrypted": "5DBFC9347CF72C5C",
        "Track2.Encrypted": "08BA8C3AC39394ED",
        "Track3.Encrypted": "",
        "Track1.Masked": "",
        "Track2.Masked": "",
        "Track3.Masked": "",
        "MagnePrint.Encrypted": "",
        "MagnePrint.Status": "",
        "Device.Serial": "2E9CFB0430000D00",
        "Session.ID": "",
        "KSN": "9011140B24670F00017B",
        "Device.Name": "MagTek aDynamo V1.0",
        "Swipe.Count": 115,
        "Cap.MagnePrint": "",
        "Cap.MagnePrintEncryption": "",
        "Cap.MagneSafe20Encryption": "",
        "Cap.MagStripeEncryption": "",
        "Cap.MSR": "",
        "Cap.Tracks": "",
        "Card.Data.CRC": 0,
        "Card.Exp.Date": "",
        "Card.IIN": "",
        "Card.Last4": "",
        "Card.Name": "",
        "Card.PAN": "",
        "Card.PAN.Length": 0,
        "Card.Service.Code": "",
        "Card.Status": "05",
        "HashCode": "",
        "Data.Field.Count": 0,
        "Encryption.Status": "0206",
        "Firmware": "21043017A01",
        "MagTek.Device.Serial": "",
        "Response.Type": "C101",
        "TLV.Version": "0002",
        "Track.Decode.Status": "010102",
        "Track1.Status": "01",
        "Track2.Status": "01",
        "Track3.Status": "02",
        "SDK.Version": "100.16",
        "Battery.Level": 84,
        "TLV Payload": "FA48DFDF251032453943464230343330303030443030F432DFDF37085DBFC9347CF72C5CDFDF390808BA8C3AC39394EDDFDF3B00DFDF3C00DFDF3D00DFDF500A9011140B24670F00017B"
    }

