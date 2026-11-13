// The navigation every client shares (docs/architecture/client-screens.md).

/** Sections every member sees, in the same order on web, Android and iOS. */
export type MemberSection = "home" | "units" | "people" | "profile";
/** Administration sections, each shown only to the roles that can use it. */
export type AdminSection = "members" | "buildings" | "details" | "audit";
export type Section = MemberSection | AdminSection;

export const MEMBER_SECTIONS: Record<MemberSection, string> = {
  home: "Inicio",
  units: "Unidades",
  people: "Directorio",
  profile: "Mi perfil",
};

export const ADMIN_SECTIONS: Record<AdminSection, string> = {
  members: "Miembros",
  buildings: "Edificios",
  details: "Comunidad",
  audit: "Historial",
};

/** The administration sections these roles can open. */
export function adminSectionsFor(roles: string[]): AdminSection[] {
  const admin = roles.includes("administrator");
  return (Object.keys(ADMIN_SECTIONS) as AdminSection[]).filter((section) =>
    section === "audit" ? admin || roles.includes("auditor") : admin,
  );
}
