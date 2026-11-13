import { useEffect, useState } from "react";
import { acceptInvitation, ApiError, previewInvitation, type InvitationPreview } from "./api";

const STORAGE_KEY = "proho.invitation";

/**
 * Takes an invitation token from the page's address and keeps it until it's used.
 * It's in the fragment (#invitation=...) because browsers don't send that part to
 * any server, and it's removed from the address straight away.
 */
export function captureInvitationToken(): void {
  const match = window.location.hash.match(/^#invitation=(.+)$/);
  if (match) {
    sessionStorage.setItem(STORAGE_KEY, match[1]);
    window.history.replaceState(null, "", window.location.pathname + window.location.search);
  }
}

export function hasInvitationToken(): boolean {
  return sessionStorage.getItem(STORAGE_KEY) !== null;
}

export function invitationLink(token: string): string {
  return `${window.location.origin}${window.location.pathname}#invitation=${token}`;
}

function describe(err: unknown): string {
  const code = err instanceof ApiError ? err.code : "";
  switch (code) {
    case "invitation_invalid":
      return "Esta invitación ya no es válida. Pida una nueva a la administración.";
    case "invitation_email_mismatch":
      return "Esta invitación es para otro correo.";
    case "already_member":
      return "Ya pertenece a esta comunidad.";
    default:
      return "No se pudo procesar la invitación.";
  }
}

export default function InvitationBanner({ token, onJoined }: { token: string; onJoined: () => void }) {
  const [invitationToken, setInvitationToken] = useState(() => sessionStorage.getItem(STORAGE_KEY));
  const [preview, setPreview] = useState<InvitationPreview | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (invitationToken) {
      previewInvitation(token, invitationToken)
        .then(setPreview)
        .catch((err) => setError(describe(err)));
    }
  }, [token, invitationToken]);

  function dismiss() {
    sessionStorage.removeItem(STORAGE_KEY);
    setInvitationToken(null);
  }

  async function accept() {
    if (!invitationToken) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await acceptInvitation(token, invitationToken);
      dismiss();
      onJoined();
    } catch (err) {
      setError(describe(err));
    } finally {
      setBusy(false);
    }
  }

  if (!invitationToken) {
    return null;
  }

  return (
    <div className="notice">
      {preview && preview.matchesAccount && (
        <p>
          Lo han invitado a <strong>{preview.communityName}</strong>.
        </p>
      )}
      {preview && !preview.matchesAccount && (
        <p>
          Esta invitación a <strong>{preview.communityName}</strong> es para {preview.email}. Inicie sesión o cree
          una cuenta con ese correo.
        </p>
      )}
      {error && <p className="form-error">{error}</p>}
      <div className="form-actions">
        {preview?.matchesAccount && !error && (
          <button type="button" className="primary" disabled={busy} onClick={accept}>
            Unirme
          </button>
        )}
        <button type="button" className="link" onClick={dismiss}>
          Descartar
        </button>
      </div>
    </div>
  );
}
