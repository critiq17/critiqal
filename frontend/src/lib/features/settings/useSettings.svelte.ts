import { authStore } from '$lib/stores/auth.store.svelte';
import { stravaStore } from '$lib/stores/strava.store.svelte';
import { twoFactorService } from '$lib/services/two-factor.service';
import { emailVerificationService } from '$lib/services/email-verification.service';
import { recoveryService } from '$lib/services/recovery.service';
import { sessionService } from '$lib/services/session.service';
import { ApiError } from '$lib/types';
import type { TotpSetupResponse, TwoFactorStatusResponse, AuthSession } from '$lib/types';
import { t } from '$lib/i18n';

// Superset of both desktop and mobile TFA views. Desktop does not use
// 'regen-confirm' (it goes straight to handleRegenCodes from idle); mobile
// shows an intermediate confirmation step first.
export type TfaSetupStep = 'idle' | 'setup' | 'confirm' | 'regen-confirm' | 'disabled-confirm';

export class UseSettings {
  // ── Email ──────────────────────────────────────────────────────────────────
  emailInput = $state('');
  emailError = $state('');
  emailSuccess = $state(false);
  emailLoading = $state(false);

  // ── 2FA ────────────────────────────────────────────────────────────────────
  tfaEnabled = $state(false);
  tfaSetupData = $state<TotpSetupResponse | null>(null);
  tfaSetupStep = $state<TfaSetupStep>('idle');
  tfaCode = $state('');
  tfaDisableCode = $state('');
  tfaError = $state('');
  backupCodesCount = $state<number | null>(null);
  regenCodes = $state<string[]>([]);
  regenError = $state('');

  // Internal: full status response used by views to render status text.
  tfaStatus = $state<TwoFactorStatusResponse | null>(null);
  tfaLoading = $state(true);
  tfaSubmitting = $state(false);
  // Recovery codes surface (setup completion or regen completion)
  tfaRecoveryCodes = $state<string[]>([]);

  // ── Sessions ───────────────────────────────────────────────────────────────
  sessions = $state<AuthSession[]>([]);
  sessionsLoading = $state(true);
  sessionsError = $state('');
  revokingSessionId = $state<string | null>(null);

  // ── Account ────────────────────────────────────────────────────────────────
  isLoggingOut = $state(false);

  // ── Email ──────────────────────────────────────────────────────────────────

  async handleSetEmail(email: string): Promise<void> {
    this.emailLoading = true;
    this.emailError = '';
    this.emailSuccess = false;
    try {
      await emailVerificationService.setEmail({ email });
      await authStore.refresh();
      this.emailSuccess = true;
      this.emailInput = '';
    } catch (err: unknown) {
      this.emailError = this.mapError(err);
    } finally {
      this.emailLoading = false;
    }
  }

  async handleResendVerification(): Promise<void> {
    const pending = authStore.user?.pendingEmail;
    if (!pending) return;
    this.emailLoading = true;
    this.emailError = '';
    this.emailSuccess = false;
    try {
      await emailVerificationService.setEmail({ email: pending });
      this.emailSuccess = true;
    } catch (err: unknown) {
      this.emailError = this.mapError(err);
    } finally {
      this.emailLoading = false;
    }
  }

  // ── 2FA ────────────────────────────────────────────────────────────────────

  async loadTfaStatus(): Promise<void> {
    this.tfaLoading = true;
    try {
      this.tfaStatus = await twoFactorService.status();
      this.tfaEnabled = this.tfaStatus.enabled;
      if (this.tfaStatus.enabled) {
        const r = await recoveryService.getCodesCount();
        this.backupCodesCount = r.activeCount;
      }
    } catch {
      // non-fatal
    } finally {
      this.tfaLoading = false;
    }
  }

  async handleSetupTfa(): Promise<void> {
    this.tfaSubmitting = true;
    this.tfaError = '';
    try {
      this.tfaSetupData = await twoFactorService.setup();
      this.tfaCode = '';
      this.tfaSetupStep = 'setup';
    } catch (err: unknown) {
      this.tfaError = this.mapError(err);
    } finally {
      this.tfaSubmitting = false;
    }
  }

  async handleConfirmTfa(code: string): Promise<void> {
    this.tfaSubmitting = true;
    this.tfaError = '';
    try {
      await twoFactorService.confirm({ code });
      this.tfaRecoveryCodes = this.tfaSetupData?.recoveryCodes ?? [];
      this.tfaSetupStep = 'confirm';
      this.tfaStatus = { enabled: true };
      this.tfaEnabled = true;
    } catch (err: unknown) {
      this.tfaError = this.mapError(err);
    } finally {
      this.tfaSubmitting = false;
    }
  }

  handleDoneWithCodes(): void {
    this.tfaSetupData = null;
    this.tfaCode = '';
    this.tfaRecoveryCodes = [];
    this.tfaSetupStep = 'idle';
    this.loadTfaStatus();
  }

  async handleDisableTfa(): Promise<void> {
    this.tfaSubmitting = true;
    this.tfaError = '';
    try {
      await twoFactorService.disable({ code: this.tfaDisableCode });
      this.tfaStatus = { enabled: false };
      this.tfaEnabled = false;
      this.tfaDisableCode = '';
      this.backupCodesCount = null;
      this.tfaSetupStep = 'idle';
    } catch (err: unknown) {
      this.tfaError = this.mapError(err);
    } finally {
      this.tfaSubmitting = false;
    }
  }

  async handleRegenCodes(): Promise<void> {
    this.tfaSubmitting = true;
    this.tfaError = '';
    try {
      const res = await recoveryService.regenerateCodes();
      this.tfaRecoveryCodes = res.codes;
      this.regenCodes = res.codes;
      // Reuse the 'confirm' step to display the new codes (same codes panel
      // as after initial TFA setup). The regen-confirm step is the
      // pre-confirmation prompt set by the mobile UI before calling this.
      this.tfaSetupStep = 'confirm';
    } catch (err: unknown) {
      this.tfaError = this.mapError(err);
      this.regenError = this.tfaError;
    } finally {
      this.tfaSubmitting = false;
    }
  }

  // ── Sessions ───────────────────────────────────────────────────────────────

  async loadSessions(): Promise<void> {
    this.sessionsLoading = true;
    this.sessionsError = '';
    try {
      this.sessions = await sessionService.list();
    } catch (err: unknown) {
      this.sessionsError = this.mapError(err);
    } finally {
      this.sessionsLoading = false;
    }
  }

  async handleRevokeSession(sessionId: string): Promise<void> {
    this.revokingSessionId = sessionId;
    try {
      await sessionService.revoke(sessionId);
      this.sessions = this.sessions.filter((s) => s.id !== sessionId);
    } catch (err: unknown) {
      this.sessionsError = this.mapError(err);
    } finally {
      this.revokingSessionId = null;
    }
  }

  // ── Strava ─────────────────────────────────────────────────────────────────

  handleStravaConnect(): void {
    // Strava connect is coming soon — callers show their own feedback state.
    // stravaStore is a shared singleton; connect logic lives there.
  }

  async handleStravaDisconnect(): Promise<void> {
    await stravaStore.disconnect();
  }

  // ── Account ────────────────────────────────────────────────────────────────

  async handleLogout(navigate: (path: string) => void): Promise<void> {
    this.isLoggingOut = true;
    try {
      await authStore.logout();
      navigate('/');
    } finally {
      this.isLoggingOut = false;
    }
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private mapError(err: unknown): string {
    if (err instanceof ApiError) return err.message || t('common.somethingWentWrong');
    if (err instanceof Error) return err.message;
    return t('common.somethingWentWrong');
  }
}
