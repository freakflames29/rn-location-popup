import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  isLocationEnabled(): Promise<boolean>;
  promptForEnableLocation(): Promise<string>;
}

export default TurboModuleRegistry.get<Spec>('RnLocationPopup');