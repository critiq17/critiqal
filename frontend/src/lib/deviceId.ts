const DEVICE_ID_KEY = 'critiqal_device_id';
const DEVICE_ID_BYTES = 32;

function generateDeviceId(): string {
  const bytes = new Uint8Array(DEVICE_ID_BYTES);
  const cryptoApi = globalThis.crypto;

  if (cryptoApi?.getRandomValues) {
    cryptoApi.getRandomValues(bytes);
  } else {
    for (let index = 0; index < bytes.length; index += 1) {
      bytes[index] = Math.floor(Math.random() * 256);
    }
  }

  return Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('');
}

function getStorage(): Storage | null {
  try {
    return globalThis.localStorage ?? null;
  } catch {
    return null;
  }
}

export function getDeviceId(): string {
  const storage = getStorage();
  if (!storage) {
    return generateDeviceId();
  }

  try {
    let deviceId = storage.getItem(DEVICE_ID_KEY);
    if (!deviceId) {
      deviceId = generateDeviceId();
      storage.setItem(DEVICE_ID_KEY, deviceId);
    }
    return deviceId;
  } catch {
    return generateDeviceId();
  }
}
