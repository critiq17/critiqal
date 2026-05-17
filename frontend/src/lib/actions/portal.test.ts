import { describe, it, expect, afterEach } from 'vitest';
import { portal } from './portal';

afterEach(() => {
  document.body.innerHTML = '';
});

describe('portal', () => {
  it('moves the node to document.body by default', () => {
    const host = document.createElement('div');
    const node = document.createElement('span');
    host.appendChild(node);
    document.body.appendChild(host);

    portal(node);

    expect(node.parentElement).toBe(document.body);
    expect(host.contains(node)).toBe(false);
  });

  it('moves the node to a selector target', () => {
    const target = document.createElement('section');
    target.id = 'overlay-root';
    document.body.appendChild(target);
    const node = document.createElement('div');
    document.body.appendChild(node);

    portal(node, '#overlay-root');

    expect(node.parentElement).toBe(target);
  });

  it('moves the node to an element target', () => {
    const target = document.createElement('aside');
    document.body.appendChild(target);
    const node = document.createElement('div');
    document.body.appendChild(node);

    portal(node, target);

    expect(node.parentElement).toBe(target);
  });

  it('falls back to body when the selector matches nothing', () => {
    const node = document.createElement('div');
    document.body.appendChild(node);

    portal(node, '#does-not-exist');

    expect(node.parentElement).toBe(document.body);
  });

  it('removes the node from the DOM on destroy', () => {
    const node = document.createElement('div');
    document.body.appendChild(node);

    const { destroy } = portal(node);
    expect(node.isConnected).toBe(true);

    destroy();
    expect(node.isConnected).toBe(false);
  });
});
