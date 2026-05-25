import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getDeviceId } from './deviceId';

describe('getDeviceId', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('generates and persists a stable install id', () => {
    const first = getDeviceId();
    const second = getDeviceId();

    expect(first).toMatch(/^[a-f0-9]{64}$/);
    expect(second).toBe(first);
    expect(localStorage.getItem('critiqal_device_id')).toBe(first);
  });

  it('falls back to an ephemeral id when localStorage is unavailable', () => {
    const getItemSpy = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('storage blocked');
    });
    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');

    const deviceId = getDeviceId();

    expect(deviceId).toMatch(/^[a-f0-9]{64}$/);
    expect(getItemSpy).toHaveBeenCalledWith('critiqal_device_id');
    expect(setItemSpy).not.toHaveBeenCalled();
  });
});
