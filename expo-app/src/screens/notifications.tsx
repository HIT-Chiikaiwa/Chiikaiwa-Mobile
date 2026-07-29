import React, { useEffect, useState } from 'react';
import { ActivityIndicator, Image, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { apiRequest, unwrapPage } from '../services/api';
import { styles } from '../styles';
import { colors } from '../theme/colors';
import type { AppNotification, BaseResponse, BookingResponse, ChatTarget, FriendshipResponse, PageResponse, Session, ToastState } from '../types';
import { bookingTimeLabel, fullName, notificationTimestamp, relativeTime, remoteImageSource } from '../utils/formatters';

function NotificationsScreen({
  session,
  onBack,
  onOpenChat,
  onFriends,
  showToast,
}: {
  session: Session | null;
  onBack: () => void;
  onOpenChat: (target: ChatTarget) => void;
  onFriends: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(false);

  const mapBooking = (booking: BookingResponse): AppNotification => {
    const currentUserIsCreator = session?.userId && booking.creatorId === session.userId;
    const partnerName = currentUserIsCreator ? booking.partnerName : booking.creatorName;
    const partnerAvatar = currentUserIsCreator ? booking.partnerAvatar : booking.creatorAvatar;
    const timeLabel = bookingTimeLabel(booking.scheduledAt);

    return {
      id: `booking-${booking.id}`,
      kind: 'booking',
      title: `Bạn có một cuộc hẹn với ${partnerName || 'bạn học'}${timeLabel ? ` lúc ${timeLabel}` : ''}!`,
      avatar: partnerAvatar,
      createdDate: booking.createdDate || booking.lastModifiedDate || booking.scheduledAt,
      booking,
    };
  };

  const mapFriendRequest = (request: FriendshipResponse): AppNotification => ({
    id: `friend-${request.requestId}`,
    kind: 'friend',
    title: `${fullName({ firstName: request.firstName, lastName: request.lastName })} đã gửi cho bạn lời mời kết bạn!`,
    avatar: request.avatar,
    createdDate: request.createdDate,
    friendRequest: request,
  });

  const loadNotifications = async () => {
    if (!session) {
      setNotifications([]);
      return;
    }

    setLoading(true);
    try {
      const [bookingsResult, friendRequestsResult] = await Promise.allSettled([
        apiRequest<BaseResponse<BookingResponse[]>>('api/v1/bookings', {}, session.accessToken),
        apiRequest<BaseResponse<PageResponse<FriendshipResponse>>>(
          'api/v1/friends/requests/pending?page=0&size=20',
          {},
          session.accessToken,
        ),
      ]);

      const bookingNotifications =
        bookingsResult.status === 'fulfilled' ? (bookingsResult.value.data ?? []).map(mapBooking) : [];
      const friendNotifications =
        friendRequestsResult.status === 'fulfilled'
          ? unwrapPage(friendRequestsResult.value.data).map(mapFriendRequest)
          : [];

      const nextNotifications = [...bookingNotifications, ...friendNotifications]
        .sort((a, b) => notificationTimestamp(b) - notificationTimestamp(a))
        .slice(0, 30);

      setNotifications(nextNotifications);

      if (bookingsResult.status === 'rejected' && friendRequestsResult.status === 'rejected') {
        showToast('Không thể tải thông báo', 'error');
      }
    } catch (err) {
      setNotifications([]);
      showToast(err instanceof Error ? err.message : 'Không thể tải thông báo', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadNotifications();
  }, [session?.accessToken, session?.userId]);

  const recentNotifications = notifications.slice(0, 2);
  const previousNotifications = notifications.slice(2);

  const openNotification = (item: AppNotification) => {
    if (item.kind === 'booking') {
      const booking = item.booking;
      if (!booking?.conversationId) {
        showToast('Cuộc hẹn này chưa có phòng chat', 'info');
        return;
      }
      onOpenChat({
        id: booking.conversationId,
        title: item.title.replace(/^Bạn có một cuộc hẹn với /, '').replace(/ lúc .+!$/, ''),
        avatar: item.avatar,
        conversation: {
          id: booking.conversationId,
          groupName: item.title,
          groupAvatar: item.avatar,
        },
      });
      return;
    }

    onFriends();
  };

  return (
    <SafeAreaView style={styles.notificationRoot}>
      <View style={styles.notificationTopBand} />
      <View style={styles.notificationHeader}>
        <Pressable style={styles.notificationBack} onPress={onBack}>
          <Text style={styles.notificationBackText}>‹</Text>
        </Pressable>
        <Text style={styles.notificationTitle}>Thông báo</Text>
      </View>
      <ScrollView
        style={styles.notificationScroll}
        contentContainerStyle={styles.notificationContent}
        refreshControl={undefined}
      >
        {loading ? <ActivityIndicator color={colors.brown} style={styles.notificationLoader} /> : null}
        {notifications.length ? (
          <>
            <NotificationSection title="Mới đây" items={recentNotifications} onPress={openNotification} />
            <NotificationSection title="Trước đó" items={previousNotifications} onPress={openNotification} />
          </>
        ) : (
          <View style={styles.emptyState}>
            <Text style={styles.emptyStateText}>Chưa có thông báo</Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

function NotificationSection({
  title,
  items,
  onPress,
}: {
  title: string;
  items: AppNotification[];
  onPress: (item: AppNotification) => void;
}) {
  if (!items.length) return null;

  return (
    <View style={styles.notificationSection}>
      <Text style={styles.notificationSectionTitle}>{title}</Text>
      {items.map((item) => (
        <NotificationCard key={item.id} item={item} onPress={() => onPress(item)} />
      ))}
    </View>
  );
}

function NotificationCard({ item, onPress }: { item: AppNotification; onPress: () => void }) {
  return (
    <Pressable style={({ pressed }) => [styles.notificationCard, pressed && styles.notificationCardPressed]} onPress={onPress}>
      <View style={styles.notificationAvatarFrame}>
        <Image source={remoteImageSource(item.avatar)} style={styles.notificationAvatar} />
      </View>
      <View style={styles.notificationCardBody}>
        <Text style={styles.notificationMessage}>{item.title}</Text>
        <Text style={styles.notificationTime}>{relativeTime(item.createdDate)}</Text>
      </View>
    </Pressable>
  );
}

export { NotificationsScreen };

