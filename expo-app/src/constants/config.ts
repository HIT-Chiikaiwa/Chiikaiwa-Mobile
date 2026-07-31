export const BASE_URL = 'https://api.chiikaiwa.me/';
export const SOCKET_ENDPOINTS = [
  { url: 'wss://api.chiikaiwa.me/ws', mode: 'sockjs' },
] as const;
export const DEFAULT_LOCATION = { latitude: 21.028511, longitude: 105.804817 };
export const RADAR_RADIUS_KM = 5;
