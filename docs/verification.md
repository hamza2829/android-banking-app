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

## Final CI result

[Run 41440307570](https://github.com/hamza2829/android-banking-app/actions/runs/41440307570) **completed successfully** on 2 November 2026 for code commit `bd92182aac57989af3d75d11ecb7591dc44e98d6` (Java 17; Android API 34, google_apis x86_64, Pixel 6 profile).

| Suite | Executed | Passed | Failed | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| JVM | 59 | 59 | 0 | 0 | 0 |
| Instrumented | 18 | 18 | 0 | 0 | 0 |
| Total | 77 | 77 | 0 | 0 | 0 |

The JVM cases cover IBAN (17), amount parsing (18), category aggregation (6), transfer ViewModel (8), account ViewModel (2), lock ViewModel (4), and loopback Retrofit (4). Instrumented cases cover Room and loopback import (9), Compose forms/list/details (6), the actual system device-credential flow (1), encrypted token persistence (1), and three real screen captures in one screenshot test (1).

Artifacts verified present and unexpired at delivery: `debug-apk` (16,548,359-byte artifact archive), `test-reports` (345,535 bytes), and `screenshots-api34` (451,290 bytes). The summary step passed with exactly `overview.png`, `spending.png`, and `transfer.png`. The PNGs were downloaded without image edits, visually inspected, and committed under [screenshots](screenshots/). [Saved JSON](ci-results.json) and [Markdown](ci-results.md) are copies of the run's report summary. Full XML/HTML reports remain in the CI artifact.

This verifies a debug build and the listed tests. It is not a production security review or an independent audit. No real service was contacted by the app. Fingerprint/face hardware success was not tested. Later README/docs-only commits do not alter the tested application or workflow.
