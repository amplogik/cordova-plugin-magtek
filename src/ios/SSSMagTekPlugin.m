#import "SSSMagTekPlugin.h"
#import <Cordova/CDV.h>

@interface SSSMagTekPlugin() {
    NSString *cardSwipeCallback;
    NSString *connectedCallback;
    NSString *disconnectedCallback;
    NSString *errorCallback;
}
@property (nonatomic, assign, getter=isStarted) BOOL started;
//@property (nonatomic, assign) float savedVolume;
@property (nonatomic, strong) MTSCRA* magtek;
@end

@implementation SSSMagTekPlugin


- (void)pluginInitialize {

    NSLog(@"Cordova MagTek Plugin");
    NSLog(@"(c)2017 Salon Suite Solutions");

    [super pluginInitialize];

    self.magtek = [MTSCRA new];
    self.magtek.delegate = self;
    [self.magtek listenForEvents:(TRANS_EVENT_OK|TRANS_EVENT_START|TRANS_EVENT_ERROR)];
    
    [self.magtek setDeviceType:(MAGTEKAUDIOREADER)];
    self.started = false;
}

- (void)checkPermissions:(CDVInvokedUrlCommand*)command {
    
    [[AVAudioSession sharedInstance] requestRecordPermission:^(BOOL granted) {
        
        CDVPluginResult *pluginResult;

        if (granted) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
                            
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                            
    }];
    
}

- (void)start:(CDVInvokedUrlCommand *)command {
    
    [self setStarted:YES];
    BOOL deviceOpened;
    CDVPluginResult *pluginResult;
    if(!_magtek.isDeviceOpened) {
        deviceOpened = [_magtek openDevice];
    } else {
        NSLog(@"MagTek device was already open");
        deviceOpened = true;
    }

    if (deviceOpened) {
        NSLog(@"MagTek device open");
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Card Reader failed to reinitialize."];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)stop:(CDVInvokedUrlCommand *)command {
    
    [self setStarted:NO];
    [_magtek closeDevice];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    
}

- (void)registerCardSwipeCallback:(CDVInvokedUrlCommand*)command {

    cardSwipeCallback = [command.callbackId copy];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [pluginResult setKeepCallbackAsBool: TRUE];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)registerConnectedCallback:(CDVInvokedUrlCommand*)command {

    connectedCallback = [command.callbackId copy];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [pluginResult setKeepCallbackAsBool: TRUE];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)registerDisconnectedCallback:(CDVInvokedUrlCommand*)command {

    disconnectedCallback = [command.callbackId copy];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [pluginResult setKeepCallbackAsBool: TRUE];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)registerErrorCallback:(CDVInvokedUrlCommand*)command {

    errorCallback = [command.callbackId copy];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [pluginResult setKeepCallbackAsBool: TRUE];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

# pragma mark util

- (void)sendError:(NSString *)message {

    if (errorCallback) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString: message];
        [pluginResult setKeepCallbackAsBool: TRUE];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:errorCallback];
    } else {
        NSLog(@"errorCallback is missing %@", message);
    }

}

#pragma mark MTSCRAEventDelegate

