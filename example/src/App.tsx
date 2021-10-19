import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import PhotoPicker from 'react-native-photo-picker';

export default function App() {
  const [result] = React.useState<number | undefined>();

  React.useEffect(() => {
    PhotoPicker.openPicker({ maxNum: 9 });
  }, []);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
