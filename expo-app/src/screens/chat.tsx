import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AppIcon, AvatarFrame } from '../components/shared';
import { SOCKET_ENDPOINTS } from '../constants/config';
import { apiRequest, unwrapPage } from '../services/api';
import { buildStompFrame, createStompTransport, parseStompFrame, type StompTransport } from '../services/stomp';
import { styles } from '../styles';
import { colors } from '../theme/colors';
import type {
  BaseResponse,
  ChatTarget,
  ConversationResponse,
  MessageResponse,
  PageResponse,
  Session,
  ToastState,
  UserDto,
} from '../types';
import { remoteImageSource } from '../utils/formatters';

type SocketState = 'connecting' | 'connected' | 'disconnected' | 'error';

function sameMessage(left: MessageResponse, right: MessageResponse) {
  return Boolean(
    left.senderId &&
      left.senderId === right.senderId &&
      (left.content ?? '').trim() === (right.content ?? '').trim() &&
      left.id.startsWith('local-'),
  );
}

function mergeMessage(messages: MessageResponse[], incoming: MessageResponse) {
  if (messages.some((message) => message.id === incoming.id)) return messages;
  return [...messages.filter((message) => !sameMessage(message, incoming)), incoming];
}

function sortMessages(messages: MessageResponse[]) {
  return [...messages].sort((left, right) => {
    const leftTime = left.createdDate ? new Date(left.createdDate).getTime() : 0;
    const rightTime = right.createdDate ? new Date(right.createdDate).getTime() : 0;
    return leftTime - rightTime;
  });
}

function conversationIdFromTarget(target: ChatTarget, conversation: ConversationResponse | null) {
  if (conversation?.id) return conversation.id;
  return target.receiverId ? undefined : target.id;
}

