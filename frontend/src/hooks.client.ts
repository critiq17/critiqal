import type { HandleClientError } from '@sveltejs/kit';

export const handleError: HandleClientError = ({ error }) => {
  const err = error as Error;
  return {
    message: err?.message ?? 'An unexpected error occurred',
    code: 'UNKNOWN',
  };
};
