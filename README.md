# Two Environments, One Jar

A Java 21 / Spring Boot 4.0.9 enrollment demonstration. Select `dev` or `prod` at runtime; both drivers, all configuration files, and Flyway migrations are inside the **same executable jar**. There are no Maven environment profiles and no source edits between environments.

A prebuilt Java 21 jar is included at `target/two-environments-one-jar-1.0.0.jar`. Its digest is in `application.jar.sha256`. You can run it immediately, or build the source below.

## Requirements and build

Install JDK **21**, Python 3 (acceptance script), and Docker or a PostgreSQL 17 server for prod. Maven Wrapper is included for Linux/macOS and Windows. The initial build needs internet access. The build enforces JDK 21.

```bash
./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Output: `target/two-environments-one-jar-1.0.0.jar`. Tests cover binding and invalid configuration, service rules with non-default values, Flyway seeds, HTTP Actuator restrictions, and the absence of numeric business literals in EnrollmentService.

## Dev: H2 with sample data

```bash
java -jar target/two-environments-one-jar-1.0.0.jar --spring.profiles.active=dev
```

H2 is in memory and resets when the process exits. No database installation, username, or password is required. Flyway runs common V1 and dev-only V2. SQL output is enabled; Hibernate validates the Flyway schema and never generates it. The sample registration window is relative to startup, so the demo does not expire on a fixed calendar date. The H2 browser console is not enabled.

Open [students](http://localhost:8080/api/students), [sections](http://localhost:8080/api/sections), and [health](http://localhost:8080/actuator/health).

Create an enrollment using Postman: `POST http://localhost:8080/api/enrollments`, `Content-Type: application/json`, body:

```json
{"studentId": 1, "sectionId": 1}
```

Expected: 201 with `ENROLLED`. Student 2 in section 1 receives `WAITLISTED`; repeating student 1 receives 409. There are three sample students and two sections. Use section 2 for additional seats. This assignment is a configuration demo without user authentication; student IDs are supplied by the caller.

## Prod: PostgreSQL

Create a PostgreSQL database and an application role with schema creation privileges for Flyway. Supply credentials through your terminal environment or deployment secret manager. Do not put values in source, shell scripts, `.env` files, command-line arguments, or commits.

Required environment variables (all four must be set):

| Variable | Meaning | Example / format |
| --- | --- | --- |
| `DB_URL` | JDBC connection URL; do not embed credentials | `jdbc:postgresql://localhost:5432/enrollment` |
| `DB_USERNAME` | PostgreSQL application role | Provided at runtime |
| `DB_PASSWORD` | Password for that role | Provided at runtime |
| `REGISTRAR_CONTACT_ADDRESS` | Registrar email for enrollment failures | Your registrar email address |

For a remote database use an appropriate TLS JDBC URL, such as `?sslmode=verify-full`, with a trusted server certificate.

Bash (interactive prompts keep values out of command history):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/enrollment
read -r -p 'Database username: ' DB_USERNAME
export DB_USERNAME
read -r -s -p 'Database password: ' DB_PASSWORD
export DB_PASSWORD
printf '\n'
read -r -p 'Registrar email: ' REGISTRAR_CONTACT_ADDRESS
export REGISTRAR_CONTACT_ADDRESS
java -jar target/two-environments-one-jar-1.0.0.jar --spring.profiles.active=prod
```

Windows PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/enrollment'
$env:DB_USERNAME = Read-Host 'Database username'
$secureValue = Read-Host 'Database password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secureValue).Password
$env:REGISTRAR_CONTACT_ADDRESS = Read-Host 'Registrar email'
java -jar target/two-environments-one-jar-1.0.0.jar --spring.profiles.active=prod
```

For a disposable Docker database, after setting the environment variables above:

```bash
docker run -d --name enrollment-postgres -p 127.0.0.1:5432:5432 \
  -e POSTGRES_DB=enrollment -e POSTGRES_USER="$DB_USERNAME" -e POSTGRES_PASSWORD="$DB_PASSWORD" postgres:17
```

PowerShell equivalent:

```powershell
docker run -d --name enrollment-postgres -p 127.0.0.1:5432:5432 -e POSTGRES_DB=enrollment -e "POSTGRES_USER=$env:DB_USERNAME" -e "POSTGRES_PASSWORD=$env:DB_PASSWORD" postgres:17
```

Wait for PostgreSQL to accept connections before starting the jar. This disposable database has no named volume; use your managed PostgreSQL server for durable production data. `docker rm -f enrollment-postgres` removes the disposable database.

