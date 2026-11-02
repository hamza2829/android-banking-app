# Verification record

Only executed JUnit XML testcases count as results. Source annotations and intended coverage do not count. CI retains failed runs and uploads available reports using `if: always()`.

## Development failures

- First local build failed before tests: Room's `Transaction` annotation collided with the domain `Transaction` type under star imports. Fixed with an explicit domain alias.
- Second local build failed before tests: Retrofit's older transitive OkHttp lacked the Kotlin `HttpUrl.toHttpUrl` extension. Fixed by explicitly aligning OkHttp and MockWebServer at 4.12.0.
- Local instrumentation could not start: the installed Windows emulator has no hardware acceleration driver. No host driver settings were changed. API 34 GitHub Actions is the emulator verification environment.
- First executed local JVM suite: **59 tests, 57 passed, 2 failed, 0 skipped**. Both loopback HTTP tests used MockWebServer's machine-resolved host, which the strict endpoint allowlist rejected on Windows. The fixtures now explicitly bind and address `127.0.0.1`; external hosts remain rejected.

## Executed results

- Local JVM rerun: **59 passed, 0 failed, 0 errors, 0 skipped**. Debug app and instrumentation APK compile successfully.
- [First CI run](https://github.com/hamza2829/android-banking-app/actions/runs/41438995817): **failed during SDK setup**, before any tests. The setup action's default package list requested Google's removed `tools` package. The workflow now requests only platform-tools, platform 35, and build-tools 35.0.0. CI counted **0 JVM and 0 instrumented tests**, rather than reusing local counts.

- [First emulator CI run](https://github.com/hamza2829/android-banking-app/actions/runs/41439122629): **59/59 JVM tests passed; 17/18 instrumented tests passed, 1 failed, 0 skipped**. The real BiometricPrompt appeared, but the test expected `pinEntry`/`password_entry`; API 34's logcat identified its field as `lockPassword`. Added that system resource ID and explicit navigation after resuming. The screenshot test passed, but AGP's `uninstall_after_test: true` deleted its app-private files; the test now exports synthetic screenshots to the emulator Downloads folder before teardown and verifies the files exist. This run failed and had no screenshot artifact.
- Offline merged-release-manifest check passed: no INTERNET permission is present.

Final CI results and emulator screenshots are pending.
