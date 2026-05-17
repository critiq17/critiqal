/**
 * Spring animation presets for Svelte spring stores.
 * Import and use with: spring(value, SPRING_PRESETS.pop)
 */
export interface SpringOptions {
  stiffness: number;
  damping: number;
  precision?: number;
}

export const SPRING_PRESETS = {
  /** For like pop animations — bouncy and quick */
  pop: { stiffness: 0.5, damping: 0.4 } satisfies SpringOptions,

  /** For smooth UI transitions — gentle overshoot */
  smooth: { stiffness: 0.25, damping: 0.7 } satisfies SpringOptions,

  /** For drag interactions — no overshoot */
  drag: { stiffness: 0.35, damping: 1 } satisfies SpringOptions,

  /** For modal/sheet entrance — quick settle */
  modal: { stiffness: 0.4, damping: 0.65 } satisfies SpringOptions,
} as const;
