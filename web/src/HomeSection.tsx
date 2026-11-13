import type { Home } from "./api";
import { ASSIGNED_ROLES, ROLES, UNIT_KINDS } from "./labels";
import { ADMIN_SECTIONS, type AdminSection, type Section } from "./sections";

/**
 * The member's dashboard, from GET /communities/{id}/home. Android and iOS show
 * the same blocks in the same order: roles, my units, administration.
 */
export default function HomeSection({
  home,
  adminSections,
  onOpen,
}: {
  home: Home;
  adminSections: AdminSection[];
  onOpen: (section: Section) => void;
}) {
  const { community, administration } = home;

  return (
    <section className="home">
      {community.address && <p className="muted">{community.address}</p>}
      {home.roles.length > 0 && (
        <p className="member-roles">
          {home.roles.map((role) => (
            <span key={role} className="badge">
              {ASSIGNED_ROLES[role] ?? role}
            </span>
          ))}
        </p>
      )}

      <h2>Mis unidades</h2>
      {home.personId === null ? (
        <p>La administración todavía no ha vinculado su cuenta con su registro en esta comunidad.</p>
      ) : home.units.length === 0 ? (
        <p>No tiene unidades vinculadas hoy.</p>
      ) : (
        <ul className="card-grid">
          {home.units.map((unit) => (
            <li key={unit.unitId} className="card">
              <strong>{unit.building ? `${unit.building} · ${unit.number}` : unit.number}</strong>
              <span className="muted">{UNIT_KINDS[unit.kind] ?? unit.kind}</span>
              <span>{unit.roles.map((role) => ROLES[role] ?? role).join(", ")}</span>
            </li>
          ))}
        </ul>
      )}
      <p className="muted">Los saldos y estados de cuenta aparecerán aquí cuando exista la facturación.</p>

      {administration && (
        <>
          <h2>Administración</h2>
          <ul className="card-grid">
            <CountCard label="Unidades" count={administration.units} onOpen={() => onOpen("units")} />
            <CountCard label="Personas" count={administration.people} onOpen={() => onOpen("people")} />
            <CountCard label="Edificios" count={administration.buildings} onOpen={() => onOpen("buildings")} />
            <CountCard
              label="Miembros"
              count={administration.members}
              detail={[
                administration.suspendedMembers > 0 && `${administration.suspendedMembers} suspendidos`,
                administration.pendingInvitations > 0 &&
                  `${administration.pendingInvitations} invitaciones pendientes`,
              ]
                .filter(Boolean)
                .join(" · ")}
              onOpen={() => onOpen("members")}
            />
          </ul>
        </>
      )}

      {adminSections.length > 0 && (
        // On a phone the bottom bar has only the member sections, so the
        // administration ones are reached from here.
        <nav className="admin-links mobile-only" aria-label="Administración">
          {adminSections.map((section) => (
            <button key={section} type="button" className="link" onClick={() => onOpen(section)}>
              {ADMIN_SECTIONS[section]}
            </button>
          ))}
        </nav>
      )}
    </section>
  );
}

function CountCard({
  label,
  count,
  detail,
  onOpen,
}: {
  label: string;
  count: number;
  detail?: string;
  onOpen: () => void;
}) {
  return (
    <li className="card">
      <button type="button" className="card-button" onClick={onOpen}>
        <span className="count">{count}</span>
        <span>{label}</span>
        {detail && <span className="muted">{detail}</span>}
      </button>
    </li>
  );
}
