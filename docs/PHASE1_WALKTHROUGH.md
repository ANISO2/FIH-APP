# Phase 1 Walkthrough — Foundations & Read-Only Postgres Integration (Windows)

This guide takes you from an **empty folder** to a running Spring Boot backend
that can **read** the FIH 2025 database and is **physically unable to write** to
it. Every command below is for **Windows** (PowerShell). After each step there's
a **"Success looks like"** note.

> The single most important idea: the read-only guarantee comes from a dedicated
> PostgreSQL role (`fih_ro`) that only has `SELECT`. Even if our code had a bug
> that tried to write, the database itself rejects it.

---

## Step 0 — Install the tools

Open **PowerShell** (you do not need admin for `winget` in most setups). Install
and verify each tool with the one command that proves it works.

### JDK 17
```powershell
winget install Microsoft.OpenJDK.17
# close & reopen PowerShell, then:
java -version
```
**Success looks like:** a line containing `openjdk version "17...`.

### Maven
```powershell
winget install Apache.Maven
# reopen PowerShell, then:
mvn -v
```
**Success looks like:** `Apache Maven 3.x` plus the Java 17 home it found.

> Alternative if `winget` is missing: download Maven from
> https://maven.apache.org/download.cgi, unzip to `C:\tools\maven`, add
> `C:\tools\maven\bin` to your PATH.

### PostgreSQL 16 (includes `psql` and `pg_restore`)
Download the **EDB installer** from
https://www.postgresql.org/download/windows/ and install PostgreSQL 16. During
setup it asks for a **postgres superuser password** — remember it. Tick the
command-line tools. Then add the bin folder to PATH (default:
`C:\Program Files\PostgreSQL\16\bin`).

```powershell
psql --version
pg_restore --version
```
**Success looks like:** both print `... (PostgreSQL) 16.x`.

