export type Route =
  | 'login'
  | 'register'
  | 'verifyEmail'
  | 'verifyOtp'
  | 'inputEmail'
  | 'resetPassword'
  | 'home'
  | 'profile'
  | 'editProfile'
  | 'accountSettings'
  | 'friends'
  | 'notifications'
  | 'chat';

export type Flow = 'register' | 'forgot_password';

export type MapCoordinate = { latitude: number; longitude: number };

export type Session = {
  accessToken: string;
  refreshToken: string;
  userId: string;
};

export type BaseResponse<T> = {
  codeStatus: number;
  message: string;
  data: T;
  timestamp: string;
};

export type LoginResponse = {
  data: {
    accessToken: string;
    refreshToken: string;
    id: string;
  };
};

export type UserDto = {
  id: string;
  firstName?: string | null;
  lastName?: string | null;
  avatar?: string | null;
  university?: string | null;
  majorName?: string | null;
  gender?: string | null;
  dateOfBirth?: string | null;
  location?: string | null;
  trustScore?: number | null;
  buddyActive?: boolean | null;
  statusTag?: string | null;
  subjects?: SubjectDto[] | null;
  email?: string | null;
  phone?: string | null;
};

export type SubjectDto = {
  id: string;
  name: string;
  type: 'STRENGTH' | 'NEED_REVIEW' | string;
};

export type NearbyUser = {
  userId: string;
  firstName: string;
  lastName: string;
  avatar?: string | null;
  university?: string | null;
  majorName?: string | null;
  statusTag?: string | null;
  latitude: number;
  longitude: number;
  distanceKm?: number;
};

export type MessageResponse = {
  id: string;
  conversationId?: string | null;
  senderId?: string | null;
  senderName?: string | null;
  senderAvatar?: string | null;
  content?: string | null;
  messageType?: string | null;
  isRecalled?: boolean;
  createdDate?: string | null;
  isPinned?: boolean;
  attachments?: unknown[];
  replyToMessage?: unknown;
  forwardedFrom?: unknown;
  reactions?: unknown[];
};

export type ConversationResponse = {
  id: string;
  type?: string | null;
  groupName?: string | null;
  groupAvatar?: string | null;
  memberCount?: number;
  unreadCount?: number;
  hasLeft?: boolean;
  lastMessage?: MessageResponse | null;
};

export type BookingResponse = {
  id: string;
  status?: string | null;
  subject?: string | null;
  scheduledAt?: string | null;
  creatorId?: string | null;
  creatorName?: string | null;
  creatorAvatar?: string | null;
  partnerId?: string | null;
  partnerName?: string | null;
  partnerAvatar?: string | null;
  conversationId?: string | null;
  createdDate?: string | null;
  lastModifiedDate?: string | null;
};

export type FriendshipResponse = {
  requestId: string;
  userId: string;
  firstName?: string | null;
  lastName?: string | null;
  avatar?: string | null;
  phone?: string | null;
  status?: string | null;
  createdDate?: string | null;
};

export type AppNotification = {
  id: string;
  kind: 'booking' | 'friend';
  title: string;
  avatar?: string | null;
  createdDate?: string | null;
  booking?: BookingResponse;
  friendRequest?: FriendshipResponse;
};

export type PageResponse<T> = {
  content?: T[];
  items?: T[];
  totalElements?: number;
  totalPages?: number;
  number?: number;
};

export type ToastState = {
  id: number;
  message: string;
  type?: 'info' | 'error' | 'success';
};

export type VerifyEmailParams = {
  email: string;
  flow: Flow;
};

export type ChatTarget = {
  id: string;
  title: string;
  avatar?: string | null;
  receiverId?: string;
  conversation?: ConversationResponse;
};

