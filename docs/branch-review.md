# Review: `update-to-current-toolchain`

Scope: the 18 commits on `update-to-current-toolchain` vs `master`. The branch
modernises the build (AGP 8.9.1, Gradle 8.11.1, `compileSdk 35`, `targetSdk 33`,
namespace, foreground service, notification channel, boot/network receivers) and
reorganises the `db` package back into the main package.

Overall the toolchain migration is sound and the app compiles against the new
SDK. The findings below are the outstanding functional issues; the first is a
user-facing regression introduced by this branch.

## Findings

### 1. (Blocker, regression) `SSLDroidDbAdapter.createContentValues` drops `cacertfile` and `usesni`

`createContentValues(...)` takes `cacertfile` and `usesni` parameters but never
writes them into the `ContentValues`:

- `master` persisted `cacertfile` (`values.put(KEY_CACERTFILE, cacertfile)`).
  This branch's refactor removed that line, so **CA-pinning configuration is
  silently lost** on save.
- The branch added a new `usesni` column (`usesni integer not null`) and a
  `usesni` parameter, but never persists it. So **the SNI checkbox is not saved**.
- Worse: on a **fresh** install the `tunnels.usesni` column is `NOT NULL` with no
  default (only upgraded DBs get `default 1` via `onUpgrade`). Inserting a row
  without `usesni` violates the constraint, `insert` returns `-1`, and
  `saveState()` treats that as failure — **tunnels cannot be saved at all on a
  clean install.**

There is also a duplicated `values.put(KEY_REMOTEPORT, remoteport)` line.

Fix: put `KEY_CACERTFILE` and `KEY_USE_SNI`, drop the duplicate. Covered by the
new regression test `SSLDroidDbAdapterTest.persistsCaCertFileAndSniFlag`.

### 2. (Minor) Handshake failure leaks the accepted client socket

In `TcpProxyServerThread.run()`, when the upstream TLS `createSocket` /
`startHandshake` throws `IOException`, the handler logs and `return`s without
closing the already-accepted client socket `sc`. The sibling `catch (Exception)`
does close `sc`. The client is then left with a half-open connection that only
resolves on its own timeout (this is what the negative e2e test observes).
Non-blocking for this task; noting for a follow-up.

### 3. (Minor) A single bad connection tears down the whole tunnel

Several error paths in the `accept()` loop `return` from `run()` instead of
`continue`-ing, so one failed upstream connection stops the listener for that
tunnel until the service restarts. Pre-existing behaviour, not introduced here.

### 4. (Cosmetic) Dead byte-scrubbing loop in `Relay.run()`

The `for` loop that rewrites byte `0x07` to `'#'` runs *after* the buffer has
already been written to `out`, so it has no effect — leftover from the original
`TcpTunnelGui`. Harmless; could be deleted.

### 5. (Housekeeping) IDE and local files committed

`.idea/` (incl. `workspace.xml`) and `local.properties` are tracked on this
branch. `local.properties` is machine-specific (`sdk.dir=/home/blint/...`) and
should not be in VCS. Recommend adding both to `.gitignore`.

## Testing added by this change

- `tests/java/hu/blint/ssldroid/TcpProxyE2ETest.java` — true end-to-end tunnel
  tests (cleartext client → real `TcpProxy` → real TLS backend), covering the
  no-pinning, correct-CA, and wrong-CA cases.
- `tests/java/hu/blint/ssldroid/SSLDroidDbAdapterTest.java` — CRUD + the
  regression test for finding #1.
- `.github/workflows/ci.yml` — runs the suite on GitHub Actions (free provider),
  entirely on the JVM via Robolectric (no emulator needed).

Note: because Robolectric instruments core classes, the unit-test JVM is started
with `--add-opens` for several `java.base` packages (configured in
`build.gradle`); without `java.base/java.net` opened, JDK 17 fails the real TLS
handshake in the e2e tests with an `InaccessibleObjectException`.
