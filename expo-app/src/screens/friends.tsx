import React, { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Modal, Pressable, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AppIcon, AvatarFrame, Header } from '../components/shared';
import { apiRequest, unwrapPage } from '../services/api';
import { styles } from '../styles';
import { colors } from '../theme/colors';
import type { BaseResponse, ChatTarget, ConversationResponse, FriendshipResponse, PageResponse, Session, ToastState, UserDto } from '../types';
import { formatTime, fullName, remoteImageSource } from '../utils/formatters';

function FriendsScreen({
  session,
  onBack,
  onOpenChat,
  onOpenProfile,
  showToast,
}: {
  session: Session | null;
  onBack: () => void;
  onOpenChat: (target: ChatTarget) => void;
  onOpenProfile: (user: UserDto) => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [query, setQuery] = useState('');
  const [friends, setFriends] = useState<FriendshipResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [optionsFor, setOptionsFor] = useState<FriendshipResponse | null>(null);

  const load = async (keyword = '') => {
    if (!session) {
      setFriends([]);
      return;
    }

    const normalizedKeyword = keyword.trim();
    setLoading(true);
    const endpoint = normalizedKeyword
      ? `api/v1/friends/search?keyword=${encodeURIComponent(normalizedKeyword)}&page=0&size=20`
      : 'api/v1/friends?page=0&size=20';
    try {
      const response = await apiRequest<BaseResponse<PageResponse<FriendshipResponse>>>(endpoint, {}, session.accessToken);
      const items = unwrapPage(response.data);
      setFriends(items);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể tải bạn bè', 'error');
      setFriends([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      void load(query);
    }, query.trim() ? 350 : 0);

    return () => clearTimeout(timer);
  }, [query, session?.accessToken]);

  const openDirectChat = async (friend: FriendshipResponse) => {
    if (!session) {
      showToast('Vui lòng đăng nhập để nhắn tin', 'error');
      return;
    }

    try {
      const response = await apiRequest<BaseResponse<ConversationResponse>>(
        'api/v1/chat/conversations/direct',
        {
          method: 'POST',
          body: JSON.stringify({ targetUserId: friend.userId }),
        },
        session.accessToken,
      );
      onOpenChat({
        id: response.data.id,
        receiverId: friend.userId,
        title: response.data.groupName || fullName(friend),
        avatar: response.data.groupAvatar || friend.avatar,
        conversation: response.data,
      });
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể mở cuộc trò chuyện', 'error');
    }
  };

  return (
    <SafeAreaView style={styles.friendsRoot}>
      <Header title="Bạn bè" onBack={onBack} />
      <View style={styles.friendSearch}>
        <View style={styles.searchIcon}>
          <AppIcon name="search-outline" size={18} />
        </View>
        <TextInput
          value={query}
          onChangeText={setQuery}
          onSubmitEditing={() => load(query)}
          placeholder="Tìm kiếm bạn bè"
          placeholderTextColor={colors.hint}
          style={styles.friendSearchInput}
        />
      </View>
      {loading ? <ActivityIndicator color={colors.brown} /> : null}
      <FlatList
        data={friends}
        keyExtractor={(item) => item.requestId || item.userId}
        contentContainerStyle={styles.friendList}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyStateText}>{query.trim() ? 'Không tìm thấy bạn bè' : 'Chưa có bạn bè'}</Text>
          </View>
        }
        renderItem={({ item }) => (
          <FriendItem
            friend={item}
            onPress={() => void openDirectChat(item)}
            onLongPress={() => setOptionsFor(item)}
          />
        )}
      />
      <Modal visible={Boolean(optionsFor)} transparent animationType="fade" onRequestClose={() => setOptionsFor(null)}>
        <Pressable style={styles.modalBackdrop} onPress={() => setOptionsFor(null)}>
          <Pressable style={styles.friendOptions}>
            {optionsFor ? (
              <>
                <FriendItem friend={optionsFor} compact onPress={() => undefined} />
                {['Nhắn tin', 'Xem hồ sơ', 'Chặn', 'Xóa'].map((option) => (
                  <Pressable
                    key={option}
                    style={styles.optionRow}
                    onPress={() => {
                      if (option === 'Nhắn tin') {
                        void openDirectChat(optionsFor);
                      } else if (option === 'Xem hồ sơ') {
                        onOpenProfile({
                          id: optionsFor.userId,
                          firstName: optionsFor.firstName,
                          lastName: optionsFor.lastName,
                          avatar: optionsFor.avatar,
                          phone: optionsFor.phone,
                        });
                      } else {
                        showToast(`${option} đang được phát triển`);
                      }
                      setOptionsFor(null);
                    }}
                  >
                    <Text style={styles.optionText}>{option}</Text>
                  </Pressable>
                ))}
              </>
            ) : null}
          </Pressable>
        </Pressable>
      </Modal>
    </SafeAreaView>
  );
}

function friendTitle(friend: FriendshipResponse): string {
  return fullName({
    firstName: friend.firstName,
    lastName: friend.lastName,
  });
}

function FriendItem({
  friend,
  onPress,
  onLongPress,
  compact,
}: {
  friend: FriendshipResponse;
  onPress: () => void;
  onLongPress?: () => void;
  compact?: boolean;
}) {
  return (
    <Pressable style={[styles.friendItem, compact && styles.friendItemCompact]} onPress={onPress} onLongPress={onLongPress}>
      <AvatarFrame source={remoteImageSource(friend.avatar)} size={50} online={friend.status === 'ONLINE'} />
      <View style={styles.friendBody}>
        <View style={styles.friendTopLine}>
          <Text style={styles.friendName} numberOfLines={1}>
            {friendTitle(friend)}
          </Text>
          <Text style={styles.friendTime}>{formatTime(friend.createdDate)}</Text>
        </View>
        <Text style={styles.friendLastMessage} numberOfLines={1}>
          {friend.phone || 'Bạn bè đã kết nối'}
        </Text>
      </View>
    </Pressable>
  );
}

export { FriendsScreen };
