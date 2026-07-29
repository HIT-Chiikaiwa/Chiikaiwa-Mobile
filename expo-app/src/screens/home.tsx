import React, { useEffect, useRef, useState } from 'react';
import * as Location from 'expo-location';
import MapView, { Circle, Marker, Region } from 'react-native-maps';
import { Animated, Image, Modal, Pressable, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { assets } from '../assets';
import { AppIcon, AvatarFrame, InfoLine, NavBarIcon, PrimaryButton } from '../components/shared';
import { DEFAULT_LOCATION, RADAR_RADIUS_KM } from '../constants/config';
import { useSpin } from '../hooks/useSpin';
import { apiRequest } from '../services/api';
import { styles } from '../styles';
import { colors } from '../theme/colors';
import type { BaseResponse, ChatTarget, ConversationResponse, MapCoordinate, NearbyUser, Session, ToastState, UserDto } from '../types';
import { distanceKm, fullName, remoteImageSource } from '../utils/formatters';

function HomeScreen({
  session,
  currentUser,
  setCurrentUser,
  onProfile,
  onFriends,
  onNotifications,
  onOpenProfile,
  onOpenChat,
  showToast,
}: {
  session: Session | null;
  currentUser: UserDto | null;
  setCurrentUser: (user: UserDto) => void;
  onProfile: () => void;
  onFriends: () => void;
  onNotifications: () => void;
  onOpenProfile: (user: UserDto) => void;
  onOpenChat: (target: ChatTarget) => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const { width: screenWidth } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const [nearby, setNearby] = useState<NearbyUser[]>([]);
  const [scanning, setScanning] = useState(false);
  const [selected, setSelected] = useState<NearbyUser | null>(null);
  const [origin, setOrigin] = useState<MapCoordinate | null>(null);
  const [region, setRegion] = useState<Region>({
    ...DEFAULT_LOCATION,
    latitudeDelta: 0.08,
    longitudeDelta: 0.08,
  });
  const hasCenteredOnUser = useRef(false);
  const rotate = useSpin(scanning);
  const navScale = Math.min(Math.max(screenWidth / 390, 0.92), 1.22);
  const pillWidth = Math.min(Math.max(screenWidth * 0.32, 124), 158);
  const pillHeight = 44 * navScale;
  const radarSize = 72 * navScale;
  const radarInner = radarSize - 11 * navScale;
  const radarLeaf = radarSize - 21 * navScale;
  const navGap = Math.max(13 * navScale, 14);
  const horizontalInset = Math.min(Math.max(screenWidth * 0.04, 16), 22);
  const searchTop = insets.top + 8;
  const navBottom = Math.max(insets.bottom + 12, 22);

  const syncOrigin = (nextOrigin: MapCoordinate, centerMap: boolean) => {
    setOrigin((current) => {
      if (current && distanceKm(current, nextOrigin) < 0.02) return current;
      return nextOrigin;
    });

    if (centerMap || !hasCenteredOnUser.current) {
      hasCenteredOnUser.current = true;
      setRegion((current) => ({
        ...current,
        ...nextOrigin,
        latitudeDelta: centerMap ? 0.08 : current.latitudeDelta,
        longitudeDelta: centerMap ? 0.08 : current.longitudeDelta,
      }));
    }
  };

  const readCurrentCoordinate = async (notifyOnFailure: boolean): Promise<MapCoordinate | null> => {
    try {
      const permission = await Location.requestForegroundPermissionsAsync();
      if (permission.status !== Location.PermissionStatus.GRANTED) {
        if (notifyOnFailure) showToast('Quyền vị trí bị từ chối. Đang dùng vị trí mặc định Hà Nội.', 'info');
        return null;
      }

      const position = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });
      return {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      };
    } catch {
      try {
        const cached = await Location.getLastKnownPositionAsync({
          maxAge: 120000,
          requiredAccuracy: 500,
        });
        if (cached) {
          return {
            latitude: cached.coords.latitude,
            longitude: cached.coords.longitude,
          };
        }
      } catch {
        // Keep the map usable even when iOS cannot provide a cached location.
      }

      if (notifyOnFailure) showToast('Đang dùng vị trí mặc định Hà Nội vì chưa lấy được GPS', 'info');
      return null;
    }
  };

  useEffect(() => {
    if (!session) return;
    apiRequest<BaseResponse<UserDto>>(`api/v1/profile/${session.userId}`, {}, session.accessToken)
      .then((response) => setCurrentUser(response.data))
      .catch(() => undefined);
  }, [session, setCurrentUser]);

  useEffect(() => {
    let cancelled = false;

    const locateOnOpen = async () => {
      const coordinate = await readCurrentCoordinate(false);
      if (!cancelled && coordinate) {
        syncOrigin(coordinate, true);
      }
    };

    void locateOnOpen();

    return () => {
      cancelled = true;
    };
  }, []);

  const scan = async () => {
    setScanning(true);
    const scanOrigin = (await readCurrentCoordinate(true)) ?? origin ?? DEFAULT_LOCATION;

    syncOrigin(scanOrigin, true);

    try {
      const response = await apiRequest<BaseResponse<NearbyUser[]>>(
        `api/v1/location/radar?lat=${scanOrigin.latitude}&lng=${scanOrigin.longitude}&radius=${RADAR_RADIUS_KM}`,
        {},
        session?.accessToken,
      );
      const filtered = (response.data ?? [])
        .filter((user) => Number.isFinite(user.latitude) && Number.isFinite(user.longitude))
        .map((user) => ({ ...user, distanceKm: user.distanceKm ?? distanceKm(scanOrigin, user) }))
        .filter((user) => (user.distanceKm ?? 999) <= RADAR_RADIUS_KM);
      setNearby(filtered);
      showToast(`Đã quét ${filtered.length} bạn học gần đây`, 'success');
    } catch (err) {
      setNearby([]);
      showToast(err instanceof Error ? err.message : 'Không thể quét radar', 'error');
    } finally {
      setScanning(false);
    }
  };

  const startDirectChat = async (user: NearbyUser) => {
    if (!session) {
      onOpenChat({ id: user.userId, receiverId: user.userId, title: fullName(user), avatar: user.avatar });
      return;
    }

    try {
      const response = await apiRequest<BaseResponse<ConversationResponse>>(
        'api/v1/chat/conversations/direct',
        {
          method: 'POST',
          body: JSON.stringify({ targetUserId: user.userId }),
        },
        session.accessToken,
      );
      onOpenChat({
        id: response.data.id,
        receiverId: user.userId,
        title: response.data.groupName || fullName(user),
        avatar: response.data.groupAvatar || user.avatar,
        conversation: response.data,
      });
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể mở cuộc trò chuyện', 'error');
      onOpenChat({ id: user.userId, receiverId: user.userId, title: fullName(user), avatar: user.avatar });
    }
  };

  return (
    <View style={styles.mapRoot}>
      <MapView
        style={styles.realMap}
        initialRegion={region}
        region={region}
        onRegionChangeComplete={setRegion}
        onUserLocationChange={(event) => {
          const coordinate = event.nativeEvent.coordinate;
          if (coordinate) {
            syncOrigin(
              {
                latitude: coordinate.latitude,
                longitude: coordinate.longitude,
              },
              false,
            );
          }
        }}
        mapPadding={{ top: searchTop + 62, bottom: navBottom + radarSize + 18, left: 0, right: 0 }}
        showsUserLocation={false}
        showsMyLocationButton={false}
        showsCompass={false}
        loadingEnabled
      >
        {origin ? (
          <>
            <Circle
              center={origin}
              radius={RADAR_RADIUS_KM * 1000}
              strokeColor="rgba(220,107,83,0.9)"
              fillColor="rgba(248,149,4,0.14)"
              strokeWidth={2}
            />
            <Marker coordinate={origin} title="Bạn đang ở đây">
              <View style={styles.myLocationMarker}>
                <Image source={remoteImageSource(currentUser?.avatar)} style={styles.myLocationAvatar} />
              </View>
            </Marker>
          </>
        ) : null}
        {nearby
          .filter((user) => Number.isFinite(user.latitude) && Number.isFinite(user.longitude))
          .map((user) => (
          <Marker
            key={user.userId}
            coordinate={{ latitude: user.latitude, longitude: user.longitude }}
            title={fullName(user)}
            description={user.statusTag || undefined}
            onPress={() => setSelected(user)}
          >
            <View style={styles.mapMarker}>
              <Image source={remoteImageSource(user.avatar)} style={styles.pinAvatar} />
            </View>
          </Marker>
        ))}
      </MapView>
      <View style={[styles.searchCard, { top: searchTop, left: horizontalInset, right: horizontalInset }]}>
        <View style={styles.searchIcon}>
          <AppIcon name="crown-outline" family="material" size={19} />
        </View>
        <View style={styles.searchIcon}>
          <AppIcon name="search-outline" size={18} />
        </View>
        <Text style={styles.searchText}>Bạn muốn tìm ai?</Text>
        <Pressable onPress={onProfile}>
          <Image source={remoteImageSource(currentUser?.avatar)} style={styles.topAvatar} />
        </Pressable>
      </View>
      <View
        style={[
          styles.bottomNav,
          {
            left: horizontalInset,
            right: horizontalInset,
            bottom: navBottom,
            height: radarSize + 10,
          },
        ]}
      >
        <View
          style={[
            styles.navPill,
            styles.navPillLeft,
            { width: pillWidth, height: pillHeight, marginRight: navGap, borderRadius: 6 * navScale },
          ]}
        >
          <NavBarIcon name="clipboard-outline" onPress={() => showToast('Tính năng Lịch trình đang được phát triển')} label="Lịch trình" scale={navScale} />
          <NavBarIcon name="person-add-outline" onPress={onFriends} label="Bạn bè" badge="9" scale={navScale} />
        </View>
        <Pressable style={[styles.radarButton, { width: radarSize, height: radarSize, bottom: 4 * navScale }]} onPress={scan} disabled={scanning}>
          <View style={[styles.radarHalo, { width: radarInner, height: radarInner, borderRadius: radarInner / 2, borderWidth: 3 * navScale }]}>
            <Animated.Image
              source={scanning ? assets.radaring : assets.radar}
              style={[styles.radarImage, { width: radarLeaf, height: radarLeaf, transform: [{ rotate }] }]}
            />
            <Image
              source={assets.groupRadar}
              style={[styles.radarOverlay, { width: radarLeaf, height: radarLeaf, left: (radarInner - radarLeaf) / 2, top: (radarInner - radarLeaf) / 2 }]}
            />
          </View>
        </Pressable>
        <View
          style={[
            styles.navPill,
            styles.navPillRight,
            { width: pillWidth, height: pillHeight, marginLeft: navGap, borderRadius: 6 * navScale },
          ]}
        >
          <NavBarIcon name="notifications-outline" onPress={onNotifications} label="Thông báo" badge="9" scale={navScale} />
          <NavBarIcon name="alarm-outline" onPress={() => showToast('Tính năng Lịch hẹn đang được phát triển')} label="Lịch hẹn" badge="1" scale={navScale} />
        </View>
      </View>
      <Modal visible={Boolean(selected)} transparent animationType="slide" onRequestClose={() => setSelected(null)}>
        <Pressable style={styles.modalBackdrop} onPress={() => setSelected(null)}>
          <Pressable style={styles.userDialog}>
            {selected ? (
              <>
                <View style={styles.userDialogTop}>
                  <AvatarFrame source={remoteImageSource(selected.avatar)} size={60} />
                  <View style={styles.flex}>
                    <View style={styles.dialogNameRow}>
                      <Text style={styles.dialogName}>{fullName(selected)}</Text>
                      <Text style={styles.dialogDistance}>Cách bạn {(selected.distanceKm ?? 0).toFixed(2)} km</Text>
                    </View>
                    <Text style={styles.statusTag}>{selected.statusTag || 'Đang rảnh học bài'}</Text>
                  </View>
                </View>
                <InfoLine name="school-outline" text={selected.university || 'Chưa cập nhật trường học'} />
                <InfoLine name="crown-outline" family="material" text={selected.majorName || 'Chưa cập nhật ngành học'} />
                <PrimaryButton
                  title="Xem hồ sơ"
                  variant="outline"
                  onPress={() => {
                    const target = selected;
                    setSelected(null);
                    onOpenProfile({
                      id: target.userId,
                      firstName: target.firstName,
                      lastName: target.lastName,
                      avatar: target.avatar,
                      university: target.university,
                      majorName: target.majorName,
                      statusTag: target.statusTag,
                    });
                  }}
                />
                <PrimaryButton
                  title="Nhắn tin"
                  onPress={() => {
                    const target = selected;
                    setSelected(null);
                    void startDirectChat(target);
                  }}
                />
              </>
            ) : null}
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

export { HomeScreen };
