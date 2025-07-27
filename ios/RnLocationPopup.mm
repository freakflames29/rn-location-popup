#import "RnLocationPopup.h"
#import <CoreLocation/CoreLocation.h>

@implementation RnLocationPopup {
    CLLocationManager *_locationManager;
    RCTPromiseResolveBlock _resolve;
    RCTPromiseRejectBlock _reject;
}

RCT_EXPORT_MODULE()

- (void)isLocationEnabled:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject
{
    if ([CLLocationManager locationServicesEnabled]) {
        resolve(@(YES));
    } else {
        resolve(@(NO));
    }
}

- (void)promptForEnableLocation:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject
{
    _locationManager = [[CLLocationManager alloc] init];
    _locationManager.delegate = self;
    _resolve = resolve;
    _reject = reject;
    [_locationManager requestWhenInUseAuthorization];
}

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status
{
    if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
        _resolve(@"enabled");
    } else if (status == kCLAuthorizationStatusDenied) {
        _resolve(@"denied");
    } else if (status == kCLAuthorizationStatusNotDetermined) {
        // The user has not yet made a choice.
    } else if (status == kCLAuthorizationStatusRestricted) {
        _resolve(@"restricted");
    }
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeRnLocationPopupSpecJSI>(params);
}

@end