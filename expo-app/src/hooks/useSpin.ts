import { useEffect, useRef } from 'react';
import { Animated } from 'react-native';

export function useSpin(active: boolean) {
  const spin = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (!active) {
      spin.stopAnimation();
      spin.setValue(0);
      return;
    }
    const loop = Animated.loop(
      Animated.timing(spin, {
        toValue: 1,
        duration: 900,
        useNativeDriver: true,
      }),
    );
    loop.start();
    return () => loop.stop();
  }, [active, spin]);

  return spin.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });
}

