import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR = (
  `The package 'rn-location-popup' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n'
);

// @ts-expect-error
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const RnLocationPopupModule = isTurboModuleEnabled
  ? require('./NativeRnLocationPopup').default
  : NativeModules.RnLocationPopup;

const RnLocationPopup = RnLocationPopupModule
  ? RnLocationPopupModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function isLocationEnabled(): Promise<boolean> {
  return RnLocationPopup.isLocationEnabled();
}

export function promptForEnableLocation(): Promise<string> {
  return RnLocationPopup.promptForEnableLocation();
}