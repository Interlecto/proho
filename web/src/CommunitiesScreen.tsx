import { useEffect, useState, type FormEvent } from "react";
import CommunityScreen from "./CommunityScreen";
import InvitationBanner from "./InvitationBanner";
import { ApiError, createCommunity, listCommunities, type Account, type Community } from "./api";

interface CommunitiesScreenProps {
  token: string;
  account: Account;
  onAccountChanged: (account: Account) => void;
  onLogout: () => void;
}

export default function CommunitiesScreen({
  token,
  account,
  onAccountChanged,
  onLogout,
}: CommunitiesScreenProps) {
  const [communities, setCommunities] = useState<Community[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  async function refresh() {
    try {
      setCommunities(await listCommunities(token));
    } catch {
      setError("No se pudieron cargar las comunidades.");
    }
  }

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  if (selectedId) {
    return (
      <CommunityScreen
        key={selectedId}
        token={token}
        communityId={selectedId}
        communities={communities ?? []}
        account={account}
        onAccountChanged={onAccountChanged}
        onSwitch={setSelectedId}
        onBack={() => {
          setSelectedId(null);
          refresh();
        }}
      />
    );
  }

  return (
    <div className="content">
      <div className="top-row">
        <span className="chip">{account.displayName}</span>
        <button type="button" className="link" onClick={onLogout}>
          Cerrar sesión
        </button>
      </div>
      <InvitationBanner token={token} onJoined={refresh} />
      <h1>Sus comunidades</h1>
      {error && <p className="form-error">{error}</p>}
      {communities === null && <p>Cargando…</p>}
      {communities !== null && communities.length === 0 && !showCreate && (
        <p>Todavía no pertenece a ninguna comunidad.</p>
      )}
      {communities !== null && communities.length > 0 && (
        <ul className="community-list">
          {communities.map((community) => (
            <li key={community.id} className="community-item">
              <button type="button" className="link strong" onClick={() => setSelectedId(community.id)}>
                {community.name}
              </button>
              <span className="community-key">{community.key}</span>
            </li>
          ))}
        </ul>
      )}
      {!showCreate && (
        <button type="button" onClick={() => setShowCreate(true)}>
          Crear comunidad
        </button>
      )}
      {showCreate && (
        <CreateCommunityForm
          token={token}
          onCreated={() => {
            setShowCreate(false);
            refresh();
          }}
          onCancel={() => setShowCreate(false)}
        />
      )}
    </div>
  );
}

function CreateCommunityForm({
  token,
  onCreated,
  onCancel,
}: {
  token: string;
  onCreated: () => void;
  onCancel: () => void;
}) {
  const [key, setKey] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await createCommunity(token, { key, name, kind: "ph_residential" });
      onCreated();
    } catch (err) {
      if (err instanceof ApiError && err.code === "community_key_already_registered") {
        setError("Ya existe una comunidad con esa clave.");
      } else {
        setError("No se pudo crear la comunidad.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label className="field">
        Nombre
        <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={200} />
      </label>
      <label className="field">
        Clave (única, minúsculas y guiones)
        <input
          value={key}
          onChange={(e) => setKey(e.target.value.toLowerCase())}
          required
          pattern="[a-z0-9\-]+"
          maxLength={64}
        />
      </label>
      {error && <p className="form-error">{error}</p>}
      <div className="form-actions">
        <button type="submit" disabled={busy}>
          Crear
        </button>
        <button type="button" className="link" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  );
}
