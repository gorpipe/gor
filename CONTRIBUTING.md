# Contributing to GOR

For developers of GOR, people who want to contribute to the GOR codebase or documentation, or people who want to install from source and make local changes to their copy of GOR.

## Reporting an Issue
TBD

## Build
TBD

## Release

This section is for developers of GOR and describes how releases are created on [GitHub](https://github.com/gorpipe/gor).

### Versioning

We use [semantic versioning](https://semver.org), `<MAJOR>.<MINOR>.<PATCH>`, and release tags are `vX.Y.Z`.

There is no version file, the version is derived from git tags by the gradle
[git-versioning plugin](https://github.com/qoomon/gradle-git-versioning-plugin) (see `build.gradle`):

- A commit tagged `vX.Y.Z` builds version `X.Y.Z`.
- Any other commit builds `<latest vX.Y.Z tag>-SNAPSHOT`, e.g. `5.14.0-SNAPSHOT` is a development version on top of
  the released `5.14.0`.  Snapshots from `main` are published to GitHub Packages.
- `make gitversion` (or `./gradlew -q printVersion`) prints the current version, `-Pversion=...` overrides it.
- A shallow clone without tags falls back to `0.0.0-SNAPSHOT`, so CI checkouts that run gradle use `fetch-depth: 0`.

Pick the next version from the changes since the last release:

- Only fixes bump PATCH, e.g. `5.14.0` -> `5.14.1`.
- New features bump MINOR, e.g. `5.14.0` -> `5.15.0`.
- Breaking changes bump MAJOR, e.g. `5.14.0` -> `6.0.0`.

### Creating a Release

1. Make sure `main` contains everything that should go into the release and that the CI build on `main` is green.
2. Under [Releases](https://github.com/gorpipe/gor/releases), press **Draft a new release**.
3. Create a new tag `vX.Y.Z` on `main` (the tag must be strictly `vX.Y.Z`, other tags are ignored by the versioning)
   and use it as the release title.
4. Press **Generate release notes**, review them and press **Publish release**.
5. The tag push triggers the CI build (`.github/workflows/build.yml`), which runs all tests, checks that the gradle
   version matches the tag and publishes the release to GitHub Packages.

If the tag build fails, fix it on `main` and create a new release (the next patch version).

### Create a Release of GOR Services

To update our customer environments, we will need to create a new release of GOR Services including this GOR release.
That process is documented in the [GOR Services repo](https://github.com/GeneDx/gdb-gor-services/blob/main/CONTRIBUTING.md#release).