Prod runs common V1 only and starts with empty students/sections; sample data is never loaded. SQL output is disabled. Creating student/section management endpoints is outside this assignment; populate real data through your administration process. Never activate `dev` and `prod` together. Both modes use the same jar path; **do not build again**.

## Registration configuration

`RegistrationRules` is a validated `@ConfigurationProperties` record with prefix `registration.rules`. Constructor injection supplies it to EnrollmentService. Every configurable business rule, including error contact information, comes from this record.

| Property under `registration.rules` | Environment override | Default | Validation |
| --- | --- | --- | --- |
| `max-credit-hours-per-term` | `REGISTRATION_MAX_CREDIT_HOURS_PER_TERM` | 18 | 1–30 |
| `waitlist-size` | `REGISTRATION_WAITLIST_SIZE` | 10 | 0–1000; zero disables waitlisting |
| `registration-window-grace-period` | `REGISTRATION_WINDOW_GRACE_PERIOD` | `PT24H` | Duration from zero through 30 days |
| `registrar-contact-address` | `REGISTRAR_CONTACT_ADDRESS` | `registrar@example.edu` in dev; required in prod | Nonblank email |

The grace period extends the registration closing instant, inclusively. UTC instants avoid server-timezone differences. Enrollment and waitlist reservations both count toward a student's term credit limit. Transactions lock the student before the section, preventing simultaneous requests from bypassing credit or seat/waitlist limits. Section capacity and course credits are stored data, not configuration constants.

Negative startup test:

```bash
java -jar target/two-environments-one-jar-1.0.0.jar --spring.profiles.active=dev --registration.rules.waitlist-size=-1
```

Expected: nonzero process exit and a validation failure identifying `registration.rules.waitlist-size`. No request is needed to trigger validation. Duration and email errors also prevent startup.

## Actuator and future broker

Only `/actuator/health`, `/actuator/info`, and `/actuator/metrics` (including metric names beneath it) are exposed. All other endpoint access defaults to `none`; JMX exposure is closed. `/actuator/env` and `/actuator/beans` return 404. Health details remain hidden on public HTTP; the database health contributor still participates in aggregate status and a failing database produces DOWN/503. `/actuator/info` contains only fixed application metadata.

No broker is required or contacted this week. In Week 13, add `spring-boot-starter-amqp` to the normal dependencies and provide RabbitMQ connection settings through environment variables. Boot then auto-configures RabbitMQ health when a connection factory exists; do not add a dummy UP indicator or expand the Actuator allowlist. Keep broker credentials out of files and test aggregate DOWN when the broker is unavailable.

## Prove one jar across both databases

Build once, set all required prod variables, and point them to a **fresh empty** PostgreSQL database. Stop any processes on ports 18081/18082, then:

```bash
python3 scripts/verify_jar.py
```

Windows: `python scripts/verify_jar.py`.

Without PostgreSQL, `python3 scripts/verify_jar.py --dev-only` verifies the packaged dev jar and negative startup test only; this does not satisfy the cross-environment criterion.

The script computes SHA-256, launches this exact path under dev, checks seeded H2/Actuator, stops it, launches the same path under prod, checks PostgreSQL/no sample data/Actuator, and compares the digest after each run. It also launches invalid configuration and requires startup failure naming the property. It invokes no build tool. An existing database with real students is deliberately rejected by the empty-database assertion; never run this acceptance check against real data.

## Pipeline and secret hygiene

`.github/workflows/ci.yml` runs on pushes and pull requests. It fetches complete Git history, runs Gitleaks against history and the working tree, then builds/tests on Java 21, starts PostgreSQL 17 with freshly generated credentials, and runs the same-jar acceptance script. A secret-scan failure blocks the build job. Configure `secret-scan` and `build-and-acceptance` as required branch checks in your repository settings. No GitHub remote is configured by this download; push the extracted project to your own repository to run the pipeline.

No real credential belongs in any file or commit. `.gitignore` excludes local environment files but ignoring them is not permission to store credentials there. Runtime CI database credentials are generated and masked. If a secret is ever committed, revoke/rotate it and remove it from all reachable history before proceeding; deleting the current file alone is insufficient. Local Gitleaks commands:

```bash
gitleaks git . --redact --exit-code 1
gitleaks dir . --redact --exit-code 1
```

See `VERIFICATION.md` for checks actually completed in the delivery environment.

## Framework references

- [Spring Boot 4 build systems](https://docs.spring.io/spring-boot/4.0/reference/using/build-systems.html)
- [Actuator access and exposure](https://docs.spring.io/spring-boot/4.0/reference/actuator/endpoints.html)
