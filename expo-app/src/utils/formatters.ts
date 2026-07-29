import { ImageSourcePropType } from 'react-native';
import { assets } from '../assets';
import type { AppNotification, NearbyUser, UserDto } from '../types';

export function relativeTime(value?: string | null): string {
  if (!value) return 'vừa xong';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'vừa xong';
  const minutes = Math.max(1, Math.floor((Date.now() - date.getTime()) / 60000));
  if (minutes < 60) return `${minutes} phút`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ`;
  const days = Math.floor(hours / 24);
  return `${days} ngày`;
}

export function bookingTimeLabel(value?: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }).replace(':', 'h');
}

export function notificationTimestamp(item: AppNotification): number {
  const raw = item.createdDate ? new Date(item.createdDate).getTime() : 0;
  return Number.isFinite(raw) ? raw : 0;
}

export function fullName(user: Pick<UserDto, 'firstName' | 'lastName'> | NearbyUser): string {
  return `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim() || 'Chưa cập nhật';
}

export function genderText(gender?: string | null): string {
  if (gender === 'MALE') return 'Nam';
  if (gender === 'FEMALE') return 'Nữ';
  return 'Khác';
}

export function calculateAge(dateOfBirth?: string | null): string {
  if (!dateOfBirth) return 'Chưa cập nhật';
  const birth = new Date(dateOfBirth);
  if (Number.isNaN(birth.getTime())) return 'Chưa cập nhật';
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const hasHadBirthday =
    today.getMonth() > birth.getMonth() ||
    (today.getMonth() === birth.getMonth() && today.getDate() >= birth.getDate());
  if (!hasHadBirthday) age -= 1;
  return String(age);
}

export function distanceKm(
  from: { latitude: number; longitude: number },
  to: { latitude: number; longitude: number },
): number {
  const earthRadius = 6371;
  const dLat = ((to.latitude - from.latitude) * Math.PI) / 180;
  const dLng = ((to.longitude - from.longitude) * Math.PI) / 180;
  const lat1 = (from.latitude * Math.PI) / 180;
  const lat2 = (to.latitude * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function remoteImageSource(uri?: string | null): ImageSourcePropType {
  if (uri?.startsWith('http://') || uri?.startsWith('https://')) {
    return { uri };
  }
  return assets.avatar;
}

export function formatTime(value?: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

