// Thin client for the ProHo backend (api/openapi/openapi.yaml). Talks to
// localhost:8080 in dev; see docs/architecture/client-screens.md for the
// workflow this follows.

const API_BASE = "http://localhost:8080";

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
  ) {
    super(code);
  }
}

export interface Account {
  id: string;
  email: string;
  displayName: string;
  locale: string;
  status: string;
}

export interface Community {
  id: string;
  key: string;
  name: string;
  legalName: string | null;
  taxId: string | null;
  kind: string;
  plan: string | null;
  country: string;
  currency: string;
  timezone: string;
  address: string | null;
}

export interface MyMembership {
  membershipId: string;
  personId: string | null;
  roles: string[];
}

/** One of the caller's own units today (GET /communities/{id}/home). */
export interface MyUnit {
  unitId: string;
  number: string;
  kind: string;
  building: string | null;
  roles: string[];
}

export interface AdministrationSummary {
  buildings: number;
  units: number;
  people: number;
  members: number;
  suspendedMembers: number;
  pendingInvitations: number;
}

/** The dashboard: the same data on web, Android and iOS. */
export interface Home {
  community: Community;
  membershipId: string;
  personId: string | null;
  roles: string[];
  units: MyUnit[];
  administration: AdministrationSummary | null;
}

export interface Member {
  membershipId: string;
  displayName: string;
  email: string;
  personId: string | null;
  status: string;
  roles: string[];
}

export interface Invitation {
  id: string;
  email: string;
  personId: string | null;
  createdAt: string;
  expiresAt: string;
  token: string | null;
}

export interface InvitationPreview {
  communityName: string;
  email: string;
  matchesAccount: boolean;
}

export interface AuditEvent {
  id: string;
  occurredAt: string;
  entityType: string;
  entityId: string;
  action: string;
  summary: string | null;
  actorName: string | null;
  requestId: string | null;
}

export type Visibility = "privileged" | "community";

export interface ProfileInput {
  email: string | null;
  phone: string | null;
  nameVisibility: Visibility;
  emailVisibility: Visibility;
  phoneVisibility: Visibility;
  unitsVisibility: Visibility;
}

export interface Building {
  id: string;
  communityId: string;
  name: string;
  archivedAt: string | null;
}

export interface UnitInput {
  number: string;
  kind: string;
  buildingId: string | null;
  floor: string | null;
  areaM2: number | null;
  registryId: string | null;
}

export interface PropertyUnit extends UnitInput {
  id: string;
  communityId: string;
  archivedAt: string | null;
}

export interface PersonInput {
  kind: string;
  givenName: string | null;
  surname: string | null;
  legalName: string | null;
  idType: string | null;
  idNumber: string | null;
  email: string | null;
  phone: string | null;
}

export interface Person extends PersonInput {
  id: string;
  communityId: string;
  nameVisibility: Visibility;
  emailVisibility: Visibility;
  phoneVisibility: Visibility;
  unitsVisibility: Visibility;
  archivedAt: string | null;
}

export interface UnitRelationship {
  id: string;
  unitId: string;
  personId: string;
  role: string;
  share: number | null;
  validFrom: string;
  validTo: string | null;
}

