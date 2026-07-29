import React, { useState } from 'react';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import {
  ActivityIndicator,
  Image,
  ImageSourcePropType,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { assets } from '../assets';
import { styles } from '../styles';
import { colors } from '../theme/colors';
import type { ToastState } from '../types';

type IconFamily = 'ion' | 'material';
type IonIconName = React.ComponentProps<typeof Ionicons>['name'];
type MaterialIconName = React.ComponentProps<typeof MaterialCommunityIcons>['name'];
type AppIconName = IonIconName | MaterialIconName;

function AppIcon({
  name,
  family = 'ion',
  size = 20,
  color = colors.brown,
}: {
  name: AppIconName;
  family?: IconFamily;
  size?: number;
  color?: string;
}) {
  const Icon = family === 'material' ? MaterialCommunityIcons : Ionicons;
  return <Icon name={name as never} size={size} color={color} />;
}

type InputFieldProps = {
  label?: string;
  value: string;
  onChangeText: (text: string) => void;
  placeholder: string;
  secure?: boolean;
  keyboardType?: 'default' | 'email-address' | 'numeric';
  multiline?: boolean;
};

function InputField({
  label,
  value,
  onChangeText,
  placeholder,
  secure,
  keyboardType = 'default',
  multiline,
}: InputFieldProps) {
  const [visible, setVisible] = useState(false);
  return (
    <View style={styles.fieldBlock}>
      {label ? <Text style={styles.label}>{label}</Text> : null}
      <View style={[styles.inputWrap, multiline && styles.inputWrapTall]}>
        <TextInput
          style={[styles.input, secure && styles.inputWithIcon, multiline && styles.inputMultiline]}
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor={colors.hint}
          secureTextEntry={Boolean(secure && !visible)}
          keyboardType={keyboardType}
          autoCapitalize={keyboardType === 'email-address' ? 'none' : 'sentences'}
          multiline={multiline}
        />
        {secure ? (
          <Pressable style={styles.inputIconButton} onPress={() => setVisible((current) => !current)}>
            <AppIcon name={visible ? 'eye-outline' : 'eye-off-outline'} size={20} />
          </Pressable>
        ) : null}
      </View>
    </View>
  );
}

type PrimaryButtonProps = {
  title: string;
  onPress: () => void;
  variant?: 'solid' | 'outline';
  loading?: boolean;
  disabled?: boolean;
  wide?: boolean;
};

function PrimaryButton({ title, onPress, variant = 'solid', loading, disabled, wide }: PrimaryButtonProps) {
  return (
    <Pressable
      style={({ pressed }) => [
        styles.button,
        wide && styles.buttonWide,
        variant === 'outline' && styles.buttonOutline,
        (pressed || disabled || loading) && styles.buttonPressed,
      ]}
      disabled={disabled || loading}
      onPress={onPress}
    >
      {loading ? <ActivityIndicator color={colors.brown} /> : <Text style={styles.buttonText}>{title}</Text>}
    </Pressable>
  );
}

function IconButton({
  source,
  onPress,
  size = 40,
  label,
}: {
  source: ImageSourcePropType;
  onPress: () => void;
  size?: number;
  label?: string;
}) {
  return (
    <Pressable accessibilityLabel={label} onPress={onPress} style={[styles.iconButton, { width: size, height: size }]}>
      <Image source={source} style={styles.iconImage} />
    </Pressable>
  );
}

function NavBarIcon({
  name,
  family = 'ion',
  onPress,
  label,
  badge,
  scale = 1,
}: {
  name: AppIconName;
  family?: IconFamily;
  onPress: () => void;
  label: string;
  badge?: string;
  scale?: number;
}) {
  return (
    <Pressable accessibilityLabel={label} onPress={onPress} style={[styles.navBarIcon, { width: 38 * scale, height: 38 * scale }]}>
      <AppIcon name={name} family={family} size={22 * scale} />
      {badge ? (
        <View
          style={[
            styles.navBadge,
            {
              right: 2 * scale,
              top: 2 * scale,
              minWidth: 15 * scale,
              height: 15 * scale,
              borderRadius: 8 * scale,
              paddingHorizontal: 2 * scale,
            },
          ]}
        >
          <Text style={[styles.navBadgeText, { fontSize: 8 * scale, lineHeight: 10 * scale }]}>{badge}</Text>
        </View>
      ) : null}
    </Pressable>
  );
}

function Header({
  title,
  onBack,
  right,
}: {
  title: string;
  onBack?: () => void;
  right?: React.ReactNode;
}) {
  return (
    <View style={styles.headerBar}>
      {onBack ? (
        <Pressable style={styles.headerBack} onPress={onBack}>
          <Text style={styles.backGlyph}>‹</Text>
        </Pressable>
      ) : (
        <View style={styles.headerBack} />
      )}
      <Text style={styles.headerTitle}>{title}</Text>
      <View style={styles.headerRight}>{right}</View>
    </View>
  );
}

function AuthShell({
  image,
  imageHeight,
  onBack,
  children,
}: {
  image: ImageSourcePropType;
  imageHeight: number;
  onBack?: () => void;
  children: React.ReactNode;
}) {
  return (
    <SafeAreaView style={styles.authRoot}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <View style={[styles.authHeader, { height: imageHeight }]}>
          <Image source={image} style={styles.authHeaderImage} resizeMode="contain" />
          {onBack ? (
            <Pressable style={styles.authBack} onPress={onBack}>
              <Text style={styles.backGlyph}>‹</Text>
            </Pressable>
          ) : null}
        </View>
        <ScrollView style={styles.authForm} contentContainerStyle={styles.authFormContent} keyboardShouldPersistTaps="handled">
          {children}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function MinimalAuthScreen({
  title,
  description,
  image = assets.group1,
  buttonTitle,
  onBack,
  onSubmit,
  loading,
  children,
}: {
  title: string;
  description: string;
  image?: ImageSourcePropType;
  buttonTitle: string;
  onBack: () => void;
  onSubmit: () => void;
  loading?: boolean;
  children?: React.ReactNode;
}) {
  return (
    <SafeAreaView style={styles.minimalRoot}>
      <ScrollView contentContainerStyle={styles.minimalContent} keyboardShouldPersistTaps="handled">
        <Pressable style={styles.minimalBack} onPress={onBack}>
          <Text style={styles.backGlyph}>‹</Text>
        </Pressable>
        <Text style={styles.bigTitle}>{title}</Text>
        <Text style={styles.minimalDescription}>{description}</Text>
        {children}
        <Image source={image} style={styles.chickenImage} resizeMode="contain" />
        <PrimaryButton title={buttonTitle} onPress={onSubmit} loading={loading} />
      </ScrollView>
    </SafeAreaView>
  );
}

function Toast({ toast }: { toast?: ToastState }) {
  const insets = useSafeAreaInsets();
  if (!toast) return null;
  return (
    <View
      pointerEvents="none"
      style={[
        styles.toast,
        { top: Math.max(insets.top + 72, 92) },
        toast.type === 'error' && styles.toastError,
        toast.type === 'success' && styles.toastSuccess,
      ]}
    >
      <Text style={styles.toastText}>{toast.message}</Text>
    </View>
  );
}

function AvatarFrame({ source, size = 65, online }: { source: ImageSourcePropType; size?: number; online?: boolean }) {
  return (
    <View style={[styles.avatarFrame, { width: size, height: size }]}>
      <Image source={source} style={styles.avatarImage} />
      {online ? <View style={styles.onlineBadge} /> : null}
    </View>
  );
}

function InfoLine({ name, family = 'ion', text }: { name: AppIconName; family?: IconFamily; text: string }) {
  return (
    <View style={styles.infoLine}>
      <View style={styles.infoIcon}>
        <AppIcon name={name} family={family} size={15} />
      </View>
      <Text style={styles.infoText}>{text}</Text>
    </View>
  );
}

export { AppIcon, AvatarFrame, AuthShell, Header, IconButton, InfoLine, InputField, MinimalAuthScreen, NavBarIcon, PrimaryButton, Toast };
export type { AppIconName, IconFamily };
