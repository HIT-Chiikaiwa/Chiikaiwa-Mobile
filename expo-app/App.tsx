import React, { useEffect, useRef, useState } from 'react';
import { StatusBar } from 'expo-status-bar';
import { View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { Toast } from './src/components/shared';
import { getStorage } from './src/services/storage';
import { AccountSettingsScreen, EditProfileScreen, ProfileScreen } from './src/screens/profile';
import { ChatScreen } from './src/screens/chat';
import { FriendsScreen } from './src/screens/friends';
import { HomeScreen } from './src/screens/home';
import { InputEmailScreen, LoginScreen, RegisterScreen, ResetPasswordScreen, VerifyEmailScreen, VerifyOtpScreen } from './src/screens/auth';
import { NotificationsScreen } from './src/screens/notifications';
import { styles } from './src/styles';
import type { ChatTarget, Route, Session, ToastState, UserDto, VerifyEmailParams } from './src/types';

export default function App() {
  const [route, setRoute] = useState<Route>('login');
  const [session, setSession] = useState<Session | null>(null);
  const [verifyParams, setVerifyParams] = useState<VerifyEmailParams>({ email: '', flow: 'register' });
  const [currentUser, setCurrentUser] = useState<UserDto | null>(null);
  const [profileTarget, setProfileTarget] = useState<UserDto | null>(null);
  const [toast, setToast] = useState<ToastState | undefined>();
  const [chatTarget, setChatTarget] = useState<ChatTarget | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const chatBackRoute = useRef<Route>('home');

  useEffect(() => {
    const storage = getStorage();
    const raw = storage?.getItem('chiikaiwa_pref');
    if (raw) {
      try {
        const saved = JSON.parse(raw) as Session;
        if (saved.accessToken && saved.userId) {
          setSession(saved);
          setRoute('home');
        }
      } catch {
        storage?.removeItem('chiikaiwa_pref');
      }
    }
  }, []);

  const showToast = (message: string, type: ToastState['type'] = 'info') => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    setToast({ id: Date.now(), message, type });
    toastTimer.current = setTimeout(() => setToast(undefined), 3000);
  };

  const saveSession = (nextSession: Session) => {
    setSession(nextSession);
    getStorage()?.setItem('chiikaiwa_pref', JSON.stringify(nextSession));
  };

  const logout = () => {
    setSession(null);
    setCurrentUser(null);
    setProfileTarget(null);
    getStorage()?.removeItem('chiikaiwa_pref');
    setRoute('login');
  };

  const openChat = (target: ChatTarget) => {
    chatBackRoute.current = route === 'chat' ? 'home' : route;
    setChatTarget(target);
    setRoute('chat');
  };

  const openOwnProfile = () => {
    setProfileTarget(null);
    setRoute('profile');
  };

  const openUserProfile = (user: UserDto) => {
    setProfileTarget(user);
    setRoute('profile');
  };

  return (
    <SafeAreaProvider>
      <View style={styles.appRoot}>
        <StatusBar style="dark" />
        {route === 'login' ? (
          <LoginScreen
            onLogin={(nextSession) => {
              saveSession(nextSession);
              showToast('Đăng nhập thành công', 'success');
              setRoute('home');
            }}
            onRegister={() => setRoute('register')}
            onForgot={() => setRoute('inputEmail')}
            showToast={showToast}
          />
        ) : null}
        {route === 'register' ? (
          <RegisterScreen
            onBack={() => setRoute('login')}
            showToast={showToast}
            onRegistered={(email) => {
              setVerifyParams({ email, flow: 'register' });
              setRoute('verifyEmail');
            }}
          />
        ) : null}
        {route === 'inputEmail' ? (
          <InputEmailScreen
            onBack={() => setRoute('login')}
            showToast={showToast}
            onContinue={(email) => {
              setVerifyParams({ email, flow: 'forgot_password' });
              setRoute('verifyEmail');
            }}
          />
        ) : null}
        {route === 'verifyEmail' ? (
          <VerifyEmailScreen
            params={verifyParams}
            onBack={() => setRoute(verifyParams.flow === 'forgot_password' ? 'inputEmail' : 'register')}
            onSent={() => setRoute('verifyOtp')}
            showToast={showToast}
          />
        ) : null}
        {route === 'verifyOtp' ? (
          <VerifyOtpScreen
            params={verifyParams}
            onBack={() => setRoute('verifyEmail')}
            onVerified={() => setRoute(verifyParams.flow === 'forgot_password' ? 'resetPassword' : 'login')}
            showToast={showToast}
          />
        ) : null}
        {route === 'resetPassword' ? (
          <ResetPasswordScreen
            email={verifyParams.email}
            onBack={() => setRoute('verifyOtp')}
            onDone={() => setRoute('login')}
            showToast={showToast}
          />
        ) : null}
        {route === 'home' ? (
          <HomeScreen
            session={session}
            currentUser={currentUser}
            setCurrentUser={setCurrentUser}
            onProfile={openOwnProfile}
            onFriends={() => setRoute('friends')}
            onNotifications={() => setRoute('notifications')}
            onOpenProfile={openUserProfile}
            onOpenChat={openChat}
            showToast={showToast}
          />
        ) : null}
        {route === 'notifications' ? (
          <NotificationsScreen
            session={session}
            onBack={() => setRoute('home')}
            onOpenChat={openChat}
            onFriends={() => setRoute('friends')}
            showToast={showToast}
          />
        ) : null}
        {route === 'profile' ? (
          <ProfileScreen
            session={session}
            currentUser={currentUser}
            profileTarget={profileTarget}
            setCurrentUser={setCurrentUser}
            onBack={() => setRoute('home')}
            onEdit={() => setRoute('editProfile')}
            onSettings={() => setRoute('accountSettings')}
            onOpenChat={openChat}
            onLogout={logout}
            showToast={showToast}
          />
        ) : null}
        {route === 'editProfile' ? (
          <EditProfileScreen
            session={session}
            currentUser={currentUser}
            onBack={() => setRoute('profile')}
            onUserChange={setCurrentUser}
            showToast={showToast}
            onSave={(user) => {
              setCurrentUser(user);
              showToast('Lưu thay đổi thành công', 'success');
              setRoute('profile');
            }}
          />
        ) : null}
        {route === 'accountSettings' ? (
          <AccountSettingsScreen
            session={session}
            currentUser={currentUser}
            setCurrentUser={setCurrentUser}
            onBack={() => setRoute('profile')}
            onLogout={logout}
            showToast={showToast}
          />
        ) : null}
        {route === 'friends' ? (
          <FriendsScreen
            session={session}
            onBack={() => setRoute('home')}
            onOpenChat={openChat}
            onOpenProfile={openUserProfile}
            showToast={showToast}
          />
        ) : null}
        {route === 'chat' && chatTarget ? (
          <ChatScreen
            session={session}
            currentUser={currentUser}
            target={chatTarget}
            onBack={() => setRoute(chatBackRoute.current)}
            showToast={showToast}
          />
        ) : null}
        <Toast toast={toast} />
      </View>
    </SafeAreaProvider>
  );
}
