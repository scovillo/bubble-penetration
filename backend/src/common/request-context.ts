import { AsyncLocalStorage } from 'node:async_hooks';

export interface RequestContextStore {
  requestId: string;
}

const requestContextStorage = new AsyncLocalStorage<RequestContextStore>();

export function runWithRequestContext<T>(
  store: RequestContextStore,
  callback: () => T,
): T {
  return requestContextStorage.run(store, callback);
}

export function getCurrentRequestId(): string | undefined {
  return requestContextStorage.getStore()?.requestId;
}