interface LoginResponse {
  token: string;
  expiresAt: string;
  account: Account;
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (!response.ok) {
    let code = "unknown_error";
    try {
      const body = (await response.json()) as { error?: string };
      code = body.error ?? code;
    } catch {
      // No JSON body (e.g. a 401 that never reached a controller).
    }
    throw new ApiError(response.status, code);
  }
  if (response.status === 204 || response.status === 202) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export function registerAccount(
  email: string,
  password: string,
  displayName: string,
): Promise<Account> {
  return request<Account>("/accounts", {
    method: "POST",
    body: JSON.stringify({ email, password, displayName }),
  });
}

export function login(email: string, password: string): Promise<LoginResponse> {
  return request<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function me(token: string): Promise<Account> {
  return request<Account>("/me", {}, token);
}

export function logout(token: string): Promise<void> {
  return request<void>("/auth/logout", { method: "POST" }, token);
}

export function listCommunities(token: string): Promise<Community[]> {
  return request<Community[]>("/communities", {}, token);
}

export function createCommunity(
  token: string,
  input: { key: string; name: string; kind: string },
): Promise<Community> {
  return request<Community>(
    "/communities",
    { method: "POST", body: JSON.stringify(input) },
    token,
  );
}

function send<T>(token: string, method: string, path: string, body?: unknown): Promise<T> {
  return request<T>(
    path,
    { method, body: body === undefined ? undefined : JSON.stringify(body) },
    token,
  );
}

export function updateMe(token: string, displayName: string, locale: string): Promise<Account> {
  return send<Account>(token, "PUT", "/me", { displayName, locale });
}

export function getProfile(token: string, communityId: string): Promise<Person> {
  return request<Person>(`/communities/${communityId}/profile`, {}, token);
}

export function updateProfile(token: string, communityId: string, input: ProfileInput): Promise<Person> {
  return send<Person>(token, "PUT", `/communities/${communityId}/profile`, input);
}

export function listMembers(token: string, communityId: string): Promise<Member[]> {
  return request<Member[]>(`/communities/${communityId}/memberships`, {}, token);
}

export function linkMemberPerson(
  token: string,
  communityId: string,
  membershipId: string,
  personId: string | null,
): Promise<void> {
  return send<void>(token, "PUT", `/communities/${communityId}/memberships/${membershipId}/person`, { personId });
}

export function createInvitation(
  token: string,
  communityId: string,
  email: string,
  personId: string | null,
): Promise<Invitation> {
  return send<Invitation>(token, "POST", `/communities/${communityId}/invitations`, { email, personId });
}

export function listInvitations(token: string, communityId: string): Promise<Invitation[]> {
  return request<Invitation[]>(`/communities/${communityId}/invitations`, {}, token);
}

export function revokeInvitation(token: string, communityId: string, invitationId: string): Promise<void> {
  return send<void>(token, "POST", `/communities/${communityId}/invitations/${invitationId}/revoke`);
}

export function previewInvitation(token: string, invitationToken: string): Promise<InvitationPreview> {
  return send<InvitationPreview>(token, "POST", "/invitations/preview", { token: invitationToken });
}

export function acceptInvitation(token: string, invitationToken: string): Promise<unknown> {
  return send(token, "POST", "/invitations/accept", { token: invitationToken });
}

export function assignRole(token: string, communityId: string, membershipId: string, role: string): Promise<Member> {
  return send<Member>(token, "POST", `/communities/${communityId}/memberships/${membershipId}/roles`, { role });
}

export function endRole(token: string, communityId: string, membershipId: string, role: string): Promise<Member> {
  return send<Member>(token, "POST", `/communities/${communityId}/memberships/${membershipId}/roles/${role}/end`);
}

export function changeMemberStatus(
  token: string,
  communityId: string,
  membershipId: string,
  action: "suspend" | "reactivate" | "remove",
): Promise<unknown> {
  return send(token, "POST", `/communities/${communityId}/memberships/${membershipId}/${action}`);
}

export function leaveCommunity(token: string, communityId: string): Promise<void> {
  return send<void>(token, "POST", `/communities/${communityId}/membership/leave`);
}

export function listAuditEvents(
  token: string,
  communityId: string,
  before: string | null,
): Promise<AuditEvent[]> {
  const page = before ? `&before=${before}` : "";
  return request<AuditEvent[]>(`/communities/${communityId}/audit-events?limit=50${page}`, {}, token);
}

export function getCommunity(token: string, communityId: string): Promise<Community> {
  return request<Community>(`/communities/${communityId}`, {}, token);
}

export function updateCommunity(
  token: string,
  communityId: string,
  input: { name: string; legalName: string | null; taxId: string | null; address: string | null },
): Promise<Community> {
  return send<Community>(token, "PUT", `/communities/${communityId}`, input);
}

export function getHome(token: string, communityId: string): Promise<Home> {
  return request<Home>(`/communities/${communityId}/home`, {}, token);
}

export function getMyMembership(token: string, communityId: string): Promise<MyMembership> {
  return request<MyMembership>(`/communities/${communityId}/membership`, {}, token);
}

export function listBuildings(
  token: string,
  communityId: string,
  includeArchived: boolean,
): Promise<Building[]> {
  return request<Building[]>(
    `/communities/${communityId}/buildings?includeArchived=${includeArchived}`,
    {},
    token,
  );
}

export function createBuilding(token: string, communityId: string, name: string): Promise<Building> {
  return send<Building>(token, "POST", `/communities/${communityId}/buildings`, { name });
}

export function updateBuilding(
  token: string,
  communityId: string,
  buildingId: string,
  name: string,
): Promise<Building> {
  return send<Building>(token, "PUT", `/communities/${communityId}/buildings/${buildingId}`, { name });
}

export function listUnits(
  token: string,
  communityId: string,
  includeArchived: boolean,
): Promise<PropertyUnit[]> {
  return request<PropertyUnit[]>(
    `/communities/${communityId}/units?includeArchived=${includeArchived}`,
    {},
    token,
  );
}

export function createUnit(token: string, communityId: string, input: UnitInput): Promise<PropertyUnit> {
  return send<PropertyUnit>(token, "POST", `/communities/${communityId}/units`, input);
}

export function updateUnit(
  token: string,
  communityId: string,
  unitId: string,
  input: UnitInput,
): Promise<PropertyUnit> {
  return send<PropertyUnit>(token, "PUT", `/communities/${communityId}/units/${unitId}`, input);
}

export function listPeople(
  token: string,
  communityId: string,
  includeArchived: boolean,
): Promise<Person[]> {
  return request<Person[]>(
    `/communities/${communityId}/people?includeArchived=${includeArchived}`,
    {},
    token,
  );
}

export function createPerson(token: string, communityId: string, input: PersonInput): Promise<Person> {
  return send<Person>(token, "POST", `/communities/${communityId}/people`, input);
}

export function updatePerson(
  token: string,
  communityId: string,
  personId: string,
  input: PersonInput,
): Promise<Person> {
  return send<Person>(token, "PUT", `/communities/${communityId}/people/${personId}`, input);
}

type Archivable = "buildings" | "units" | "people";

export function archive(
  token: string,
  communityId: string,
  collection: Archivable,
  id: string,
): Promise<unknown> {
  return send(token, "POST", `/communities/${communityId}/${collection}/${id}/archive`);
}

export function restore(
  token: string,
  communityId: string,
  collection: Archivable,
  id: string,
): Promise<unknown> {
  return send(token, "POST", `/communities/${communityId}/${collection}/${id}/restore`);
}

export function listUnitRelationships(
  token: string,
  communityId: string,
  unitId: string,
): Promise<UnitRelationship[]> {
  return request<UnitRelationship[]>(`/communities/${communityId}/units/${unitId}/relationships`, {}, token);
}

export function listPersonRelationships(
  token: string,
  communityId: string,
  personId: string,
): Promise<UnitRelationship[]> {
  return request<UnitRelationship[]>(`/communities/${communityId}/people/${personId}/relationships`, {}, token);
}

export function createRelationship(
  token: string,
  communityId: string,
  unitId: string,
  input: {
    personId: string;
    role: string;
    share: number | null;
    validFrom: string | null;
    validTo: string | null;
  },
): Promise<UnitRelationship> {
  return send<UnitRelationship>(token, "POST", `/communities/${communityId}/units/${unitId}/relationships`, input);
}

export function endRelationship(
  token: string,
  communityId: string,
  unitId: string,
  relationshipId: string,
  validTo: string | null,
): Promise<UnitRelationship> {
  return send<UnitRelationship>(
    token,
    "POST",
    `/communities/${communityId}/units/${unitId}/relationships/${relationshipId}/end`,
    { validTo },
  );
}
