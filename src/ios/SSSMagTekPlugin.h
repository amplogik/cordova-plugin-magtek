#import <Cordova/CDVPlugin.h>

#import "MTSCRA.h"
#import <MediaPlayer/MediaPlayer.h>

@interface SSSMagTekPlugin : CDVPlugin <MTSCRAEventDelegate>

- (void)checkPermissions:(CDVInvokedUrlCommand*)command;

- (void)start:(CDVInvokedUrlCommand*)command;
- (void)stop:(CDVInvokedUrlCommand*)command;

- (void)registerCardSwipeCallback:(CDVInvokedUrlCommand*)command;
- (void)registerConnectedCallback:(CDVInvokedUrlCommand*)command;
- (void)registerDisconnectedCallback:(CDVInvokedUrlCommand*)command;
- (void)registerErrorCallback:(CDVInvokedUrlCommand*)command;

@end