-(void)onDataReceived:(MTCardData *)cardDataObj instance:(id)instance
{
    NSMutableDictionary *data = [NSMutableDictionary dictionary];
    [data setObject: cardDataObj.trackDecodeStatus forKey: @"Track.Status"];
    [data setObject: cardDataObj.track1DecodeStatus forKey: @"Track1.Status"];
    [data setObject: cardDataObj.track2DecodeStatus forKey: @"Track2.Status"];
    [data setObject: cardDataObj.track3DecodeStatus forKey: @"Track3.Status"];
    [data setObject: cardDataObj.encryptionStatus forKey: @"Encryption.Status"];
    [data setObject: [NSNumber numberWithLong:cardDataObj.batteryLevel] forKey: @"Battery.Level"];
    [data setObject: [NSNumber numberWithLong:cardDataObj.swipeCount] forKey: @"Swipe.Count"];
    [data setObject: cardDataObj.maskedTracks forKey: @"Track.Masked"];
    [data setObject: cardDataObj.maskedTrack1 forKey: @"Track1.Masked"];
    [data setObject: cardDataObj.maskedTrack2 forKey: @"Track2.Masked"];
    [data setObject: cardDataObj.maskedTrack3 forKey: @"Track3.Masked"];
    [data setObject: cardDataObj.encryptedTrack1 forKey: @"Track1.Encrypted"];
    [data setObject: cardDataObj.encryptedTrack2 forKey: @"Track2.Encrypted"];
    [data setObject: cardDataObj.encryptedTrack3 forKey: @"Track3.Encrypted"];
    [data setObject: cardDataObj.cardPAN forKey: @"Card.PAN"];
    [data setObject: cardDataObj.encryptedMagneprint forKey: @"MagnePrint.Encrypted"];
    [data setObject: [NSNumber numberWithLong:cardDataObj.magnePrintLength] forKey: @"MagnePrint.Length"];
    [data setObject: cardDataObj.magneprintStatus forKey: @"MagnePrint.Status"];
    [data setObject: cardDataObj.encrypedSessionID forKey: @"SessionID"];
    [data setObject: cardDataObj.cardIIN forKey: @"Card.IIN"];
    [data setObject: cardDataObj.cardName forKey: @"Card.Name"];
    [data setObject: cardDataObj.cardLast4 forKey: @"Card.Last4"];
    [data setObject: cardDataObj.cardExpDate forKey: @"Card.ExpDate"];
    [data setObject: cardDataObj.cardExpDateMonth forKey: @"Card.ExpDateMonth"];
    [data setObject: cardDataObj.cardExpDateYear forKey: @"Card.ExpDateYear"];
    [data setObject: cardDataObj.cardServiceCode forKey: @"Card.SvcCode"];
    [data setObject: [NSNumber numberWithLong:cardDataObj.cardPANLength] forKey: @"Card.PANLength"];
    [data setObject: cardDataObj.deviceKSN forKey: @"KSN"];
    [data setObject: cardDataObj.deviceSerialNumber forKey: @"Device.SerialNumber"];
    [data setObject: cardDataObj.deviceSerialNumberMagTek forKey: @"MagTek SN"];
    [data setObject: cardDataObj.firmware forKey: @"Firmware Part Number"];
    [data setObject: cardDataObj.deviceName forKey: @"Device Model Name"];
    [data setObject: [(MTSCRA*)instance getTLVPayload] forKey: @"TLV Payload"];
    [data setObject: cardDataObj.deviceCaps forKey: @"DeviceCapMSR"];
    [data setObject: [(MTSCRA*)instance getOperationStatus] forKey: @"Operation.Status"];
    [data setObject: cardDataObj.cardStatus forKey: @"Card.Status"];
    [data setObject: [(MTSCRA*)instance getResponseData] forKey: @"Raw Data"];
    
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:data];
    [pluginResult setKeepCallbackAsBool:TRUE];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:cardSwipeCallback];
    
}

- (void) cardSwipeDidStart:(id)instance {
    NSLog(@"cardSwipeDidStart");
}

- (void) cardSwipeDidGetTransError {
    NSLog(@"cardSwipeDidGetTransError");
}

- (void) onDeviceConnectionDidChange:(MTSCRADeviceType)deviceType connected:(BOOL) connected instance:(id)instance {
    NSLog(@"onDeviceConnectionDidChange connected: %d started: %d", connected, [self isStarted]);
    
    if (connected && [self isStarted])
    {
        NSLog(@"Reader Connected");
        [self setVolumeToMax];
        if (connectedCallback) {
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [pluginResult setKeepCallbackAsBool: TRUE];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:connectedCallback];
        }
    }
    else
    {
        NSLog(@"Reader Disconnected");
        [self restoreVolume];
        if (disconnectedCallback) {
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [pluginResult setKeepCallbackAsBool: TRUE];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:disconnectedCallback];
        }
        if ([self isStarted]) {
            // reader hardware was removed, but plugin is still started
            // open again so we can get connected status callbacks
            NSLog(@"Re-opening device");
            [self.magtek openDevice];
        }
    }
    
}

// Ignore the volume deprecations since MPVolumeView needs a UI
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
- (void) setVolumeToMax {
    if (_magtek.getDeviceType != MAGTEKAUDIOREADER) { return; }
    
    MPMusicPlayerController *musicPlayer = [MPMusicPlayerController applicationMusicPlayer];
    NSLog(@"Setting Volume to Max");
    musicPlayer.volume = 1.0f;
}

- (void) restoreVolume {
    if (_magtek.getDeviceType != MAGTEKAUDIOREADER) { return; }

    NSLog(@"Restoring Volume");
    MPMusicPlayerController *musicPlayer = [MPMusicPlayerController applicationMusicPlayer];
    // Having trouble reliably getting the volume, setting back to a low value
    musicPlayer.volume = 0.2f;
}
#pragma clang diagnostic pop

- (void) onDeviceResponse:(NSData*)data {
    NSLog(@"onDeviceResponse %@", data);
}

- (void) onDeviceError:(NSError*)error {
    NSLog(@"onDeviceError %@", error);
    [self sendError:[error localizedDescription]];
}



@end
