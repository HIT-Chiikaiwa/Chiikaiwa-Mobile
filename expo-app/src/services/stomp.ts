type StompHeaders = Record<string, string>;

export type StompFrame = {
  command: string;
  headers: StompHeaders;
  body: string;
};

export type StompMode = 'raw' | 'sockjs';

export type StompEndpoint = {
  url: string;
  mode: StompMode;
};

export type StompTransport = {
  mode: StompMode;
  url: string;
  socket: WebSocket;
  send: (frame: string) => void;
  unwrap: (data: string) => string[];
  isOpenFrame: (data: string) => boolean;
};

type ReactNativeWebSocketConstructor = new (
  url: string,
  protocols?: string | string[] | null,
  options?: { headers?: StompHeaders },
) => WebSocket;

function sockJsWebSocketUrl(url: string) {
  const cleanUrl = url.replace(/\/$/, '').replace(/\/websocket$/, '');
  const serverId = String(Math.floor(Math.random() * 1000)).padStart(3, '0');
  const sessionId = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`;
  return `${cleanUrl}/${serverId}/${sessionId}/websocket`;
}

export function createAuthorizedWebSocket(url: string, token: string): WebSocket {
  const SocketConstructor = WebSocket as unknown as ReactNativeWebSocketConstructor;
  return new SocketConstructor(url, null, {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

export function createStompTransport(endpoint: StompEndpoint, token: string): StompTransport {
  const transportUrl = endpoint.mode === 'sockjs' ? sockJsWebSocketUrl(endpoint.url) : endpoint.url;
  const socket = createAuthorizedWebSocket(transportUrl, token);
  const send = endpoint.mode === 'sockjs'
    ? (frame: string) => socket.send(JSON.stringify([frame]))
    : (frame: string) => socket.send(frame);
  const unwrap = endpoint.mode === 'sockjs'
    ? (data: string) => {
        if (data === 'o' || data === 'h') return [];
        if (data.startsWith('a')) {
          try {
            const frames = JSON.parse(data.slice(1));
            return Array.isArray(frames) ? frames.map(String) : [];
          } catch {
            return [];
          }
        }
        if (data.startsWith('c')) return [];
        return [data];
      }
    : (data: string) => [data];

  return {
    mode: endpoint.mode,
    url: transportUrl,
    socket,
    send,
    unwrap,
    isOpenFrame: (data: string) => endpoint.mode === 'sockjs' && data === 'o',
  };
}

export function buildStompFrame(command: string, headers: StompHeaders = {}, body = '') {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
  return `${command}\n${headerLines.join('\n')}\n\n${body}\0`;
}

export function parseStompFrame(rawFrame: string): StompFrame | null {
  const cleaned = rawFrame.replace(/^\n+/, '');
  if (!cleaned.trim()) return null;

  const [headerBlock, ...bodyParts] = cleaned.split('\n\n');
  const [command, ...headerLines] = headerBlock.split('\n');
  if (!command) return null;

  const headers = headerLines.reduce<StompHeaders>((current, line) => {
    const separator = line.indexOf(':');
    if (separator <= 0) return current;
    current[line.slice(0, separator)] = line.slice(separator + 1);
    return current;
  }, {});

  return {
    command,
    headers,
    body: bodyParts.join('\n\n').replace(/\0/g, ''),
  };
}
