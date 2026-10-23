# Seed data

`demo-community.json` describes a small fictional building, Edificio Los
Robles, for development and demos. It is adapted from the 2016 ProHo sample
data (see `docs/legacy-salvage.md`) and exercises the cases the MVP must get
right:

- An owner who does not live in the unit (212, 307, 702) and a tenant (212).
- A household member without an account (Marta, 509).
- Ordinary and extraordinary assessments allocated by ownership coefficient.
  Coefficients add up to 100 and each `expected_by_unit` adds up to `total`.
- Confirmed payments in cash and by transfer, and one resident-reported
  payment still waiting for the treasurer (105).
- Published, public, and pending-approval posts, and marketplace listings.

The format is a neutral description, not a database dump. The backend will
load it through its own services once those exist, so that journal entries
are produced by the posting rules rather than inserted directly.

All names are invented and all emails use the reserved `example.org` domain.
Never add real personal data here.
