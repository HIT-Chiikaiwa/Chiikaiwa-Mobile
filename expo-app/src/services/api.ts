import { BASE_URL } from '../constants/config';
import type { PageResponse } from '../types';

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20000);
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData;
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...(options.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        ...headers,
        ...(options.headers as Record<string, string> | undefined),
      },
    });
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new Error('Server phản hồi quá lâu, vui lòng thử lại');
    }
    throw err;
  } finally {
    clearTimeout(timeout);
  }

  const text = await response.text();
  const json = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = json?.message ?? json?.error ?? response.statusText ?? 'Đã xảy ra lỗi';
    throw new Error(message);
  }

  return json as T;
}

export function unwrapPage<T>(data?: PageResponse<T> | null): T[] {
  return data?.content ?? data?.items ?? [];
}
