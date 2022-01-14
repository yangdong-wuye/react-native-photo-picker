#import <React/RCTBridgeModule.h>
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif
#import <UIKit/UIKit.h>
#import "HXPhotoPicker.h"
#import "SVProgressHUD.h"
@interface PhotoPickerModule : NSObject <HXCustomNavigationControllerDelegate, RCTBridgeModule>

@end
