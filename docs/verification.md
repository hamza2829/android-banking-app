# Verification record

Only executed JUnit XML testcases count as results. Source annotations and intended coverage do not count. CI retains failed runs and uploads available reports using `if: always()`.

## Development failures

- First local build failed before tests: Room's `Transaction` annotation collided with the domain `Transaction` type under star imports. Fixed with an explicit domain alias.
- Second local build failed before tests: Retrofit's older transitive OkHttp lacked the Kotlin `HttpUrl.toHttpUrl` extension. Fixed by explicitly aligning OkHttp and MockWebServer at 4.12.0.
- Local instrumentation could not start: the installed Windows emulator has no hardware acceleration driver. No host driver settings were changed. API 34 GitHub Actions is the emulator verification environment.
- First executed local JVM suite: **59 tests, 57 passed, 2 failed, 0 skipped**. Both loopback HTTP tests used MockWebServer's machine-resolved host, which the strict endpoint allowlist rejected on Windows. The fixtures now explicitly bind and address `127.0.0.1`; external hosts remain rejected.

Executed results and CI run links will be added after verification.