### IDE (recommended for a beginner)
Install **IntelliJ IDEA Community Edition**
(https://www.jetbrains.com/idea/download/). It understands Maven and Spring Boot
out of the box.

### Later phases — just be aware (do NOT install yet)
- **Node.js + Angular CLI** → for the `fih-admin` web app.
- **Flutter SDK** → for the `fih-verifier` mobile app.

---

## Step 1 — Create the workspace

Pick a parent folder, then:

```powershell
mkdir fih-companion
cd fih-companion
mkdir fih-companion-api, fih-admin, fih-verifier, db, docs
```

- `fih-companion-api/` — the Spring Boot backend (this phase).
- `fih-admin/` — Angular admin app (later).
- `fih-verifier/` — Flutter verifier (later).
- `db/` — SQL scripts (the read-only role, helper queries).
- `docs/` — notes, this guide, the future data dictionary.

> The provided ZIP already contains this whole structure with all Phase 1 files,
> so you can simply unzip it instead of recreating it by hand.

**Success looks like:** the five folders exist next to a `README.md`.

---

## Step 2 — Make a SAFE local copy of the database

**All development happens against a LOCAL restore of the backup — never the real
shared database.** Pointing at the real DB is a config-only change in a later
phase.

Put `billeterie-fih-2025.backup` somewhere easy, e.g. `C:\fih\`.

### 2a. Create an empty local database
```powershell
createdb -U postgres billeterie_fih_dev
```
(It will prompt for the postgres password you set during install.)

### 2b. Restore the custom-format dump
The backup is a **custom-format** dump, so you must use `pg_restore`, **not**
`psql`.

```powershell
pg_restore -U postgres -d billeterie_fih_dev --no-owner --no-privileges "C:\fih\billeterie-fih-2025.backup"
```

**Warnings you can safely ignore:** restoring this old (PG 9.5) dump into PG 16
prints exactly one harmless message:

```
pg_restore: error: could not execute query: ERROR:  schema "public" already exists
```

This is normal — PG 16 already has a `public` schema, so re-creating it is
skipped. The data still loads. (`--no-owner --no-privileges` also avoids noise
about the original `postgres` ownership from the source server.)

### 2c. Sanity-check the data
```powershell
psql -U postgres -d billeterie_fih_dev -c "SELECT count(*) FROM evenement;"
psql -U postgres -d billeterie_fih_dev -c "SELECT count(*) FROM billet;"
psql -U postgres -d billeterie_fih_dev -c "SELECT count(*) FROM voucher;"
psql -U postgres -d billeterie_fih_dev -c "SELECT count(*) FROM tturnstile;"
```

**Success looks like (verified against your actual backup):**

| table        | rows   |
|--------------|--------|
| `evenement`  | 34     |
| `billet`     | 6,304  |
| `voucher`    | 23,147 |
| `tturnstile` | 22,195 |

The dump contains 28 tables total. Real event titles you'll see include
*RAGOUJ – Le Spectacle*, *La Nuit des Chefs*, *Naïka*, *Hafedh Khalifa*.

---

## Step 3 — Create the read-only role (the real safety net)

The script is at `db/01_create_readonly_role.sql`. Run it as the superuser
against your dev DB:

```powershell
psql -U postgres -d billeterie_fih_dev -f db\01_create_readonly_role.sql
```

What it does:
- creates login role `fih_ro` (change the password before any real use),
- grants `CONNECT` on the database, `USAGE` on schema `public`, `SELECT` on **all
  current tables**,
- sets **default privileges** so it also gets `SELECT` on tables created later,
- grants **nothing** that allows writing — no INSERT/UPDATE/DELETE, no sequence
  usage, no CREATE, no DDL.

### Why this matters
This is the heart of the whole design. Even a buggy line of application code
that tried to `INSERT` would be rejected by PostgreSQL, because the role it
connects with simply has no write privilege. Careful coding is good; an enforced
database role is a guarantee.

### Prove it (optional but reassuring)
```powershell
psql -U fih_ro -d billeterie_fih_dev -c "SELECT count(*) FROM evenement;"
psql -U fih_ro -d billeterie_fih_dev -c "INSERT INTO evenement(billet,ddate,titre,voucher,location) VALUES (false, CURRENT_DATE, 'HACK', false, 1);"
```
**Success looks like:** the SELECT returns 34; the INSERT fails with
`ERROR: permission denied for table evenement`. (Password for `fih_ro` is
`fih_ro_pwd` from the script.)

---

## Step 4 — Scaffold the Spring Boot backend

You have two options. **Both give the same result** — the provided ZIP already
contains option B fully built.

### Option A — generate it yourself with Spring Initializr (gets you `mvnw`)
Go to https://start.spring.io with:
- Project: **Maven**, Language: **Java**, Spring Boot: **3.3.x**
- Group: `com.fih`, Artifact: `fih-companion-api`, Packaging: **Jar**, Java: **17**
- Dependencies: **Spring Web**, **Spring Data JPA**, **PostgreSQL Driver**,
  **Lombok**, **Validation**

Click **Generate**, unzip into `fih-companion/fih-companion-api/`, then replace
its `pom.xml`, `application.properties` (delete it; use the provided
`application.yml`), and add the Java files from the ZIP.

### Option B — use the provided files directly
The ZIP already contains the complete `fih-companion-api/` with this layout:

```
fih-companion-api/
├── pom.xml
└── src/main/
    ├── java/com/fih/companion/
    │   ├── FihCompanionApiApplication.java   # @SpringBootApplication entry point
    │   ├── evenement/
    │   │   ├── Evenement.java                 # entity mapped to "evenement"
    │   │   ├── EvenementRepository.java       # Spring Data repo
    │   │   ├── EventDto.java                  # API shape (record)
    │   │   ├── EventMapper.java               # entity -> DTO
    │   │   └── EventController.java           # GET /api/events
    │   └── diagnostics/
    │       └── DiagnosticsController.java     # GET /api/diagnostics/db
    └── resources/
        └── application.yml                    # connection + read-only config
```

The five chosen dependencies are exactly: Spring Web, Spring Data JPA,
PostgreSQL Driver, Lombok, Validation.

---

## Step 5 — Read-only configuration (already in `application.yml`)

Each property, one line:

- `spring.datasource.url` — points at the **local** `billeterie_fih_dev`
  (overridable via `FIH_DB_URL`).
- `spring.datasource.username` / `password` — connect as **`fih_ro`**, not as
  postgres (overridable via `FIH_DB_USER` / `FIH_DB_PASSWORD`).
- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate **checks** that the
  schema matches the entities but **never changes** it (no create/alter/drop).
- `spring.jpa.open-in-view: false` — closes the persistence context after the
  service call, not at HTTP-response time; cleaner and avoids lazy surprises.
- `spring.datasource.hikari.read-only` — left **false** in Phase 1 so the
  diagnostic write test reaches the DB and you can watch the *role* reject it;
  flip to `true` later for an extra JDBC-layer guard.
- `fih.photos.folder` — placeholder for later phases (badge photos).

Plus the **transaction posture**: read endpoints use
`@Transactional(readOnly = true)` (see `EventController`). That's the
application-layer expression of "read-only"; the `fih_ro` role is the hard wall.

> `ddl-auto: validate` means: on startup Hibernate compares your `@Entity`
> classes to the real tables. If a column you mapped doesn't exist, the app
> **fails to start** — which is how you know your mapping is correct. It will
> never modify the database.

---

## Step 6 — The one mapped entity: `Evenement`

We map only `evenement` in Phase 1 to prove the approach. Verified column shape:

| column      | type    | Java field            |
|-------------|---------|-----------------------|
| `reference` | integer | `Integer reference` (PK) |
| `titre`     | varchar | `String titre`        |
| `ddate`     | date    | `LocalDate ddate`     |
| `billet`    | boolean | `boolean billet`      |
| `voucher`   | boolean | `boolean voucher`     |
| `location`  | integer | `Integer location` (plain FK for now) |

Key annotations (full explanation in the entity's comments):
- `@Immutable` — Hibernate never issues UPDATEs for this entity.
- `@Column(insertable = false, updatable = false)` — these columns are excluded
  from any INSERT/UPDATE Hibernate might otherwise build.
- French column names are preserved; only the API DTO uses friendlier names.

The flow is: `EvenementRepository.findAll()` → `EventMapper.toDto()` →
`EventController` returns `List<EventDto>` at `GET /api/events`.

---

## Step 7 — The diagnostic endpoint

`GET /api/diagnostics/db` returns JSON with:
- `connectionAlive` — is the DB reachable,
- `databaseUser` — should be `fih_ro`,
- `evenementRowCount` — a real read (34),
- `writeBlocked` + `writeAttempt` — it runs an intentional `INSERT`, catches the
  failure, and reports the database's own error message as proof.

**How to read it:** if `databaseUser` is `fih_ro`, `evenementRowCount` is 34, and
`writeBlocked` is `true` with a "permission denied" message, the safety net is
confirmed working end to end.

---

## Step 8 — Run and verify

```powershell
cd fih-companion-api
mvn spring-boot:run
```
(First run downloads dependencies and takes a minute. If you scaffolded via
Initializr you can also use `.\mvnw spring-boot:run`.)

Then, in a second PowerShell window:

```powershell
curl http://localhost:8080/api/events
curl http://localhost:8080/api/diagnostics/db
```
(Or just paste those URLs into your browser.)

### Acceptance checklist
- [ ] App starts with **no schema errors** (proves `ddl-auto: validate` matched
      the real schema).
- [ ] `GET /api/events` returns **34** events with real titles.
- [ ] `GET /api/diagnostics/db` shows user **`fih_ro`**, count **34**, and
      `writeBlocked: true` with `permission denied for table evenement`.
- [ ] No row was ever inserted/updated/deleted (the write attempt always fails).

If the app fails to start with a message like *"Schema-validation: missing
table"* or *"wrong column type"*, that means an entity field doesn't match the
real table — re-check Step 6. That failure is `validate` doing its job.

---

## What Phase 2 will add (preview only — do not build it now)

Phase 2 models the rest of the schema properly: the real `Location`
relationship as a `@ManyToOne`, the **composite-key** tables (`generation` and
the ticket tables that reference it), `billet`, `voucher`, `tturnstile`,
`modelebillet` (including its serialized `access` blob), and the supporting
lookup tables — all still strictly read-only. With the full model in place,
later phases layer on statistics, badge-PDF generation, and the Flutter
verifier. Phase 1's job was only to prove the plumbing and the safety net, which
it now does.