function ChatScreen({
  session,
  currentUser,
  target,
  onBack,
  showToast,
}: {
  session: Session | null;
  currentUser: UserDto | null;
  target: ChatTarget;
  onBack: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [conversation, setConversation] = useState<ConversationResponse | null>(target.conversation ?? null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [draft, setDraft] = useState('');
  const [socketState, setSocketState] = useState<SocketState>('connecting');
  const socketRef = useRef<WebSocket | null>(null);
  const transportRef = useRef<StompTransport | null>(null);
  const frameBufferRef = useRef('');
  const heartbeatRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const listRef = useRef<FlatList<MessageResponse>>(null);
  const conversationId = conversationIdFromTarget(target, conversation);
  const canSend = socketState === 'connected' && Boolean(session?.userId && target.receiverId);

  const connectionText = useMemo(() => {
    if (socketState === 'connecting') return 'Đang kết nối WebSocket...';
    if (socketState === 'error') return 'WebSocket đang lỗi, đang thử kết nối lại';
    if (socketState === 'disconnected') return 'WebSocket chưa kết nối, đang thử lại';
    return '';
  }, [socketState]);

  useEffect(() => {
    setConversation(target.conversation ?? null);
    setMessages([]);
    setDraft('');
  }, [target.id, target.conversation?.id]);

  useEffect(() => {
    let cancelled = false;
    if (!session?.accessToken || !target.receiverId || conversation?.id) return undefined;

    apiRequest<BaseResponse<ConversationResponse>>(
      'api/v1/chat/conversations/direct',
      {
        method: 'POST',
        body: JSON.stringify({ targetUserId: target.receiverId }),
      },
      session.accessToken,
    )
      .then((response) => {
        if (!cancelled) setConversation(response.data);
      })
      .catch((err) => showToast(err instanceof Error ? err.message : 'Không thể mở cuộc trò chuyện', 'error'));

    return () => {
      cancelled = true;
    };
  }, [conversation?.id, session?.accessToken, target.receiverId]);

  useEffect(() => {
    if (!session || !conversationId) return;
    apiRequest<BaseResponse<PageResponse<MessageResponse>>>(
      `api/v1/chat/conversations/${conversationId}/messages?page=0&size=40&sort=createdDate,desc`,
      {},
      session.accessToken,
    )
      .then((response) => {
        setMessages(sortMessages(unwrapPage(response.data)));
      })
      .catch((err) => showToast(err instanceof Error ? err.message : 'Không thể tải tin nhắn', 'error'));
  }, [conversationId, session?.accessToken]);

  useEffect(() => {
    if (!session?.accessToken || !session.userId) {
      setSocketState('disconnected');
      return undefined;
    }

    let closedByScreen = false;
    let endpointIndex = 0;
    let connectedToStomp = false;
    let connectTimeout: ReturnType<typeof setTimeout> | null = null;

    const clearHeartbeat = () => {
      if (heartbeatRef.current) {
        clearInterval(heartbeatRef.current);
        heartbeatRef.current = null;
      }
    };

    const clearConnectTimeout = () => {
      if (connectTimeout) {
        clearTimeout(connectTimeout);
        connectTimeout = null;
      }
    };

    const connect = () => {
      clearHeartbeat();
      clearConnectTimeout();
      frameBufferRef.current = '';
      connectedToStomp = false;
      setSocketState('connecting');

      const endpoint = SOCKET_ENDPOINTS[endpointIndex % SOCKET_ENDPOINTS.length];
      const transport = createStompTransport(endpoint, session.accessToken);
      const ws = transport.socket;
      socketRef.current = ws;
      transportRef.current = transport;
      if (__DEV__) console.log('[chat-socket]', { event: 'connecting', mode: transport.mode, url: transport.url });

      const connectStomp = () => {
        transport.send(
          buildStompFrame('CONNECT', {
            'accept-version': '1.1,1.2',
            'heart-beat': '10000,10000',
            Authorization: `Bearer ${session.accessToken}`,
          }),
        );
      };

      ws.onopen = () => {
        if (__DEV__) console.log('[chat-socket]', { event: 'transport-open', mode: transport.mode });
        if (transport.mode === 'raw') connectStomp();
        connectTimeout = setTimeout(() => {
          if (!connectedToStomp && !closedByScreen) {
            if (__DEV__) console.log('[chat-socket]', { event: 'connect-timeout', mode: transport.mode });
            ws.close();
          }
        }, 4500);
      };

      ws.onmessage = (event) => {
        const data = String(event.data);
        if (transport.isOpenFrame(data)) {
          if (__DEV__) console.log('[chat-socket]', { event: 'sockjs-open' });
          connectStomp();
          return;
        }

        const stompChunks = transport.unwrap(data);
        stompChunks.forEach((chunk) => {
          frameBufferRef.current += chunk;
          const rawFrames = frameBufferRef.current.split('\0');
          frameBufferRef.current = rawFrames.pop() ?? '';

          rawFrames.forEach((rawFrame) => {
            const frame = parseStompFrame(rawFrame);
            if (!frame) return;

            if (frame.command === 'CONNECTED') {
              connectedToStomp = true;
              clearConnectTimeout();
              if (__DEV__) console.log('[chat-socket]', { event: 'stomp-connected' });
              setSocketState('connected');
              transport.send(
                buildStompFrame('SUBSCRIBE', {
                  id: `sub-user-${session.userId}`,
                  destination: `/user/${session.userId}/queue/messages`,
                }),
              );
              heartbeatRef.current = setInterval(() => {
                if (ws.readyState === WebSocket.OPEN) transport.send('\n');
              }, 10000);
              return;
            }

            if (frame.command === 'MESSAGE' && frame.body) {
              try {
                const parsed = JSON.parse(frame.body) as MessageResponse;
                setMessages((current) => mergeMessage(current, parsed));
                requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
              } catch {
                showToast('Không thể đọc tin nhắn từ WebSocket', 'error');
              }
              return;
            }

            if (frame.command === 'ERROR') {
              if (__DEV__) console.log('[chat-socket]', { event: 'stomp-error', body: frame.body });
              setSocketState('error');
              showToast(frame.body || 'WebSocket trả lỗi khi gửi tin nhắn', 'error');
            }
          });
        });
      };

      ws.onerror = () => {
        if (__DEV__) console.log('[chat-socket]', { event: 'transport-error' });
        setSocketState('error');
      };

      ws.onclose = (event) => {
        if (__DEV__) console.log('[chat-socket]', { event: 'transport-closed', code: event.code, reason: event.reason });
        clearHeartbeat();
        clearConnectTimeout();
        socketRef.current = null;
        transportRef.current = null;
        if (closedByScreen) return;
        setSocketState('disconnected');
        if (!connectedToStomp) endpointIndex += 1;
        reconnectRef.current = setTimeout(connect, 1800);
      };
    };

    connect();

    return () => {
      closedByScreen = true;
      if (reconnectRef.current) clearTimeout(reconnectRef.current);
      clearHeartbeat();
      clearConnectTimeout();
      if (socketRef.current?.readyState === WebSocket.OPEN) {
        transportRef.current?.send(buildStompFrame('DISCONNECT'));
      }
      socketRef.current?.close();
      socketRef.current = null;
      transportRef.current = null;
    };
  }, [session?.accessToken, session?.userId]);

  const send = () => {
    const text = draft.trim();
    if (!text) return;
    if (!session?.userId || !target.receiverId) {
      showToast('Chưa xác định được người nhận tin nhắn', 'error');
      return;
    }
    if (!canSend || socketRef.current?.readyState !== WebSocket.OPEN) {
      showToast('WebSocket chưa kết nối, chưa thể gửi tin nhắn', 'error');
      return;
    }

    const optimistic: MessageResponse = {
      id: `local-${Date.now()}`,
      conversationId,
      senderId: session.userId,
      senderName: 'Bạn',
      senderAvatar: currentUser?.avatar,
      content: text,
      messageType: 'TEXT',
      createdDate: new Date().toISOString(),
    };
    const body = JSON.stringify({ senderId: session.userId, receiverId: target.receiverId, text });

    transportRef.current?.send(
      buildStompFrame(
        'SEND',
        {
          destination: '/app/chat.sendPrivate',
          'content-type': 'application/json',
        },
        body,
      ),
    );
    setDraft('');
    setMessages((current) => mergeMessage(current, optimistic));
    requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
  };

  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.chatRoot}>
      <View style={styles.chatHeader}>
        <Pressable style={styles.chatHeaderButton} onPress={onBack}>
          <AppIcon name="chevron-back-outline" size={30} />
        </Pressable>
        <Text numberOfLines={1} style={styles.chatTitle}>{target.title}</Text>
        <Pressable style={styles.chatHeaderButton} onPress={() => showToast('Tùy chọn chat đang được phát triển')}>
          <AppIcon name="add-outline" size={26} />
        </Pressable>
      </View>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.chatBody}>
        {connectionText ? (
          <View style={[styles.chatSocketBanner, socketState === 'connecting' && styles.chatSocketBannerInfo]}>
            <Text style={styles.chatSocketBannerText}>{connectionText}</Text>
          </View>
        ) : null}
        <FlatList
          ref={listRef}
          data={messages}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.messageList}
          keyboardShouldPersistTaps="handled"
          onContentSizeChange={() => listRef.current?.scrollToEnd({ animated: false })}
          ListEmptyComponent={
            <View style={styles.chatEmptyState}>
              <Text style={styles.chatEmptyText}>Chưa có tin nhắn</Text>
            </View>
          }
          renderItem={({ item }) => (
            <MessageBubble
              message={item}
              mine={Boolean(session?.userId && item.senderId === session.userId)}
              incomingAvatar={target.avatar}
              mineAvatar={currentUser?.avatar}
            />
          )}
        />
        <View style={styles.inputBar}>
          <Pressable style={styles.chatIconButton} onPress={() => showToast('Sticker đang được phát triển')}>
            <AppIcon name="chatbox-ellipses-outline" size={22} />
          </Pressable>
          <TextInput
            value={draft}
            onChangeText={setDraft}
            placeholder="Nhập tin nhắn"
            placeholderTextColor={colors.hint}
            multiline
            style={styles.chatInput}
            onSubmitEditing={send}
          />
          <Pressable style={styles.chatIconButton} onPress={() => showToast('Ghi âm đang được phát triển')}>
            <AppIcon name="mic-outline" size={21} />
          </Pressable>
          <Pressable style={styles.chatIconButton} onPress={() => showToast('Ảnh đang được phát triển')}>
            <AppIcon name="camera-outline" size={21} />
          </Pressable>
          <Pressable style={styles.chatIconButton} onPress={() => showToast('Tệp đang được phát triển')}>
            <AppIcon name="folder-outline" size={21} />
          </Pressable>
          <Pressable style={styles.chatSendButton} onPress={send}>
            <AppIcon name="send-outline" size={24} />
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function MessageBubble({
  message,
  mine,
  incomingAvatar,
  mineAvatar,
}: {
  message: MessageResponse;
  mine: boolean;
  incomingAvatar?: string | null;
  mineAvatar?: string | null;
}) {
  const text = message.isRecalled ? 'Tin nhắn đã được thu hồi' : message.content;
  return (
    <View style={[styles.messageRow, mine && styles.messageRowMine]}>
      {!mine ? <AvatarFrame source={remoteImageSource(message.senderAvatar ?? incomingAvatar)} size={54} /> : null}
      <View style={[styles.messageBubble, mine ? styles.messageMine : styles.messageIncoming]}>
        <Text style={[styles.messageText, mine && styles.messageTextMine]}>{text || ''}</Text>
      </View>
      {mine ? <AvatarFrame source={remoteImageSource(message.senderAvatar ?? mineAvatar)} size={44} /> : null}
    </View>
  );
}

export { ChatScreen };
