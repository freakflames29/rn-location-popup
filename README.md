# rn-location-popup

A React Native library to check and prompt for enabling location services on Android.

## Installation

```sh
npm install rn-location-popup
```

## Usage

```javascript
import { isLocationEnabled, promptForEnableLocation } from 'rn-location-popup';

// ...

const checkLocation = async () => {
  const enabled = await isLocationEnabled();
  if (!enabled) {
    const result = await promptForEnableLocation();
    console.log(result);
  }
};
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)