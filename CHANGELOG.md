# Changelog

All notable changes to WorldAccessBlocker are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [0.12.0] — 2026-09-23

### Fixed

- **Folia forks (Arbor, Canvas, DeerFolia, Lophine, …) no longer fail to enable** ([#1](https://github.com/Etoryx/WorldAccessBlocker/issues/1)). Folia was detected by the server brand name, so forks fell through to the Bukkit scheduler and crashed with `UnsupportedOperationException` in `ElytraBlocker.start`. Detection now relies only on the presence of the regionised-server class. Thanks to @lijinhong11 ([#2](https://github.com/Etoryx/WorldAccessBlocker/pull/2)).
- Repeating global tasks on Folia no longer pass a zero initial delay, which the region scheduler rejects.

### Changed

- The Folia adapter calls the Paper scheduler API (`GlobalRegionScheduler`, `AsyncScheduler`, `EntityScheduler`) directly instead of through reflection, and no longer silently falls back to the Bukkit scheduler.
- Modrinth releases now include the year-based Minecraft versions (26.1, 26.2, …); the version filter only matched `1.x` before.
- Dependencies: bStats 3.1.0, JUnit 6.1.3, Mockito 5.23.0, Surefire 3.5.6. CI actions bumped to their current major versions.

---

## [0.11.0] — 2026-07-03

### Changed

- **Wider version support: Minecraft 1.20.1 → latest.** The plugin now compiles to Java 17 bytecode (was Java 21) against the Paper 1.20.1 API, so a single JAR loads on 1.20.1–1.20.4 (Java 17) servers *and* 1.20.5+ (Java 21) servers, up to the newest release. The 0.10.0 Java 21 / Paper 1.20.5+ baseline had locked out 1.20.1–1.20.4.
- Replaced the Java 21 `List.getFirst()` / `getLast()` calls (SequencedCollection API) with index access so the code runs on Java 17.
- Release workflow now builds the Modrinth game-version list dynamically — every `release` tag from 1.20 upward — instead of a hardcoded set, so new Minecraft versions are covered automatically. Falls back to a static list if the Modrinth tag API is unreachable.

---

## [0.10.0] — 2026-06-23

### Added

- `wab.status` permission (default: true) — players can now view restriction status without the op-level `wab.bypass` permission. `/wab bypass` and `/wab remove` still require `wab.bypass`.
- `PlatformRuntime.runLater(...)` — Folia-safe delayed task dispatch.
- Elytra **flight** enforcement now also stops players who are *already gliding* when a restriction window begins (previously only the start of a glide was cancelled).
- `config-version` field in `config.yml` with a startup warning when the config is older than the plugin expects.
- Per-key language fallback: missing keys in a custom/partial language file now fall back to `en.yml` instead of showing "Message not found".
- Unit tests: `BypassManagerTest` (grant/expiry/remove/persistence) and `LanguageManagerTest` (English fallback).

### Fixed

- **Folia: `BypassManager` no longer crashes.** Debounced save was using `Bukkit.getScheduler().runTaskLater()`, which throws `UnsupportedOperationException` on Folia. It now dispatches through `PlatformRuntime`.
- **Folia: thread safety.** `BypassManager` now uses `ConcurrentHashMap` (it is read/written from multiple region threads on Folia).
- **Folia: teleports** now use `teleportAsync` in the Folia adapter (required for cross-region/cross-world teleports).
- **Update notification color codes** — the `update_available` message now renders legacy `§` codes via `LegacyComponentSerializer` instead of showing them literally.
- **End return portal** — leaving the End is no longer blocked when the End is restricted; only entering it is.
- Legacy `recurring-allowed-days` migration for nether/end/elytra now reads the correct config path (was looking at the config root).

### Changed

- `WorldAccessBlocker.isRestricted(...)` / `BypassManager.isRestricted(...)` renamed to `hasNoBypass(...)` for clarity (the method reports whether the player has *no* active bypass).
- `plugin.yml`: `api-version` raised from `1.16` to `1.20` to match the actual Java 21 / Paper 1.20.5+ baseline; `/wab` command-level permission removed in favor of per-subcommand checks.
- Replaced deprecated `getDescription()` with `getPluginMeta()` and the deprecated `new URL(...)` with `URI.create(...).toURL()`.
- bStats charts renamed (`*_disabled` → `*_blocking`) so the reported value matches the chart name.
- bStats shaded into `net.denfry.worldAccessBlocker.libs.bstats` (dedicated sub-package).
- Release workflow now runs tests (removed `-DskipTests`); Modrinth metadata corrected to `paper`/`purpur`/`folia` loaders and the actually supported game versions.
- README: corrected the platform support table (Paper/Purpur/Folia; Spigot/Bukkit not supported) and the recurring-schedule explanation (periods are *open* windows).

---

## [0.9.0] — 2026-05-28

### Added

- **Version checker** — on startup the plugin asynchronously queries the Modrinth API and notifies operators with the `wab.update-notify` permission when a newer release is available. Notification appears once per join session.
- **`wab.update-notify` permission** — dedicated permission for update notifications, separate from `wab.reload`. Default: op.
- **Folia-native scheduler** (`PlatformRuntime`) — new `runtime/` abstraction layer dispatches tasks via the Folia regional scheduler on Folia servers and the standard Bukkit scheduler on Paper/Spigot. Eliminates cross-thread teleport warnings on Folia.
- **`WorldEntryBlocker` listener** — handles `PlayerTeleportEvent` and `PlayerChangedWorldEvent` to block custom-world entry on teleport, in addition to the periodic enforcer sweep.
- **CI/CD — build workflow** (`.github/workflows/build.yml`) — builds and runs all tests automatically on every push and pull request to `master`.
- **CI/CD — release workflow** (`.github/workflows/release.yml`) — on `v*` tag push: builds the JAR, creates a GitHub Release, and uploads to Modrinth automatically.

### Changed

- `VersionChecker` and `RestrictionEnforcer` now use `PlatformRuntime` instead of `Bukkit.getScheduler()` directly.
- Folia detection moved to `PlatformRuntimeFactory.isFoliaRuntime()` using class-presence check.
- Log message on Folia detection changed from a warning to an info message.

### Fixed

- `VersionChecker`: `latestVersion` field made `private volatile` — closes accidental external mutation.
- `VersionChecker`: error stream now properly drained and closed on non-200 HTTP responses, preventing socket leaks.
- `VersionChecker`: `parseSemver` strips pre-release labels (e.g. `-SNAPSHOT`) before parsing — avoids silent `null` on dev builds.

---

## [0.8.0] — 2025-XX-XX

### Added

- `/wab status [player|uuid]` command to view restriction status per player.
- PlaceholderAPI support (soft dependency). New placeholders:
  - `%wab_nether_blocked%`
  - `%wab_end_blocked%`
  - `%wab_elytra_blocked%`
  - `%wab_time_left_nether%`
  - `%wab_time_left_end%`
  - `%wab_time_left_elytra%`
- Per-feature and per-custom-world fallback spawn configuration (`fallback-spawns`).
- Unit tests for core restriction logic (`ConfigManagerTest`).

### Changed

- `/wab bypass` and `/wab remove` now support offline players and UUIDs.
- Bypass expiration timestamps now use the configured plugin timezone.
- Bypass persistence optimized with delayed save scheduling to reduce disk writes.
- Updated README with new commands, placeholders, and config examples.

### Fixed

- Fixed recurring behavior: `periods: []` now correctly means "always blocked".
- Fixed date parsing with configured timezone for `restriction-date`.
- Fixed edge cases and null-safety in restriction logic.
- Fixed potential crash in portal creation handling when block list is empty.
- Fixed teleport fallback handling when blocking Nether/End/custom world access.
- Fixed broken README encoding.

### Notes

- PlaceholderAPI integration is optional and auto-enabled when the plugin is installed.

---

## [0.7.x and earlier]

Initial releases — date-based Nether, End, and elytra restrictions with recurring schedule support.
