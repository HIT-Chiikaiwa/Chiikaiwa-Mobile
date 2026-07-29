import React, { useEffect, useState } from 'react';
import * as ImagePicker from 'expo-image-picker';
import { ActivityIndicator, Alert, Image, Modal, Pressable, ScrollView, Switch, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { assets } from '../assets';
import { AppIcon, AvatarFrame, Header, InputField, PrimaryButton } from '../components/shared';
import { apiRequest } from '../services/api';
import { styles } from '../styles';
import { colors } from '../theme/colors';
import type { BaseResponse, ChatTarget, ConversationResponse, Session, SubjectDto, ToastState, UserDto } from '../types';
import { calculateAge, fullName, genderText, remoteImageSource } from '../utils/formatters';

function ProfileScreen({
  session,
  currentUser,
  profileTarget,
  setCurrentUser,
  onBack,
  onEdit,
  onSettings,
  onOpenChat,
  onLogout,
  showToast,
}: {
  session: Session | null;
  currentUser: UserDto | null;
  profileTarget?: UserDto | null;
  setCurrentUser: (user: UserDto) => void;
  onBack: () => void;
  onEdit: () => void;
  onSettings: () => void;
  onOpenChat: (target: ChatTarget) => void;
  onLogout: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const isOwnProfile = !profileTarget || profileTarget.id === session?.userId;
  const [user, setUser] = useState<UserDto | null>(profileTarget ?? currentUser);
  const [loading, setLoading] = useState(false);
  const [subjectsOpen, setSubjectsOpen] = useState(false);

  const loadProfile = async () => {
    const targetUserId = isOwnProfile ? session?.userId : profileTarget?.id;

    if (!targetUserId) {
      setUser(null);
      return;
    }

    if (!isOwnProfile && profileTarget) {
      setUser(profileTarget);
    }

    setLoading(true);
    try {
      const response = await apiRequest<BaseResponse<UserDto>>(
        `api/v1/profile/${targetUserId}`,
        {},
        session?.accessToken,
      );
      setUser(response.data);
      if (isOwnProfile) setCurrentUser(response.data);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể tải hồ sơ', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadProfile();
  }, [session?.accessToken, session?.userId, profileTarget?.id]);

  const sendFriendRequest = async () => {
    if (!session || !user || isOwnProfile) {
      showToast('Không thể gửi lời mời kết bạn', 'error');
      return;
    }

    try {
      const response = await apiRequest<BaseResponse<{ message?: string }>>(
        `api/v1/friends/request/${user.id}`,
        { method: 'POST' },
        session.accessToken,
      );
      showToast(response.data?.message ?? response.message ?? 'Đã gửi lời mời kết bạn', 'success');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể gửi lời mời kết bạn', 'error');
    }
  };

  const startDirectChat = async () => {
    if (!session || !user || isOwnProfile) {
      showToast('Không thể mở cuộc trò chuyện', 'error');
      return;
    }

    try {
      const response = await apiRequest<BaseResponse<ConversationResponse>>(
        'api/v1/chat/conversations/direct',
        {
          method: 'POST',
          body: JSON.stringify({ targetUserId: user.id }),
        },
        session.accessToken,
      );
      onOpenChat({
        id: response.data.id,
        receiverId: user.id,
        title: response.data.groupName || fullName(user),
        avatar: response.data.groupAvatar || user.avatar,
        conversation: response.data,
      });
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể mở cuộc trò chuyện', 'error');
    }
  };

  return (
    <SafeAreaView style={styles.profileRoot}>
      <View style={styles.profileTopBar}>
        <Pressable style={styles.profileBackButton} onPress={onBack}>
          <Text style={styles.profileBackText}>‹</Text>
        </Pressable>
        <Text style={styles.profileHeaderTitle}>Hồ sơ cá nhân</Text>
        <View style={styles.profileBackButton} />
      </View>
      <ScrollView style={styles.profileScene} contentContainerStyle={styles.profileContent}>
        <ProfileLandscape />
        {loading ? <ActivityIndicator color={colors.brown} style={styles.profileLoader} /> : null}
        {user ? (
          <ProfileBoard
            user={user}
            isOwnProfile={isOwnProfile}
            onEdit={onEdit}
            onSettings={onSettings}
            onOpenSubjects={() => setSubjectsOpen(true)}
            onAddFriend={sendFriendRequest}
            onMessage={startDirectChat}
            showToast={showToast}
          />
        ) : (
          <View style={styles.emptyState}>
            <Text style={styles.emptyStateText}>Chưa có dữ liệu hồ sơ</Text>
          </View>
        )}
      </ScrollView>
      {user && isOwnProfile ? (
        <SubjectModal
          visible={subjectsOpen}
          user={user}
          session={session}
          onClose={() => setSubjectsOpen(false)}
          onUserChange={(next) => {
            setUser(next);
            setCurrentUser(next);
          }}
          showToast={showToast}
        />
      ) : null}
    </SafeAreaView>
  );
}

function ProfileLandscape() {
  return (
    <View pointerEvents="none" style={styles.profileLandscape}>
      <View style={styles.profileSky} />
      <View style={styles.profileLake} />
      <View style={styles.profileHillBack} />
      <View style={styles.profileHillFront} />
      <View style={styles.profilePath} />
    </View>
  );
}

function ProfileBoard({
  user,
  isOwnProfile,
  onEdit,
  onSettings,
  onOpenSubjects,
  onAddFriend,
  onMessage,
  showToast,
}: {
  user: UserDto;
  isOwnProfile: boolean;
  onEdit: () => void;
  onSettings: () => void;
  onOpenSubjects: () => void;
  onAddFriend: () => void;
  onMessage: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const subjects = user.subjects ?? [];
  const strengths = subjects.filter((item) => item.type === 'STRENGTH');
  const weaknesses = subjects.filter((item) => item.type !== 'STRENGTH');
  const badges = [
    user.buddyActive ? 'Buddy sẵn sàng' : null,
    strengths.length ? 'Có điểm mạnh' : null,
    weaknesses.length ? 'Đang ôn tập' : null,
  ].filter(Boolean) as string[];

  return (
    <>
      <ProfileHangingCard>
        <View style={styles.profileIdentityBox}>
          <Image source={remoteImageSource(user.avatar)} style={styles.profilePhoto} />
          <View style={styles.profileIdentityText}>
            <Text style={styles.profileName}>{fullName(user)}</Text>
            <Text style={styles.profileSmall}>ID: {user.id}</Text>
            <Text style={styles.profileSmall}>Bạn bè: {user.buddyActive ? 'Có' : 'Chưa bật Buddy'}</Text>
          </View>
        </View>
        <Text style={styles.introText}>{user.statusTag || 'Chưa có giới thiệu'}</Text>
        {isOwnProfile ? (
          <Pressable style={styles.profilePrimaryAction} onPress={onEdit}>
            <Text style={styles.profilePrimaryActionText}>Chỉnh sửa trang cá nhân</Text>
          </Pressable>
        ) : (
          <View style={styles.profileActionRow}>
            <Pressable style={styles.profileSecondaryAction} onPress={onAddFriend}>
              <Text style={styles.profileSecondaryActionText}>Kết bạn</Text>
            </Pressable>
            <Pressable style={styles.profilePrimaryActionSmall} onPress={onMessage}>
              <Text style={styles.profilePrimaryActionText}>Nhắn tin</Text>
            </Pressable>
          </View>
        )}
      </ProfileHangingCard>

      <ProfileHangingCard>
        <Text style={styles.cardTitle}>Thông tin cá nhân</Text>
        <View style={styles.profileInfoBox}>
          <ProfileInfoLine label="Tuổi" value={calculateAge(user.dateOfBirth)} />
          <ProfileInfoLine label="Giới tính" value={genderText(user.gender)} />
          <ProfileInfoLine label="Trường học/ Nơi làm việc" value={user.university || 'Chưa cập nhật'} />
          <ProfileInfoLine label="Ngành học" value={user.majorName || 'Chưa cập nhật'} />
          <ProfileInfoLine label="Quê quán" value={user.location || 'Chưa cập nhật'} />
        </View>
        <View style={styles.profileSubjectRow}>
          <SubjectPreview title="Điểm mạnh" subjects={strengths} fallback="Chưa cập nhật" />
          <SubjectPreview title="Điểm yếu" subjects={weaknesses} fallback="Chưa cập nhật" />
        </View>
      </ProfileHangingCard>

      <View style={styles.profileTiles}>
        <ProfileTile title="Yêu thích" value={String(subjects.length)} detail={subjects.length ? `${subjects.length}/5` : '--'} onPress={onOpenSubjects} />
        <ProfileTile title="Cuộc hẹn" value="--" detail="--" onPress={() => showToast('Tính năng Cuộc hẹn đang được phát triển')} />
        <ProfileTile title="Xếp hạng" value={user.trustScore != null ? String(user.trustScore) : '--'} detail="Trust" onPress={() => showToast('Tính năng Xếp hạng đang được phát triển')} />
      </View>

      <ProfileHangingCard compact>
        <Text style={styles.cardTitle}>Danh hiệu</Text>
        {badges.length ? (
          <View style={styles.profileBadgeRow}>
            {badges.map((badge) => (
              <ProfileBadge key={badge} title={badge} />
            ))}
          </View>
        ) : (
          <Text style={styles.emptyText}>Chưa có danh hiệu</Text>
        )}
      </ProfileHangingCard>

      {isOwnProfile ? (
        <Pressable style={styles.settingsCard} onPress={onSettings}>
          <View style={styles.settingsIcon}>
            <AppIcon name="settings-outline" size={22} />
          </View>
          <Text style={styles.settingsText}>Cài đặt tài khoản</Text>
        </Pressable>
      ) : null}
    </>
  );
}

function ProfileHangingCard({ children, compact }: { children: React.ReactNode; compact?: boolean }) {
  return (
    <View style={[styles.hangingWrap, compact && styles.hangingWrapCompact]}>
      <View pointerEvents="none" style={styles.woodRow}>
        <Image source={assets.wood} style={styles.woodStrip} resizeMode="stretch" />
        <Image source={assets.wood} style={styles.woodStrip} resizeMode="stretch" />
      </View>
      <View style={styles.hangingCard}>{children}</View>
    </View>
  );
}

function ProfileInfoLine({ label, value }: { label: string; value: string }) {
  return (
    <Text style={styles.profileInfoText}>
      <Text style={styles.profileInfoLabel}>{label}: </Text>
      {value}
    </Text>
  );
}

function SubjectPreview({ title, subjects, fallback }: { title: string; subjects: SubjectDto[]; fallback: string }) {
  const visibleSubjects = subjects.slice(0, 2);

  return (
    <View style={styles.profileSubjectCard}>
      <Text style={styles.profileSubjectTitle}>{title}</Text>
      {visibleSubjects.length ? (
        visibleSubjects.map((subject) => (
          <Text key={subject.id} style={styles.profileSubjectText} numberOfLines={1}>
            {subject.name}
          </Text>
        ))
      ) : (
        <Text style={styles.profileSubjectText}>{fallback}</Text>
      )}
    </View>
  );
}

function ProfileTile({
  title,
  value,
  detail,
  onPress,
}: {
  title: string;
  value: string;
  detail: string;
  onPress: () => void;
}) {
  return (
    <Pressable style={styles.profileTile} onPress={onPress}>
      <View pointerEvents="none" style={styles.smallWoodRow}>
        <Image source={assets.wood} style={styles.smallWood} />
        <Image source={assets.wood} style={styles.smallWood} />
      </View>
      <Text style={styles.profileTileText}>{title}</Text>
      <Text style={styles.profileTileValue}>{value}</Text>
      <Text style={styles.profileTileDetail}>{detail}</Text>
    </Pressable>
  );
}

function ProfileBadge({ title }: { title: string }) {
  return (
    <View style={styles.profileBadge}>
      <Image source={assets.group1} style={styles.profileBadgeImage} resizeMode="contain" />
      <Text style={styles.profileBadgeText} numberOfLines={2}>
        {title}
      </Text>
    </View>
  );
}

function EditProfileScreen({
  session,
  currentUser,
  onBack,
  onUserChange,
  onSave,
  showToast,
}: {
  session: Session | null;
  currentUser: UserDto | null;
  onBack: () => void;
  onUserChange: (user: UserDto) => void;
  onSave: (user: UserDto) => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [firstName, setFirstName] = useState(currentUser?.firstName ?? '');
  const [lastName, setLastName] = useState(currentUser?.lastName ?? '');
  const [gender, setGender] = useState(currentUser?.gender ?? '');
  const [dateOfBirth, setDateOfBirth] = useState(currentUser?.dateOfBirth ?? '');
  const [phone, setPhone] = useState(currentUser?.phone ?? '');
  const [email, setEmail] = useState(currentUser?.email ?? '');
  const [intro, setIntro] = useState(currentUser?.statusTag ?? '');
  const [university, setUniversity] = useState(currentUser?.university ?? '');
  const [major, setMajor] = useState(currentUser?.majorName ?? '');
  const [location, setLocation] = useState(currentUser?.location ?? '');
  const [saving, setSaving] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);

  const avatarSource = remoteImageSource(currentUser?.avatar);

  const save = async () => {
    if (!session || !currentUser) {
      showToast('Vui lòng đăng nhập để lưu hồ sơ', 'error');
      return;
    }

    const nextFirstName = firstName.trim();
    const nextLastName = lastName.trim();
    const nextGender = gender.trim();
    const nextDateOfBirth = dateOfBirth.trim();

    if (!nextLastName || !nextFirstName || !nextGender || !nextDateOfBirth) {
      showToast('Vui lòng nhập họ, tên, giới tính và ngày sinh', 'error');
      return;
    }

    setSaving(true);
    try {
      let savedUser = currentUser;
      const token = session.accessToken;
      const userId = session.userId;

      const personalInfo = await apiRequest<BaseResponse<UserDto>>(
        `api/v1/profile/${userId}/personal-info`,
        {
          method: 'PUT',
          body: JSON.stringify({
            firstName: nextFirstName,
            lastName: nextLastName,
            gender: nextGender,
            dateOfBirth: nextDateOfBirth,
            phone: phone.trim(),
            email: email.trim(),
          }),
        },
        token,
      );
      savedUser = personalInfo.data;

      const academicInfo = await apiRequest<BaseResponse<UserDto>>(
        `api/v1/profile/${userId}/academic-info`,
        {
          method: 'PUT',
          body: JSON.stringify({
            university: university.trim(),
            majorName: major.trim(),
          }),
        },
        token,
      );
      savedUser = academicInfo.data;

      const locationInfo = await apiRequest<BaseResponse<UserDto>>(
        `api/v1/profile/${userId}/location`,
        {
          method: 'PUT',
          body: JSON.stringify({ location: location.trim() }),
        },
        token,
      );
      savedUser = locationInfo.data;

      const statusInfo = await apiRequest<BaseResponse<UserDto>>(
        `api/v1/profile/${userId}/status-tag`,
        {
          method: 'PUT',
          body: JSON.stringify({ statusTag: intro.trim() }),
        },
        token,
      );
      savedUser = statusInfo.data;

      onSave(savedUser);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể lưu hồ sơ', 'error');
    } finally {
      setSaving(false);
    }
  };

  const uploadAvatar = async () => {
    if (!session || !currentUser) {
      showToast('Vui lòng đăng nhập để cập nhật avatar', 'error');
      return;
    }

    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      showToast('Bạn cần cấp quyền thư viện ảnh để chọn avatar', 'error');
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.82,
    });

    if (result.canceled || !result.assets[0]) return;

    const asset = result.assets[0];
    const extension = asset.uri.split('.').pop()?.split('?')[0] || 'jpg';
    const mimeType = asset.mimeType || `image/${extension === 'jpg' ? 'jpeg' : extension}`;
    const fileName = asset.fileName || `avatar.${extension}`;
    const formData = new FormData();

    formData.append('file', {
      uri: asset.uri,
      name: fileName,
      type: mimeType,
    } as unknown as Blob);

    setUploadingAvatar(true);
    try {
      const response = await apiRequest<BaseResponse<UserDto>>(
        `api/v1/profile/${session.userId}/avatar`,
        {
          method: 'POST',
          body: formData,
        },
        session.accessToken,
      );
      onUserChange(response.data);
      showToast('Cập nhật avatar thành công', 'success');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể cập nhật avatar', 'error');
    } finally {
      setUploadingAvatar(false);
    }
  };

  if (!currentUser) {
    return (
      <SafeAreaView style={styles.profileRoot}>
        <Header title="Hồ sơ cá nhân" onBack={onBack} />
        <View style={styles.emptyState}>
          <Text style={styles.emptyStateText}>Chưa có dữ liệu hồ sơ để chỉnh sửa</Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.profileRoot}>
      <ScrollView contentContainerStyle={styles.profileContent}>
        <Header
          title="Hồ sơ cá nhân"
          onBack={onBack}
          right={
            <View style={styles.headerEditIcon}>
              <AppIcon name="create-outline" size={24} />
            </View>
          }
        />
        <ProfileHangingCard>
          <View style={styles.profileIdentityBox}>
            <Pressable style={styles.editAvatarWrap} onPress={uploadAvatar} disabled={uploadingAvatar}>
              <AvatarFrame source={avatarSource} size={80} />
              <View style={styles.editAvatarIcon}>
                {uploadingAvatar ? (
                  <ActivityIndicator color={colors.white} size="small" />
                ) : (
                  <AppIcon name="camera-outline" size={18} color={colors.white} />
                )}
              </View>
            </Pressable>
            <View style={styles.profileIdentityText}>
              <InputField value={lastName} onChangeText={setLastName} placeholder="Họ" />
              <InputField value={firstName} onChangeText={setFirstName} placeholder="Tên" />
            </View>
          </View>
          <InputField value={intro} onChangeText={setIntro} placeholder="Dòng giới thiệu" />
          <PrimaryButton title="Lưu thay đổi" onPress={save} loading={saving} />
        </ProfileHangingCard>
        <ProfileHangingCard>
          <Text style={styles.cardTitle}>Thông tin cá nhân</Text>
          <InputField value={dateOfBirth} onChangeText={setDateOfBirth} placeholder="Ngày sinh yyyy-MM-dd" />
          <InputField value={gender} onChangeText={setGender} placeholder="Giới tính MALE/FEMALE/OTHER" />
          <InputField value={phone} onChangeText={setPhone} placeholder="Số điện thoại" />
          <InputField value={email} onChangeText={setEmail} placeholder="Email" keyboardType="email-address" />
          <InputField value={university} onChangeText={setUniversity} placeholder="Trường học" />
          <InputField value={major} onChangeText={setMajor} placeholder="Ngành học" />
          <InputField value={location} onChangeText={setLocation} placeholder="Quê quán" />
          <Text style={styles.addInfoText}>+ Thêm thông tin</Text>
        </ProfileHangingCard>
      </ScrollView>
    </SafeAreaView>
  );
}

function AccountSettingsScreen({
  session,
  currentUser,
  setCurrentUser,
  onBack,
  onLogout,
  showToast,
}: {
  session: Session | null;
  currentUser: UserDto | null;
  setCurrentUser: (user: UserDto) => void;
  onBack: () => void;
  onLogout: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [user, setUser] = useState<UserDto | null>(currentUser);

  const toggleBuddy = async () => {
    if (!user) {
      showToast('Chưa có dữ liệu hồ sơ', 'error');
      return;
    }
    const nextBuddy = !(user.buddyActive ?? false);
    if (!session) {
      showToast('Vui lòng đăng nhập để cập nhật Buddy', 'error');
      return;
    }
    try {
      const response = await apiRequest<BaseResponse<UserDto>>(
        `api/v1/profile/${session.userId}/status`,
        {
          method: 'PATCH',
          body: JSON.stringify({ buddyActive: nextBuddy }),
        },
        session.accessToken,
      );
      setUser(response.data);
      setCurrentUser(response.data);
      showToast('Cập nhật trạng thái Buddy thành công', 'success');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Không thể cập nhật Buddy', 'error');
    }
  };

  const deleteAccount = () => {
    Alert.alert('Xác nhận xóa tài khoản', 'Bạn có chắc chắn muốn xóa tài khoản này? Hành động này không thể hoàn tác.', [
      { text: 'Hủy', style: 'cancel' },
      {
        text: 'Xóa',
        style: 'destructive',
        onPress: async () => {
          if (session) {
            try {
              await apiRequest<BaseResponse<{ message?: string }>>(`api/v1/profile/${session.userId}`, { method: 'DELETE' }, session.accessToken);
            } catch (err) {
              showToast(err instanceof Error ? err.message : 'Xóa tài khoản thất bại', 'error');
              return;
            }
          }
          showToast('Xoá tài khoản thành công', 'success');
          onLogout();
        },
      },
    ]);
  };

  return (
    <SafeAreaView edges={['left', 'right', 'bottom']} style={styles.settingsRoot}>
      <View style={styles.settingsStatusBand} />
      <View style={styles.settingsHeader}>
        <Pressable style={styles.settingsBackButton} onPress={onBack}>
          <AppIcon name="arrow-back-outline" size={18} />
        </Pressable>
        <Text style={styles.settingsTitle}>Cài đặt tài khoản</Text>
      </View>
      <View style={styles.settingsMenu}>
        <SettingsRow
          title="Cho phép quét rada"
          onPress={toggleBuddy}
          right={
            <View pointerEvents="none">
              <Switch
                value={Boolean(user?.buddyActive)}
                trackColor={{ false: '#E4D8B9', true: colors.bg }}
                thumbColor={colors.white}
                ios_backgroundColor="#E4D8B9"
                style={styles.settingsSwitch}
              />
            </View>
          }
        />
        <SettingsRow title="Thông tin đăng kí" onPress={() => showToast(`${user?.email ?? 'Chưa có email'} ${user?.phone ?? ''}`.trim())} />
        <SettingsRow title="Đổi mật khẩu" onPress={() => setPasswordOpen(true)} />
        <SettingsRow title="Cài đặt thông báo" onPress={() => showToast('Cài đặt thông báo đang được phát triển')} />
        <SettingsRow title="Trợ giúp" onPress={() => showToast('Liên hệ Study Date để được hỗ trợ')} />
        <SettingsRow title="Đăng xuất" onPress={onLogout} />
      </View>
      <ChangePasswordModal
        visible={passwordOpen}
        session={session}
        onClose={() => setPasswordOpen(false)}
        showToast={showToast}
      />
    </SafeAreaView>
  );
}

function SettingsRow({
  title,
  onPress,
  danger,
  right,
}: {
  title: string;
  onPress: () => void;
  danger?: boolean;
  right?: React.ReactNode;
}) {
  return (
    <Pressable style={styles.settingsRow} onPress={onPress}>
      <Text style={[styles.settingsRowText, danger && styles.dangerText]}>{title}</Text>
      {right ? <View style={styles.settingsRowRight}>{right}</View> : null}
    </Pressable>
  );
}

function ChangePasswordModal({
  visible,
  session,
  onClose,
  showToast,
}: {
  visible: boolean;
  session: Session | null;
  onClose: () => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async () => {
    if (!oldPassword || !newPassword || !confirmPassword) {
      showToast('Vui lòng nhập đầy đủ thông tin', 'error');
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast('Mật khẩu mới không trùng khớp', 'error');
      return;
    }
    if (!session) {
      showToast('Vui lòng đăng nhập để đổi mật khẩu', 'error');
      return;
    }
    setLoading(true);
    try {
      await apiRequest<BaseResponse<{ message?: string }>>(
        `api/v1/profile/${session.userId}/password`,
        {
          method: 'PUT',
          body: JSON.stringify({ oldPassword, newPassword, confirmPassword }),
        },
        session.accessToken,
      );
      showToast('Đổi mật khẩu thành công', 'success');
      onClose();
    } catch (err) {
      showToast(err instanceof Error ? `Đổi mật khẩu thất bại: ${err.message}` : 'Đổi mật khẩu thất bại', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <Pressable style={styles.modalBackdrop} onPress={onClose}>
        <Pressable style={styles.modalCard}>
          <Text style={styles.modalTitle}>Đổi Mật Khẩu</Text>
          <InputField value={oldPassword} onChangeText={setOldPassword} placeholder="Mật khẩu cũ" secure />
          <InputField value={newPassword} onChangeText={setNewPassword} placeholder="Mật khẩu mới" secure />
          <InputField value={confirmPassword} onChangeText={setConfirmPassword} placeholder="Xác nhận mật khẩu mới" secure />
          <View style={styles.modalActions}>
            <PrimaryButton title="Hủy" variant="outline" onPress={onClose} />
            <PrimaryButton title="Xác nhận" onPress={submit} loading={loading} />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function SubjectModal({
  visible,
  user,
  session,
  onClose,
  onUserChange,
  showToast,
}: {
  visible: boolean;
  user: UserDto;
  session: Session | null;
  onClose: () => void;
  onUserChange: (user: UserDto) => void;
  showToast: (message: string, type?: ToastState['type']) => void;
}) {
  const [subjects, setSubjects] = useState<SubjectDto[]>(user.subjects ?? []);
  const [newSubject, setNewSubject] = useState('');
  const [type, setType] = useState<'STRENGTH' | 'NEED_REVIEW'>('STRENGTH');

  useEffect(() => setSubjects(user.subjects ?? []), [user.subjects]);

  const addSubject = async () => {
    if (!newSubject.trim()) {
      showToast('Tên môn học không được để trống', 'error');
      return;
    }
    if (!session) {
      showToast('Vui lòng đăng nhập để thêm môn học', 'error');
      return;
    }
    try {
      const response = await apiRequest<BaseResponse<SubjectDto>>(
        `api/v1/profile/${session.userId}/subjects`,
        {
          method: 'POST',
          body: JSON.stringify({ name: newSubject.trim(), type }),
        },
        session.accessToken,
      );
      const nextSubjects = [...subjects, response.data];
      setSubjects(nextSubjects);
      onUserChange({ ...user, subjects: nextSubjects });
      setNewSubject('');
      showToast('Thêm môn học thành công', 'success');
    } catch (err) {
      showToast(err instanceof Error ? `Lỗi: ${err.message}` : 'Thêm môn học thất bại', 'error');
    }
  };

  const deleteSubject = async (subject: SubjectDto) => {
    if (!session) {
      showToast('Vui lòng đăng nhập để xoá môn học', 'error');
      return;
    }

    try {
      await apiRequest<BaseResponse<{ message?: string }>>(
        `api/v1/profile/${session.userId}/subjects/${subject.id}`,
        { method: 'DELETE' },
        session.accessToken,
      );
    } catch (err) {
      showToast(err instanceof Error ? `Lỗi: ${err.message}` : 'Xoá môn học thất bại', 'error');
      return;
    }
    const nextSubjects = subjects.filter((item) => item.id !== subject.id);
    setSubjects(nextSubjects);
    onUserChange({ ...user, subjects: nextSubjects });
    showToast('Xoá môn học thành công', 'success');
  };

  const strengths = subjects.filter((item) => item.type === 'STRENGTH');
  const review = subjects.filter((item) => item.type !== 'STRENGTH');

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.modalBackdrop} onPress={onClose}>
        <Pressable style={styles.modalCard}>
          <Text style={styles.modalTitle}>Quản Lý Môn Học</Text>
          <SubjectGroup title="Môn học thế mạnh (STRENGTH)" subjects={strengths} onDelete={deleteSubject} />
          <SubjectGroup title="Môn học cần ôn tập (NEED_REVIEW)" subjects={review} onDelete={deleteSubject} />
          <InputField value={newSubject} onChangeText={setNewSubject} placeholder="Tên môn học" />
          <View style={styles.segmented}>
            <Pressable style={[styles.segment, type === 'STRENGTH' && styles.segmentActive]} onPress={() => setType('STRENGTH')}>
              <Text style={[styles.segmentText, type === 'STRENGTH' && styles.segmentTextActive]}>Thế mạnh</Text>
            </Pressable>
            <Pressable style={[styles.segment, type === 'NEED_REVIEW' && styles.segmentActive]} onPress={() => setType('NEED_REVIEW')}>
              <Text style={[styles.segmentText, type === 'NEED_REVIEW' && styles.segmentTextActive]}>Cần ôn tập</Text>
            </Pressable>
          </View>
          <View style={styles.modalActions}>
            <PrimaryButton title="Đóng" variant="outline" onPress={onClose} />
            <PrimaryButton title="Thêm môn học" onPress={addSubject} />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

function SubjectGroup({
  title,
  subjects,
  onDelete,
}: {
  title: string;
  subjects: SubjectDto[];
  onDelete: (subject: SubjectDto) => void;
}) {
  return (
    <View style={styles.subjectGroup}>
      <Text style={styles.subjectGroupTitle}>{title}</Text>
      {subjects.length ? (
        subjects.map((subject) => (
          <View key={subject.id} style={styles.subjectItem}>
            <Text style={styles.subjectName}>{subject.name}</Text>
            <Pressable onPress={() => onDelete(subject)}>
              <Text style={styles.deleteAction}>Xóa</Text>
            </Pressable>
          </View>
        ))
      ) : (
        <Text style={styles.emptyText}>Chưa có môn học</Text>
      )}
    </View>
  );
}

export { AccountSettingsScreen, EditProfileScreen, ProfileScreen };
