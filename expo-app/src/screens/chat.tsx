import React, { useEffect, useRef, useState } from 'react';
import { FlatList, KeyboardAvoidingView, Platform, Pressable, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { assets } from '../assets';
import { AppIcon, AvatarFrame, Header } from '../components/shared';
import { SOCKET_URL } from '../constants/config';
import { apiRequest, unwrapPage } from '../services/api';
import { styles } from '../styles';
import { colors } from '../theme/colors';
import type { BaseResponse, ChatTarget, MessageResponse, PageResponse, Session, ToastState } from '../types';
import { remoteImageSource } from '../utils/formatters';

function ChatScreen({
  session,
  target,
  onBack,
  showToast,
}: {
  session: Session | null;
  target: ChatTarget;
  onBack: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [draft, setDraft] = useState('');
  const [socketConnected, setSocketConnected] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!session || !target.conversation?.id) return;
    apiRequest<BaseResponse<PageResponse<MessageResponse>>>(
      `api/v1/chat/conversations/${target.conversation.id}/messages?page=0&size=20`,
      {},
      session.accessToken,
    )
      .then((response) => {
        const items = unwrapPage(response.data);
        if (items.length) setMessages(items.reverse());
      })
      .catch((err) => showToast(err instanceof Error ? err.message : 'Không thể tải tin nhắn', 'error'));
  }, [session, target.conversation?.id, showToast]);

  useEffect(() => {
    if (!session?.accessToken || !session.userId) return;
    try {
      const ws = new WebSocket(SOCKET_URL);
      socketRef.current = ws;
      ws.onopen = () => {
        ws.send(`CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\nAuthorization:Bearer ${session.accessToken}\n\n\0`);
      };
      ws.onmessage = (event) => {
        const text = String(event.data);
        if (text.startsWith('CONNECTED')) {
          setSocketConnected(true);
          ws.send(`SUBSCRIBE\nid:sub-/user/${session.userId}/queue/messages\ndestination:/user/${session.userId}/queue/messages\n\n\0`);
          return;
        }
        if (text.startsWith('MESSAGE')) {
          const body = text.split('\n\n')[1]?.replace('\0', '');
          if (body) {
            try {
              const parsed = JSON.parse(body) as MessageResponse;
              setMessages((current) => [...current, parsed]);
            } catch {
              showToast('Không thể đọc tin nhắn từ WebSocket', 'error');
            }
          }
        }
      };
      ws.onclose = () => setSocketConnected(false);
      ws.onerror = () => setSocketConnected(false);
      return () => {
        ws.send('DISCONNECT\n\n\0');
        ws.close();
      };
    } catch {
      return undefined;
    }
  }, [session, target.receiverId, target.title]);

  const send = () => {
    const text = draft.trim();
    if (!text) return;
    if (socketRef.current && socketConnected && session?.userId && target.receiverId) {
      const body = JSON.stringify({ senderId: session.userId, receiverId: target.receiverId, text });
      socketRef.current.send(`SEND\ndestination:/app/chat.sendPrivate\ncontent-type:application/json\n\n${body}\0`);
      setDraft('');
    } else {
      showToast('WebSocket chưa kết nối, chưa thể gửi tin nhắn', 'error');
    }
  };

  return (
    <SafeAreaView style={styles.chatRoot}>
      <Header
        title={target.title}
        onBack={onBack}
        right={
          <Pressable onPress={() => showToast('Tùy chọn chat đang được phát triển')}>
            <AppIcon name="add-outline" size={28} />
          </Pressable>
        }
      />
      <FlatList
        data={messages}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.messageList}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyStateText}>Chưa có tin nhắn</Text>
          </View>
        }
        renderItem={({ item }) => (
          <MessageBubble message={item} mine={Boolean(session?.userId && item.senderId === session.userId)} />
        )}
      />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={styles.inputBar}>
          <AppIcon name="happy-outline" size={22} />
          <TextInput
            value={draft}
            onChangeText={setDraft}
            placeholder="Nhập tin nhắn"
            placeholderTextColor={colors.hint}
            multiline
            style={styles.chatInput}
            onSubmitEditing={send}
          />
          <AppIcon name="image-outline" size={21} />
          <AppIcon name="camera-outline" size={21} />
          <Pressable onPress={send}>
            <AppIcon name="send-outline" size={23} />
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function MessageBubble({ message, mine }: { message: MessageResponse; mine: boolean }) {
  return (
    <View style={[styles.messageRow, mine && styles.messageRowMine]}>
      {!mine ? <AvatarFrame source={remoteImageSource(message.senderAvatar)} size={40} /> : null}
      <View style={[styles.messageBubble, mine ? styles.messageMine : styles.messageIncoming]}>
        <Text style={[styles.messageText, mine && styles.messageTextMine]}>{message.isRecalled ? 'Tin nhắn đã được thu hồi' : message.content}</Text>
      </View>
      {mine ? <AvatarFrame source={assets.avatar} size={40} /> : null}
    </View>
  );
}

export { ChatScreen };
