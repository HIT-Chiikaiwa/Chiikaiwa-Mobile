export function getStorage() {
  try {
    const maybeStorage = (globalThis as { localStorage?: Storage }).localStorage;
    return maybeStorage ?? null;
  } catch {
    return null;
  }
}

