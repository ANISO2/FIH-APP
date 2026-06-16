# FIH 2025 Companion

A **read-only** companion system that lives alongside the existing FIH 2025
ticketing platform. The legacy system owns all writes; this system only reads
from the shared PostgreSQL database.

```
fih-companion/
├── fih-companion-api/   # Spring Boot backend (Phase 1 — done)
├── fih-admin/           # Angular admin web app (later phase)
├── fih-verifier/        # Flutter mobile verifier (later phase)
├── db/                  # SQL scripts (read-only role, helper queries)
│   └── 01_create_readonly_role.sql
├── docs/
│   └── PHASE1_WALKTHROUGH.md   # step-by-step guide for Phase 1
└── README.md
```

## Quick start (Phase 1)

See `docs/PHASE1_WALKTHROUGH.md` for the full beginner walkthrough. Short version:

1. Install JDK 17, Maven, PostgreSQL 16.
2. Restore the backup into a **local** dev database:
   ```
   createdb -U postgres billeterie_fih_dev
   pg_restore -U postgres -d billeterie_fih_dev --no-owner --no-privileges billeterie-fih-2025.backup
   ```
3. Create the SELECT-only role:
   ```
   psql -U postgres -d billeterie_fih_dev -f db/01_create_readonly_role.sql
   ```
4. Run the backend:
   ```
   cd fih-companion-api
   mvn spring-boot:run
   ```
5. Verify:
   - `http://localhost:8080/api/events` → 34 events
   - `http://localhost:8080/api/diagnostics/db` → user `fih_ro`, count 34, write **rejected**

## The one rule

This system **cannot write** to the database. That is enforced by the `fih_ro`
PostgreSQL role (SELECT only), not merely by careful code.
