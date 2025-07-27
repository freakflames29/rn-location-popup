import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  Button,
  StyleSheet,
  Platform,
  AppState,
} from 'react-native';
import { isLocationEnabled, promptForEnableLocation } from 'rn-location-popup';

const App = () => {
  const [locationStatus, setLocationStatus] = useState('Checking...');

  const checkLocationStatus = async () => {
    if (Platform.OS === 'android') {
      try {
        const isEnabled = await isLocationEnabled();
        setLocationStatus(isEnabled ? 'On' : 'Off');
      } catch (e) {
        console.error(e);
        setLocationStatus('Error');
      }
    }
  };

  const enableLocation = async () => {
    if (Platform.OS === 'android') {
      try {
        const result = await promptForEnableLocation();
        console.log('Prompt result:', result);
        // The status will be re-checked by the AppState listener
      } catch (e) {
        console.error(e);
      }
    }
  };

  useEffect(() => {
    checkLocationStatus();

    const subscription = AppState.addEventListener('change', (nextAppState) => {
      if (nextAppState === 'active') {
        checkLocationStatus();
      }
    });

    return () => {
      subscription.remove();
    };
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Location Status</Text>
      <Text style={styles.status}>{locationStatus}</Text>
      {locationStatus === 'Off' && (
        <Button title="Enable Location" onPress={enableLocation} />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  status: {
    fontSize: 18,
    marginBottom: 20,
  },
});

export default App;
