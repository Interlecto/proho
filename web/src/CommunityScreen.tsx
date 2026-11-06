import { useCallback, useEffect, useState, type FormEvent, type ReactNode } from "react";
import { invitationLink } from "./InvitationBanner";
import {
  archive,
  ApiError,
  assignRole,
  changeMemberStatus,
  createBuilding,
  createInvitation,
  createPerson,
  createRelationship,
  createUnit,
  endRelationship,
  endRole,
  getProfile,
  getCommunity,
  getMyMembership,
  leaveCommunity,
  linkMemberPerson,
  listAuditEvents,
  listBuildings,
  listInvitations,
  listMembers,
  listPeople,
  listPersonRelationships,
  listUnitRelationships,
  listUnits,
  restore,
  revokeInvitation,
  updateBuilding,
  updateCommunity,
  updateMe,
  updatePerson,
  updateProfile,
  updateUnit,
  type Account,
  type AuditEvent,
  type Building,
  type Community,
  type Invitation,
  type Member,
  type Person,
  type PersonInput,
  type PropertyUnit,
  type UnitInput,
  type UnitRelationship,
  type Visibility,
} from "./api";

const ERROR_MESSAGES: Record<string, string> = {
  building_in_use: "No se puede archivar: el edificio todavía tiene unidades activas.",
  unit_in_use: "No se puede archivar: la unidad tiene vínculos abiertos. Finalícelos primero.",
  person_in_use: "No se puede archivar: la persona tiene vínculos abiertos. Finalícelos primero.",
  record_archived: "El registro, o uno relacionado, está archivado. Restáurelo primero.",
  unit_number_already_registered: "Ya existe una unidad con ese número, posiblemente archivada.",
  person_id_already_registered: "Ya existe una persona con ese documento.",
  owner_share_exceeds_100: "La suma de las participaciones de los propietarios superaría el 100 %.",
  relationship_already_ended: "Ese vínculo ya terminó.",
  person_already_linked: "Esa persona ya está vinculada a otro miembro, o tiene una invitación abierta.",
  already_member: "Ese correo ya pertenece a un miembro de la comunidad.",
  invitation_pending: "Ese correo ya tiene una invitación abierta. Revóquela para enviar otra.",
  last_administrator: "La comunidad debe conservar al menos un administrador activo.",
  role_already_held: "Ese miembro ya tiene ese rol.",
  role_not_assigned: "Ese miembro ya no tiene ese rol.",
  role_not_held: "Su rol no le permite hacer esto.",
  membership_not_active: "Ya no tiene acceso a esta comunidad.",
  not_found_in_community: "El registro no existe en esta comunidad.",
  invalid_request: "Revise los datos: alguno no es válido.",
};

function errorMessage(err: unknown): string {
  const code = err instanceof ApiError ? err.code : "";
  return ERROR_MESSAGES[code] ?? "No se pudo completar la operación.";
}

const UNIT_KINDS: Record<string, string> = {
  apartment: "Apartamento",
  house: "Casa",
  commercial: "Local comercial",
  office: "Oficina",
  parking: "Parqueadero",
  storage: "Depósito",
};

const ROLES: Record<string, string> = {
  owner: "Propietario(a)",
  tenant: "Arrendatario(a)",
  resident: "Residente",
  household_member: "Miembro del hogar",
  representative: "Representante",
};

const ID_TYPES = ["CC", "CE", "NIT", "PA", "TI"];

const ASSIGNED_ROLES: Record<string, string> = {
  administrator: "Administrador(a)",
  accountant: "Contador(a)",
  treasurer: "Tesorero(a)",
  auditor: "Revisor(a) fiscal",
  council_member: "Miembro del consejo",
  moderator: "Moderador(a)",
  staff: "Personal",
};

function personName(person: Person): string {
  return person.kind === "legal"
    ? (person.legalName ?? "")
    : `${person.givenName ?? ""} ${person.surname ?? ""}`.trim();
}

function blankToNull(value: string): string | null {
  return value.trim() === "" ? null : value.trim();
}

type Tab = "units" | "people" | "buildings" | "details" | "members" | "audit" | "profile";

const TABS: Record<Tab, string> = {
  units: "Unidades",
  people: "Personas",
  buildings: "Edificios",
  details: "Datos",
  members: "Miembros",
  audit: "Historial",
  profile: "Mi perfil",
};

interface ScreenProps {
  token: string;
  communityId: string;
  isAdmin: boolean;
}

export default function CommunityScreen({
  token,
  communityId,
  account,
  onAccountChanged,
  onBack,
}: {
  token: string;
  communityId: string;
  account: Account;
  onAccountChanged: (account: Account) => void;
  onBack: () => void;
}) {
  const [community, setCommunity] = useState<Community | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);
  const [canReadAudit, setCanReadAudit] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>("units");

  useEffect(() => {
    Promise.all([getCommunity(token, communityId), getMyMembership(token, communityId)])
      .then(([loaded, membership]) => {
        setCommunity(loaded);
        setIsAdmin(membership.roles.includes("administrator"));
        setCanReadAudit(membership.roles.includes("administrator") || membership.roles.includes("auditor"));
      })
      .catch((err) => setError(errorMessage(err)));
  }, [token, communityId]);

  /** The caller may have just changed their own roles, so what they can do is read again. */
  const reloadSelf = useCallback(() => {
    getMyMembership(token, communityId)
      .then((membership) => {
        const admin = membership.roles.includes("administrator");
        setIsAdmin(admin);
        setCanReadAudit(admin || membership.roles.includes("auditor"));
        setTab((current) => (current === "members" && !admin ? "profile" : current));
      })
      .catch(onBack);
  }, [token, communityId, onBack]);

  return (
    <div className="content wide">
      <div className="top-row">
        <button type="button" className="link" onClick={onBack}>
          ← Sus comunidades
        </button>
      </div>
      {error && <p className="form-error">{error}</p>}
      {!community && !error && <p>Cargando…</p>}
      {community && (
        <>
          <h1>{community.name}</h1>
          <nav className="tabs" aria-label="Secciones de la comunidad">
            {(Object.keys(TABS) as Tab[])
              .filter((key) => (key !== "members" || isAdmin) && (key !== "audit" || canReadAudit))
              .map((key) => (
              <button
                key={key}
                type="button"
                className={key === tab ? "tab active" : "tab"}
                aria-current={key === tab ? "page" : undefined}
                onClick={() => setTab(key)}
              >
                {TABS[key]}
              </button>
            ))}
          </nav>
          {tab === "units" && <UnitsTab token={token} communityId={communityId} isAdmin={isAdmin} />}
          {tab === "people" && <PeopleTab token={token} communityId={communityId} isAdmin={isAdmin} />}
          {tab === "buildings" && (
            <BuildingsTab token={token} communityId={communityId} isAdmin={isAdmin} />
          )}
          {tab === "members" && (
            <MembersTab token={token} communityId={communityId} isAdmin={isAdmin} onSelfChanged={reloadSelf} />
          )}
          {tab === "audit" && <AuditTab token={token} communityId={communityId} />}
          {tab === "profile" && (
            <ProfileTab
              token={token}
              communityId={communityId}
              account={account}
              onAccountChanged={onAccountChanged}
              onLeft={onBack}
            />
          )}
          {tab === "details" && (
            <DetailsTab community={community} token={token} isAdmin={isAdmin} onSaved={setCommunity} />
          )}
        </>
      )}
    </div>
  );
}

/** Loads a list again whenever its inputs change, and exposes a refresh for after each change. */
function useList<T>(load: () => Promise<T[]>) {
  const [items, setItems] = useState<T[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const refresh = useCallback(async () => {
    try {
      setItems(await load());
      setError(null);
    } catch (err) {
      setError(errorMessage(err));
    }
  }, [load]);
  useEffect(() => {
    refresh();
  }, [refresh]);
  return { items, error, setError, refresh };
}

function ArchivedToggle({ value, onChange }: { value: boolean; onChange: (next: boolean) => void }) {
  return (
    <label className="toggle">
      <input type="checkbox" checked={value} onChange={(e) => onChange(e.target.checked)} />
      Ver archivados
    </label>
  );
}

function RowActions({ children }: { children: ReactNode }) {
  return <span className="row-actions">{children}</span>;
}

// Buildings

function BuildingsTab({ token, communityId, isAdmin }: ScreenProps) {
  const [showArchived, setShowArchived] = useState(false);
  const load = useCallback(
    () => listBuildings(token, communityId, showArchived),
    [token, communityId, showArchived],
  );
  const { items, error, setError, refresh } = useList(load);
  const [editing, setEditing] = useState<Building | "new" | null>(null);

  async function run(action: () => Promise<unknown>) {
    try {
      await action();
      await refresh();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <section>
      {isAdmin && <ArchivedToggle value={showArchived} onChange={setShowArchived} />}
      {error && <p className="form-error">{error}</p>}
      {items?.length === 0 && <p>No hay edificios. Son opcionales si la comunidad tiene uno solo.</p>}
      <ul className="record-list">
        {items?.map((building) => (
          <li key={building.id} className={building.archivedAt ? "record archived" : "record"}>
            <span>
              <strong>{building.name}</strong>
              {building.archivedAt && <span className="badge">Archivado</span>}
            </span>
            {isAdmin && (
              <RowActions>
                {building.archivedAt ? (
                  <button
                    type="button"
                    className="link"
                    onClick={() => run(() => restore(token, communityId, "buildings", building.id))}
                  >
                    Restaurar
                  </button>
                ) : (
                  <>
                    <button type="button" className="link" onClick={() => setEditing(building)}>
                      Editar
                    </button>
                    <button
                      type="button"
                      className="link"
                      onClick={() => run(() => archive(token, communityId, "buildings", building.id))}
                    >
                      Archivar
                    </button>
                  </>
                )}
              </RowActions>
            )}
          </li>
        ))}
      </ul>
      {isAdmin && !editing && (
        <button type="button" className="primary" onClick={() => setEditing("new")}>
          Agregar edificio
        </button>
      )}
      {editing && (
        <BuildingForm
          key={editing === "new" ? "new" : editing.id}
          initial={editing === "new" ? null : editing}
          onSubmit={(name) =>
            editing === "new"
              ? createBuilding(token, communityId, name)
              : updateBuilding(token, communityId, editing.id, name)
          }
          onDone={() => {
            setEditing(null);
            refresh();
          }}
          onCancel={() => setEditing(null)}
        />
      )}
    </section>
  );
}

function BuildingForm({
  initial,
  onSubmit,
  onDone,
  onCancel,
}: {
  initial: Building | null;
  onSubmit: (name: string) => Promise<unknown>;
  onDone: () => void;
  onCancel: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  return (
    <FormShell submit={() => onSubmit(name.trim())} onDone={onDone} onCancel={onCancel}>
      <label className="field">
        Nombre (torre, bloque, interior)
        <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={200} />
      </label>
    </FormShell>
  );
}

/** Submit and cancel buttons, a busy state and the error line, shared by every form. */
function FormShell({
  submit,
  onDone,
  onCancel,
  children,
}: {
  submit: () => Promise<unknown>;
  onDone: () => void;
  onCancel: () => void;
  children: ReactNode;
}) {
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await submit();
      onDone();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      {children}
      {error && <p className="form-error">{error}</p>}
      <div className="form-actions">
        <button type="submit" disabled={busy}>
          Guardar
        </button>
        <button type="button" className="link" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

// Units

function UnitsTab({ token, communityId, isAdmin }: ScreenProps) {
  const [showArchived, setShowArchived] = useState(false);
  const load = useCallback(
    () => listUnits(token, communityId, showArchived),
    [token, communityId, showArchived],
  );
  const { items, error, setError, refresh } = useList(load);
  const loadBuildings = useCallback(() => listBuildings(token, communityId, false), [token, communityId]);
  const { items: buildings } = useList(loadBuildings);
  const [editing, setEditing] = useState<PropertyUnit | "new" | null>(null);
  const [selected, setSelected] = useState<PropertyUnit | null>(null);
  const [filter, setFilter] = useState("");

  async function run(action: () => Promise<unknown>) {
    try {
      await action();
      await refresh();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (selected) {
    return (
      <UnitDetail
        token={token}
        communityId={communityId}
        isAdmin={isAdmin}
        unit={selected}
        onBack={() => setSelected(null)}
      />
    );
  }

  const buildingName = (id: string | null) => buildings?.find((b) => b.id === id)?.name;
  const visible = items?.filter((unit) => !filter || unit.buildingId === filter);

  return (
    <section>
      <div className="list-controls">
        {!!buildings?.length && (
          <label className="field inline">
            Edificio
            <select value={filter} onChange={(e) => setFilter(e.target.value)}>
              <option value="">Todos</option>
              {buildings.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </label>
        )}
        {isAdmin && <ArchivedToggle value={showArchived} onChange={setShowArchived} />}
      </div>
      {error && <p className="form-error">{error}</p>}
      {visible?.length === 0 && <p>No hay unidades.</p>}
      <ul className="record-list">
        {visible?.map((unit) => (
          <li key={unit.id} className={unit.archivedAt ? "record archived" : "record"}>
            <span>
              <button type="button" className="link strong" onClick={() => setSelected(unit)}>
                {unit.number}
              </button>
              <span className="muted">
                {" "}
                {UNIT_KINDS[unit.kind] ?? unit.kind}
                {buildingName(unit.buildingId) && ` · ${buildingName(unit.buildingId)}`}
                {unit.floor && ` · Piso ${unit.floor}`}
                {unit.areaM2 !== null && ` · ${unit.areaM2} m²`}
              </span>
              {unit.archivedAt && <span className="badge">Archivada</span>}
            </span>
            {isAdmin && (
              <RowActions>
                {unit.archivedAt ? (
                  <button
                    type="button"
                    className="link"
                    onClick={() => run(() => restore(token, communityId, "units", unit.id))}
                  >
                    Restaurar
                  </button>
                ) : (
                  <>
                    <button type="button" className="link" onClick={() => setEditing(unit)}>
                      Editar
                    </button>
                    <button
                      type="button"
                      className="link"
                      onClick={() => run(() => archive(token, communityId, "units", unit.id))}
                    >
                      Archivar
                    </button>
                  </>
                )}
              </RowActions>
            )}
          </li>
        ))}
      </ul>
      {isAdmin && !editing && (
        <button type="button" className="primary" onClick={() => setEditing("new")}>
          Agregar unidad
        </button>
      )}
      {editing && (
        <UnitForm
          key={editing === "new" ? "new" : editing.id}
          initial={editing === "new" ? null : editing}
          buildings={buildings ?? []}
          onSubmit={(input) =>
            editing === "new"
              ? createUnit(token, communityId, input)
              : updateUnit(token, communityId, editing.id, input)
          }
          onDone={() => {
            setEditing(null);
            refresh();
          }}
          onCancel={() => setEditing(null)}
        />
      )}
    </section>
  );
}

function UnitForm({
  initial,
  buildings,
  onSubmit,
  onDone,
  onCancel,
}: {
  initial: PropertyUnit | null;
  buildings: Building[];
  onSubmit: (input: UnitInput) => Promise<unknown>;
  onDone: () => void;
  onCancel: () => void;
}) {
  const [number, setNumber] = useState(initial?.number ?? "");
  const [kind, setKind] = useState(initial?.kind ?? "apartment");
  const [buildingId, setBuildingId] = useState(initial?.buildingId ?? "");
  const [floor, setFloor] = useState(initial?.floor ?? "");
  const [area, setArea] = useState(initial?.areaM2?.toString() ?? "");
  const [registryId, setRegistryId] = useState(initial?.registryId ?? "");

  return (
    <FormShell
      submit={() =>
        onSubmit({
          number: number.trim(),
          kind,
          buildingId: buildingId || null,
          floor: blankToNull(floor),
          areaM2: area === "" ? null : Number(area),
          registryId: blankToNull(registryId),
        })
      }
      onDone={onDone}
      onCancel={onCancel}
    >
      <label className="field">
        Número
        <input value={number} onChange={(e) => setNumber(e.target.value)} required maxLength={40} />
      </label>
      <label className="field">
        Tipo
        <select value={kind} onChange={(e) => setKind(e.target.value)}>
          {Object.entries(UNIT_KINDS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className="field">
        Edificio
        <select value={buildingId} onChange={(e) => setBuildingId(e.target.value)}>
          <option value="">Sin edificio</option>
          {buildings.map((b) => (
            <option key={b.id} value={b.id}>
              {b.name}
            </option>
          ))}
        </select>
      </label>
      <label className="field">
        Piso
        <input value={floor} onChange={(e) => setFloor(e.target.value)} maxLength={20} />
      </label>
      <label className="field">
        Área privada (m²)
        <input type="number" min="0" step="0.01" value={area} onChange={(e) => setArea(e.target.value)} />
      </label>
      <label className="field">
        Matrícula inmobiliaria
        <input value={registryId} onChange={(e) => setRegistryId(e.target.value)} maxLength={60} />
      </label>
    </FormShell>
  );
}

function UnitDetail({
  token,
  communityId,
  isAdmin,
  unit,
  onBack,
}: ScreenProps & { unit: PropertyUnit; onBack: () => void }) {
  const load = useCallback(
    () => listUnitRelationships(token, communityId, unit.id),
    [token, communityId, unit.id],
  );
  const { items, error, refresh } = useList(load);
  const loadPeople = useCallback(() => listPeople(token, communityId, true), [token, communityId]);
  const loadVisiblePeople = useCallback(() => listPeople(token, communityId, false), [token, communityId]);
  const { items: people } = useList(isAdmin ? loadPeople : loadVisiblePeople);
  const [adding, setAdding] = useState(false);
  const [ending, setEnding] = useState<UnitRelationship | null>(null);

  const name = (personId: string) => {
    const person = people?.find((p) => p.id === personId);
    return person ? personName(person) : "Persona";
  };

  return (
    <section>
      <button type="button" className="link" onClick={onBack}>
        ← Unidades
      </button>
      <h2>Unidad {unit.number}</h2>
      {error && <p className="form-error">{error}</p>}
      {items?.length === 0 && <p>Esta unidad no tiene vínculos.</p>}
      <RelationshipList
        relationships={items}
        label={(r) => name(r.personId)}
        action={(r) =>
          isAdmin && isOpen(r) ? (
            <button type="button" className="link" onClick={() => setEnding(r)}>
              Finalizar
            </button>
          ) : null
        }
      />
      {isAdmin && !unit.archivedAt && !adding && !ending && (
        <button type="button" className="primary" onClick={() => setAdding(true)}>
          Agregar vínculo
        </button>
      )}
      {adding && (
        <RelationshipForm
          people={(people ?? []).filter((p) => !p.archivedAt)}
          onSubmit={(input) => createRelationship(token, communityId, unit.id, input)}
          onDone={() => {
            setAdding(false);
            refresh();
          }}
          onCancel={() => setAdding(false)}
        />
      )}
      {ending && (
        <EndRelationshipForm
          relationship={ending}
          personName={name(ending.personId)}
          onSubmit={(validTo) => endRelationship(token, communityId, unit.id, ending.id, validTo)}
          onDone={() => {
            setEnding(null);
            refresh();
          }}
          onCancel={() => setEnding(null)}
        />
      )}
    </section>
  );
}

/**
 * Whether a relationship still has an end date to record. The server decides by the
 * community's own date, so a client in another time zone can be a day off, and the
 * server's `relationship_already_ended` covers that.
 */
function isOpen(relationship: UnitRelationship): boolean {
  return relationship.validTo === null || relationship.validTo > new Date().toISOString().slice(0, 10);
}

function RelationshipList({
  relationships,
  label,
  action,
}: {
  relationships: UnitRelationship[] | null;
  label: (relationship: UnitRelationship) => string;
  action: (relationship: UnitRelationship) => ReactNode;
}) {
  return (
    <ul className="record-list">
      {relationships?.map((r) => (
        <li key={r.id} className={isOpen(r) ? "record" : "record archived"}>
          <span>
            <strong>{label(r)}</strong>
            <span className="muted">
              {" "}
              {ROLES[r.role] ?? r.role}
              {r.share !== null && ` · ${r.share} %`} · desde {r.validFrom}
              {r.validTo && ` hasta ${r.validTo}`}
            </span>
          </span>
          <RowActions>{action(r)}</RowActions>
        </li>
      ))}
    </ul>
  );
}

function RelationshipForm({
  people,
  onSubmit,
  onDone,
  onCancel,
}: {
  people: Person[];
  onSubmit: (input: {
    personId: string;
    role: string;
    share: number | null;
    validFrom: string | null;
    validTo: string | null;
  }) => Promise<unknown>;
  onDone: () => void;
  onCancel: () => void;
}) {
  const [personId, setPersonId] = useState("");
  const [role, setRole] = useState("resident");
  const [share, setShare] = useState("");
  const [validFrom, setValidFrom] = useState("");
  const [validTo, setValidTo] = useState("");

  if (people.length === 0) {
    return <p>Primero registre a la persona en la pestaña Personas.</p>;
  }

  return (
    <FormShell
      submit={() =>
        onSubmit({
          personId,
          role,
          share: role === "owner" && share !== "" ? Number(share) : null,
          validFrom: validFrom || null,
          validTo: validTo || null,
        })
      }
      onDone={onDone}
      onCancel={onCancel}
    >
      <label className="field">
        Persona
        <select value={personId} onChange={(e) => setPersonId(e.target.value)} required>
          <option value="">Seleccione…</option>
          {people.map((p) => (
            <option key={p.id} value={p.id}>
              {personName(p)}
            </option>
          ))}
        </select>
      </label>
      <label className="field">
        Vínculo
        <select value={role} onChange={(e) => setRole(e.target.value)}>
          {Object.entries(ROLES).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      {role === "owner" && (
        <label className="field">
          Participación (%)
          <input
            type="number"
            min="0"
            max="100"
            step="0.0001"
            value={share}
            onChange={(e) => setShare(e.target.value)}
            required
          />
        </label>
      )}
      <label className="field">
        Desde (por defecto, hoy)
        <input type="date" value={validFrom} onChange={(e) => setValidFrom(e.target.value)} />
      </label>
      <label className="field">
        Hasta (opcional, último día)
        <input type="date" value={validTo} onChange={(e) => setValidTo(e.target.value)} />
      </label>
    </FormShell>
  );
}

function EndRelationshipForm({
  relationship,
  personName: name,
  onSubmit,
  onDone,
  onCancel,
}: {
  relationship: UnitRelationship;
  personName: string;
  onSubmit: (validTo: string | null) => Promise<unknown>;
  onDone: () => void;
  onCancel: () => void;
}) {
  const [validTo, setValidTo] = useState("");
  return (
    <FormShell submit={() => onSubmit(validTo || null)} onDone={onDone} onCancel={onCancel}>
      <p>
        Finalizar el vínculo de {name} como {ROLES[relationship.role] ?? relationship.role}. Queda en el
        historial.
      </p>
      <label className="field">
        Último día (por defecto, hoy)
        <input
          type="date"
          value={validTo}
          min={relationship.validFrom}
          max={relationship.validTo ?? undefined}
          onChange={(e) => setValidTo(e.target.value)}
        />
      </label>
    </FormShell>
  );
}

// People

function PeopleTab({ token, communityId, isAdmin }: ScreenProps) {
  const [showArchived, setShowArchived] = useState(false);
  const load = useCallback(
    () => listPeople(token, communityId, showArchived),
    [token, communityId, showArchived],
  );
  const { items, error, setError, refresh } = useList(load);
  const [editing, setEditing] = useState<Person | "new" | null>(null);
  const [selected, setSelected] = useState<Person | null>(null);

  async function run(action: () => Promise<unknown>) {
    try {
      await action();
      await refresh();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (selected) {
    return <PersonDetail token={token} communityId={communityId} person={selected} onBack={() => setSelected(null)} />;
  }

  return (
    <section>
      {isAdmin && <ArchivedToggle value={showArchived} onChange={setShowArchived} />}
      {error && <p className="form-error">{error}</p>}
      {items?.length === 0 && <p>No hay personas registradas.</p>}
      <ul className="record-list">
        {items?.map((person) => (
          <li key={person.id} className={person.archivedAt ? "record archived" : "record"}>
            <span>
              <button type="button" className="link strong" onClick={() => setSelected(person)}>
                {personName(person)}
              </button>
              {person.idNumber && (
                <span className="muted">
                  {" "}
                  {person.idType} {person.idNumber}
                </span>
              )}
              {(person.email || person.phone) && (
                <span className="muted">
                  {" "}
                  · {[person.email, person.phone].filter(Boolean).join(" · ")}
                </span>
              )}
              {person.archivedAt && <span className="badge">Archivada</span>}
            </span>
            {isAdmin && (
              <RowActions>
                {person.archivedAt ? (
                  <button
                    type="button"
                    className="link"
                    onClick={() => run(() => restore(token, communityId, "people", person.id))}
                  >
                    Restaurar
                  </button>
                ) : (
                  <>
                    <button type="button" className="link" onClick={() => setEditing(person)}>
                      Editar
                    </button>
                    <button
                      type="button"
                      className="link"
                      onClick={() => run(() => archive(token, communityId, "people", person.id))}
                    >
                      Archivar
                    </button>
                  </>
                )}
              </RowActions>
            )}
          </li>
        ))}
      </ul>
      {isAdmin && !editing && (
        <button type="button" className="primary" onClick={() => setEditing("new")}>
          Agregar persona
        </button>
      )}
      {editing && (
        <PersonForm
          key={editing === "new" ? "new" : editing.id}
          initial={editing === "new" ? null : editing}
          onSubmit={(input) =>
            editing === "new"
              ? createPerson(token, communityId, input)
              : updatePerson(token, communityId, editing.id, input)
          }
          onDone={() => {
            setEditing(null);
            refresh();
          }}
          onCancel={() => setEditing(null)}
        />
      )}
    </section>
  );
}

function PersonForm({
  initial,
  onSubmit,
  onDone,
  onCancel,
}: {
  initial: Person | null;
  onSubmit: (input: PersonInput) => Promise<unknown>;
  onDone: () => void;
  onCancel: () => void;
}) {
  const [kind, setKind] = useState(initial?.kind ?? "natural");
  const [givenName, setGivenName] = useState(initial?.givenName ?? "");
  const [surname, setSurname] = useState(initial?.surname ?? "");
  const [legalName, setLegalName] = useState(initial?.legalName ?? "");
  const [idType, setIdType] = useState(initial?.idType ?? "");
  const [idNumber, setIdNumber] = useState(initial?.idNumber ?? "");
  const [email, setEmail] = useState(initial?.email ?? "");
  const [phone, setPhone] = useState(initial?.phone ?? "");
  const natural = kind === "natural";

  return (
    <FormShell
      submit={() =>
        onSubmit({
          kind,
          givenName: natural ? blankToNull(givenName) : null,
          surname: natural ? blankToNull(surname) : null,
          legalName: natural ? null : blankToNull(legalName),
          idType: idType || null,
          idNumber: blankToNull(idNumber),
          email: blankToNull(email),
          phone: blankToNull(phone),
        })
      }
      onDone={onDone}
      onCancel={onCancel}
    >
      <label className="field">
        Tipo de persona
        <select value={kind} onChange={(e) => setKind(e.target.value)}>
          <option value="natural">Natural</option>
          <option value="legal">Jurídica</option>
        </select>
      </label>
      {natural ? (
        <>
          <label className="field">
            Nombres
            <input value={givenName} onChange={(e) => setGivenName(e.target.value)} required maxLength={120} />
          </label>
          <label className="field">
            Apellidos
            <input value={surname} onChange={(e) => setSurname(e.target.value)} required maxLength={120} />
          </label>
        </>
      ) : (
        <label className="field">
          Razón social
          <input value={legalName} onChange={(e) => setLegalName(e.target.value)} required maxLength={200} />
        </label>
      )}
      <label className="field">
        Tipo de documento
        <select value={idType} onChange={(e) => setIdType(e.target.value)} required={idNumber.trim() !== ""}>
          <option value="">Sin documento</option>
          {ID_TYPES.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </label>
      <label className="field">
        Número de documento
        <input
          value={idNumber}
          onChange={(e) => setIdNumber(e.target.value)}
          required={idType !== ""}
          maxLength={40}
        />
      </label>
      <label className="field">
        Correo electrónico
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={320} />
      </label>
      <label className="field">
        Teléfono
        <input value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={40} />
      </label>
      <p className="muted">
        Solo la persona decide si los demás miembros pueden ver sus datos, desde su perfil.
      </p>
    </FormShell>
  );
}

function PersonDetail({
  token,
  communityId,
  person,
  onBack,
}: {
  token: string;
  communityId: string;
  person: Person;
  onBack: () => void;
}) {
  const load = useCallback(
    () => listPersonRelationships(token, communityId, person.id),
    [token, communityId, person.id],
  );
  const { items, error } = useList(load);
  const loadUnits = useCallback(() => listUnits(token, communityId, false), [token, communityId]);
  const { items: units } = useList(loadUnits);

  return (
    <section>
      <button type="button" className="link" onClick={onBack}>
        ← Personas
      </button>
      <h2>{personName(person)}</h2>
      {error && <p className="form-error">{error}</p>}
      {items?.length === 0 && <p>Esta persona no tiene vínculos con ninguna unidad.</p>}
      <RelationshipList
        relationships={items}
        label={(r) => {
          const unit = units?.find((u) => u.id === r.unitId);
          return unit ? `Unidad ${unit.number}` : "Unidad archivada";
        }}
        action={() => null}
      />
    </section>
  );
}

// Members

function MembersTab({
  token,
  communityId,
  onSelfChanged,
}: ScreenProps & { onSelfChanged: () => void }) {
  const load = useCallback(() => listMembers(token, communityId), [token, communityId]);
  const { items, error, setError, refresh } = useList(load);
  const loadPeople = useCallback(() => listPeople(token, communityId, false), [token, communityId]);
  const { items: people } = useList(loadPeople);
  const loadInvitations = useCallback(() => listInvitations(token, communityId), [token, communityId]);
  const { items: invitations, refresh: refreshInvitations } = useList(loadInvitations);
  const [inviting, setInviting] = useState(false);
  const [issued, setIssued] = useState<Invitation | null>(null);

  async function run(action: () => Promise<unknown>) {
    try {
      await action();
      await Promise.all([refresh(), refreshInvitations()]);
      onSelfChanged();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  function remove(member: Member) {
    if (window.confirm(`¿Retirar a ${member.displayName} de la comunidad? Solo podrá volver con una nueva invitación.`)) {
      run(() => changeMemberStatus(token, communityId, member.membershipId, "remove"));
    }
  }

  return (
    <section>
      {error && <p className="form-error">{error}</p>}
      <ul className="record-list">
        {items?.map((member) => (
          <li key={member.membershipId} className={member.status === "active" ? "record column" : "record column archived"}>
            <span>
              <strong>{member.displayName}</strong>
              <span className="muted"> {member.email}</span>
              {member.status === "suspended" && <span className="badge">Suspendido</span>}
            </span>
            <span className="member-roles">
              {member.roles.map((role) => (
                <span key={role} className="badge">
                  {ASSIGNED_ROLES[role] ?? role}{" "}
                  <button
                    type="button"
                    className="link"
                    aria-label={`Quitar rol ${ASSIGNED_ROLES[role] ?? role} a ${member.displayName}`}
                    onClick={() => run(() => endRole(token, communityId, member.membershipId, role))}
                  >
                    ×
                  </button>
                </span>
              ))}
              <select
                aria-label={`Agregar rol a ${member.displayName}`}
                value=""
                onChange={(e) => {
                  const role = e.target.value;
                  if (role) {
                    run(() => assignRole(token, communityId, member.membershipId, role));
                  }
                }}
              >
                <option value="">Agregar rol…</option>
                {Object.entries(ASSIGNED_ROLES)
                  .filter(([value]) => !member.roles.includes(value))
                  .map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
              </select>
            </span>
            <span className="member-roles">
              <label className="field inline">
                Persona
                <select
                  aria-label={`Persona de ${member.displayName}`}
                  value={member.personId ?? ""}
                  onChange={(e) => {
                    const personId = e.target.value || null;
                    run(() => linkMemberPerson(token, communityId, member.membershipId, personId));
                  }}
                >
                  <option value="">Sin vincular</option>
                  {people?.map((person) => (
                    <option key={person.id} value={person.id}>
                      {personName(person)}
                    </option>
                  ))}
                </select>
              </label>
              <RowActions>
                <button
                  type="button"
                  className="link"
                  onClick={() =>
                    run(() =>
                      changeMemberStatus(
                        token,
                        communityId,
                        member.membershipId,
                        member.status === "active" ? "suspend" : "reactivate",
                      ),
                    )
                  }
                >
                  {member.status === "active" ? "Suspender" : "Reactivar"}
                </button>
                <button type="button" className="link" onClick={() => remove(member)}>
                  Retirar
                </button>
              </RowActions>
            </span>
          </li>
        ))}
      </ul>
      <p className="muted">
        Vincular a un miembro con una persona le da acceso a todo ese registro, incluido el documento de identidad.
        Verifique que sea la persona correcta.
      </p>

      <h2>Invitaciones</h2>
      {issued?.token && (
        <div className="notice">
          <p>
            Envíe este enlace a {issued.email}. Se muestra solo esta vez, y vence en 7 días. Solo funciona con una cuenta
            con ese correo.
          </p>
          <input
            className="link-box"
            readOnly
            aria-label="Enlace de invitación"
            value={invitationLink(issued.token)}
            onFocus={(e) => e.target.select()}
          />
        </div>
      )}
      {invitations?.length === 0 && <p>No hay invitaciones abiertas.</p>}
      <ul className="record-list">
        {invitations?.map((invitation) => {
          const expired = new Date(invitation.expiresAt) <= new Date();
          return (
            <li key={invitation.id} className={expired ? "record archived" : "record"}>
              <span>
                <strong>{invitation.email}</strong>
                {expired ? (
                  <span className="badge">Vencida</span>
                ) : (
                  <span className="muted"> vence el {invitation.expiresAt.slice(0, 10)}</span>
                )}
              </span>
              <RowActions>
                <button
                  type="button"
                  className="link"
                  aria-label={`Revocar invitación de ${invitation.email}`}
                  onClick={() => run(() => revokeInvitation(token, communityId, invitation.id))}
                >
                  Revocar
                </button>
              </RowActions>
            </li>
          );
        })}
      </ul>
      {!inviting && (
        <button type="button" className="primary" onClick={() => setInviting(true)}>
          Invitar
        </button>
      )}
      {inviting && (
        <InvitationForm
          people={(people ?? []).filter((person) => !items?.some((member) => member.personId === person.id))}
          onSubmit={async (email, personId) => {
            setIssued(await createInvitation(token, communityId, email, personId));
          }}
          onDone={() => {
            setInviting(false);
            refreshInvitations();
          }}
          onCancel={() => setInviting(false)}
        />
      )}
    </section>
  );
}

function InvitationForm({
  people,
  onSubmit,
  onDone,
  onCancel,
}: {
  people: Person[];
  onSubmit: (email: string, personId: string | null) => Promise<unknown>;
  onDone: () => void;
  onCancel: () => void;
}) {
  const [email, setEmail] = useState("");
  const [personId, setPersonId] = useState("");
  return (
    <FormShell submit={() => onSubmit(email.trim(), personId || null)} onDone={onDone} onCancel={onCancel}>
      <label className="field">
        Correo de la persona invitada
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required maxLength={320} />
      </label>
      <label className="field">
        Vincular con su registro (opcional)
        <select value={personId} onChange={(e) => setPersonId(e.target.value)}>
          <option value="">Sin vincular</option>
          {people.map((person) => (
            <option key={person.id} value={person.id}>
              {personName(person)}
            </option>
          ))}
        </select>
      </label>
    </FormShell>
  );
}

// Audit log

const AUDIT_ENTITIES: Record<string, string> = {
  community: "Comunidad",
  membership: "Miembro",
  invitation: "Invitación",
  building: "Edificio",
  unit: "Unidad",
  person: "Persona",
  unit_relationship: "Vínculo",
};

const AUDIT_ACTIONS: Record<string, string> = {
  created: "creado",
  updated: "editado",
  archived: "archivado",
  restored: "restaurado",
  ended: "finalizado",
  founded: "fundador",
  accepted: "aceptada",
  revoked: "revocada",
  role_assigned: "rol asignado",
  role_ended: "rol retirado",
  suspended: "suspendido",
  reactivated: "reactivado",
  removed: "retirado",
  left: "salió",
  person_linked: "vinculado a una persona",
  person_unlinked: "desvinculado de una persona",
  profile_updated: "perfil editado",
};

const PAGE_SIZE = 50;

function AuditTab({ token, communityId }: { token: string; communityId: string }) {
  const [events, setEvents] = useState<AuditEvent[] | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadMore = useCallback(
    async (before: string | null) => {
      try {
        const page = await listAuditEvents(token, communityId, before);
        setEvents((current) => (before ? [...(current ?? []), ...page] : page));
        setHasMore(page.length === PAGE_SIZE);
      } catch (err) {
        setError(errorMessage(err));
      }
    },
    [token, communityId],
  );

  useEffect(() => {
    loadMore(null);
  }, [loadMore]);

  return (
    <section>
      {error && <p className="form-error">{error}</p>}
      {events?.length === 0 && <p>Todavía no hay eventos.</p>}
      <ul className="record-list">
        {events?.map((event) => (
          <li key={event.id} className="record">
            <span>
              <strong>{AUDIT_ENTITIES[event.entityType] ?? event.entityType}</strong>{" "}
              {AUDIT_ACTIONS[event.action] ?? event.action}
              {event.summary && <span className="muted"> · {event.summary}</span>}
            </span>
            <span className="muted">
              {event.actorName ?? "Sistema"} · {new Date(event.occurredAt).toLocaleString("es-CO")}
            </span>
          </li>
        ))}
      </ul>
      {hasMore && events && (
        <button type="button" className="link" onClick={() => loadMore(events[events.length - 1].id)}>
          Ver más
        </button>
      )}
    </section>
  );
}

// The caller's own profile

function VisibilityField({
  label,
  value,
  disabled,
  onChange,
}: {
  label: string;
  value: Visibility;
  disabled?: boolean;
  onChange: (next: Visibility) => void;
}) {
  return (
    <label className="field">
      {label}
      <select value={value} disabled={disabled} onChange={(e) => onChange(e.target.value as Visibility)}>
        <option value="privileged">Solo administración y consejo</option>
        <option value="community">Todos los miembros</option>
      </select>
    </label>
  );
}

function ProfileTab({
  token,
  communityId,
  account,
  onAccountChanged,
  onLeft,
}: {
  token: string;
  communityId: string;
  account: Account;
  onAccountChanged: (account: Account) => void;
  onLeft: () => void;
}) {
  const [leaveError, setLeaveError] = useState<string | null>(null);
  const [person, setPerson] = useState<Person | null>(null);
  const [state, setState] = useState<"loading" | "ready" | "unlinked" | "error">("loading");

  useEffect(() => {
    getProfile(token, communityId)
      .then((loaded) => {
        setPerson(loaded);
        setState("ready");
      })
      .catch((err) => setState(err instanceof ApiError && err.code === "profile_not_linked" ? "unlinked" : "error"));
  }, [token, communityId]);

  return (
    <section>
      <AccountForm token={token} account={account} onSaved={onAccountChanged} />
      <h2>En esta comunidad</h2>
      {state === "loading" && <p>Cargando…</p>}
      {state === "error" && <p className="form-error">No se pudo cargar su perfil.</p>}
      {state === "unlinked" && (
        <p>La administración todavía no ha vinculado su cuenta con su registro en esta comunidad.</p>
      )}
      {state === "ready" && person && (
        <PrivacyForm key={person.id} token={token} communityId={communityId} person={person} onSaved={setPerson} />
      )}
      <h2>Salir</h2>
      {leaveError && <p className="form-error">{leaveError}</p>}
      <button
        type="button"
        className="link"
        onClick={() => {
          if (window.confirm("¿Salir de esta comunidad? Solo podrá volver con una nueva invitación.")) {
            leaveCommunity(token, communityId)
              .then(onLeft)
              .catch((err) => setLeaveError(errorMessage(err)));
          }
        }}
      >
        Salir de esta comunidad
      </button>
    </section>
  );
}

function AccountForm({
  token,
  account,
  onSaved,
}: {
  token: string;
  account: Account;
  onSaved: (account: Account) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [displayName, setDisplayName] = useState(account.displayName);
  const [locale, setLocale] = useState(account.locale);

  if (!editing) {
    return (
      <>
        <h2>Su cuenta</h2>
        <dl className="details">
          <dt>Nombre</dt>
          <dd>{account.displayName}</dd>
          <dt>Correo de acceso</dt>
          <dd>{account.email}</dd>
        </dl>
        <button type="button" className="link" onClick={() => setEditing(true)}>
          Editar cuenta
        </button>
      </>
    );
  }
  return (
    <FormShell
      submit={async () => onSaved(await updateMe(token, displayName.trim(), locale))}
      onDone={() => setEditing(false)}
      onCancel={() => setEditing(false)}
    >
      <label className="field">
        Nombre
        <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required maxLength={200} />
      </label>
      <label className="field">
        Idioma
        <select value={locale} onChange={(e) => setLocale(e.target.value)}>
          <option value="es-CO">Español</option>
          <option value="en">English</option>
        </select>
      </label>
    </FormShell>
  );
}

function PrivacyForm({
  token,
  communityId,
  person,
  onSaved,
}: {
  token: string;
  communityId: string;
  person: Person;
  onSaved: (person: Person) => void;
}) {
  const [email, setEmail] = useState(person.email ?? "");
  const [phone, setPhone] = useState(person.phone ?? "");
  const [nameVisibility, setNameVisibility] = useState(person.nameVisibility);
  const [emailVisibility, setEmailVisibility] = useState(person.emailVisibility);
  const [phoneVisibility, setPhoneVisibility] = useState(person.phoneVisibility);
  const [unitsVisibility, setUnitsVisibility] = useState(person.unitsVisibility);
  const [saved, setSaved] = useState(false);
  const hidden = nameVisibility === "privileged";

  return (
    <>
      <dl className="details">
        <dt>Nombre</dt>
        <dd>{personName(person)}</dd>
        <dt>Documento</dt>
        <dd>{person.idNumber ? `${person.idType} ${person.idNumber}` : "—"}</dd>
      </dl>
      <p className="muted">Para corregir su nombre o documento, escriba a la administración.</p>
      <FormShell
        submit={async () => {
          setSaved(false);
          onSaved(
            await updateProfile(token, communityId, {
              email: blankToNull(email),
              phone: blankToNull(phone),
              nameVisibility,
              emailVisibility,
              phoneVisibility,
              unitsVisibility,
            }),
          );
          setSaved(true);
        }}
        onDone={() => undefined}
        onCancel={() => {
          setEmail(person.email ?? "");
          setPhone(person.phone ?? "");
          setNameVisibility(person.nameVisibility);
          setEmailVisibility(person.emailVisibility);
          setPhoneVisibility(person.phoneVisibility);
          setUnitsVisibility(person.unitsVisibility);
          setSaved(false);
        }}
      >
        <label className="field">
          Correo electrónico
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} maxLength={320} />
        </label>
        <label className="field">
          Teléfono
          <input value={phone} onChange={(e) => setPhone(e.target.value)} maxLength={40} />
        </label>
        <VisibilityField label="¿Quién puede ver su nombre?" value={nameVisibility} onChange={setNameVisibility} />
        <VisibilityField
          label="¿Quién puede ver su correo?"
          value={emailVisibility}
          disabled={hidden}
          onChange={setEmailVisibility}
        />
        <VisibilityField
          label="¿Quién puede ver su teléfono?"
          value={phoneVisibility}
          disabled={hidden}
          onChange={setPhoneVisibility}
        />
        <VisibilityField
          label="¿Quién puede ver sus unidades?"
          value={unitsVisibility}
          disabled={hidden}
          onChange={setUnitsVisibility}
        />
        <p className="muted">
          La administración, la contaduría, la revisoría fiscal, la tesorería y el consejo siempre ven sus datos
          de contacto. Quienes comparten una unidad con usted ven su nombre y su vínculo con ella.
          {hidden && " Mientras oculte su nombre, lo demás también queda oculto para los otros miembros."}
        </p>
        {saved && <p>Guardado.</p>}
      </FormShell>
    </>
  );
}

// Community details

function DetailsTab({
  community,
  token,
  isAdmin,
  onSaved,
}: {
  community: Community;
  token: string;
  isAdmin: boolean;
  onSaved: (community: Community) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(community.name);
  const [legalName, setLegalName] = useState(community.legalName ?? "");
  const [taxId, setTaxId] = useState(community.taxId ?? "");
  const [address, setAddress] = useState(community.address ?? "");

  if (!editing) {
    return (
      <section>
        <dl className="details">
          <dt>Clave</dt>
          <dd>{community.key}</dd>
          <dt>Razón social</dt>
          <dd>{community.legalName ?? "—"}</dd>
          <dt>NIT</dt>
          <dd>{community.taxId ?? "—"}</dd>
          <dt>Dirección</dt>
          <dd>{community.address ?? "—"}</dd>
          <dt>País, moneda y zona horaria</dt>
          <dd>
            {community.country}, {community.currency}, {community.timezone}
          </dd>
        </dl>
        {isAdmin && (
          <button type="button" className="primary" onClick={() => setEditing(true)}>
            Editar
          </button>
        )}
      </section>
    );
  }

  return (
    <FormShell
      submit={async () =>
        onSaved(
          await updateCommunity(token, community.id, {
            name: name.trim(),
            legalName: blankToNull(legalName),
            taxId: blankToNull(taxId),
            address: blankToNull(address),
          }),
        )
      }
      onDone={() => setEditing(false)}
      onCancel={() => setEditing(false)}
    >
      <label className="field">
        Nombre
        <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={200} />
      </label>
      <label className="field">
        Razón social
        <input value={legalName} onChange={(e) => setLegalName(e.target.value)} maxLength={200} />
      </label>
      <label className="field">
        NIT
        <input value={taxId} onChange={(e) => setTaxId(e.target.value)} maxLength={32} />
      </label>
      <label className="field">
        Dirección
        <input value={address} onChange={(e) => setAddress(e.target.value)} />
      </label>
    </FormShell>
  );
}
