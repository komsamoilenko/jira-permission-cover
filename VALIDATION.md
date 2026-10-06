# Validation record

Updated: 2026-10-06.

## Executed locally

- Initial checks on 2026-10-05: Groovy 3.0.25, Java 17, Windows.
- Repeated on 2026-10-06 under both Groovy 3.0.25 and 4.0.8 with Java 17.0.20.1, Windows: all checks below passed on each version. Rebuilds in a separate verification copy produced the same distribution SHA-256; the repository's `src/` and `dist/` were not edited.
- 29 behaviour tests in `tests/run.groovy`, including 150 deterministic overlapping-set fixtures inside one test.
- Generated single-file entry point compiled and executed with fictional API service doubles; defaults block execution until configured, and configured fictional input returns a redacted exact snapshot.
- Standalone fictional demo executed without Jira.
- Distribution rebuilt from the reviewed entry point and planner sources.

These are offline checks of the repository's code. They are not tests against Jira's actual runtime, installed apps, directory services, caches or ScriptRunner compilation settings.

## Independent model review

The author used AI assistance and requested independent passes from three models, followed by fixes and follow-up review:

- GPT-6 Astra: Jira API contracts, read-only behaviour, privacy, failure modes.
- GPT-6 Sol: set-cover correctness, tests, adversarial cases and computational cost.
- GPT-6 Luna: documentation, reproducibility, confidentiality and release claims.

Concrete changes prompted by review:

1. An empty effective holder set previously returned too early, hiding a nonempty granted-group union. A failing regression was added; union equality now precedes the empty-scope result.
2. Repeated greedy rescans could perform excessive work on highly overlapping groups. A failing adversarial regression was added; aggregate-membership and computation budgets now stop without returning a partial plan.
3. Unexpected null boolean results now block instead of being treated as false.
4. Documentation now explicitly says that a reduction is not guaranteed and explains copying the entire assembled file into the Script Console.

Model review is not a security certification, a human approval of changes to Jira, or live integration validation. Test results are the executable evidence for the behaviours they cover.

## Automated checks

The GitHub Actions workflow is configured to run the offline tests on Groovy 3.0.25 and 4.0.8 with Java 17. Each matrix entry rebuilds the distribution, rejects generated-file differences, checks the assembled entry point and runs the demo. The corresponding test/build commands passed locally on both versions as described above; the workflow has not yet run on GitHub Actions. Consult the Actions results for remote CI evidence after publication.

Official Actions are pinned by commit SHA. Groovy core downloads are pinned by version and SHA-256:

```text
3.0.25  ad009e985dd84e4f524f4ed1751866da5bef816b691851bfbcefa48a01180a07
4.0.8   dcb861e7f7b048a7eeecdec27cc02917abc8543cdfbbc8ed3d3aba6e7ffcb2b9
```

The 4.0.8 artifact was independently downloaded from Maven Central on 2026-10-06 before adding its pin. Its computed SHA-1 matched Maven Central's published `.sha1`, and its computed SHA-256 matched the value above. No Jira connection was used for these local checks.

## Live check (2026-10-06)

This section records the author's sanitised work-laptop report, produced with AI assistance. The live runs were not repeated on the repository-preparation machine. Real keys, identities, counts, addresses, raw outputs and configured-copy hashes are deliberately excluded.

- Baseline: `dist/plan-permission-cover.groovy` from commit `6e8cff38f3deda21677856a954cfe2c18ff66cdc`, SHA-256 `73EB56261BDD45E2C374F76724183969FDE22C6150D9F6805C36B6CD4BFE67A7`. The local repository file was checked against this hash when reviewing the report.
- Environment: non-production Jira Data Center 9.12.8 (clustered), ScriptRunner 8.18.0, Groovy 4.0.8, Java 17.0.7.
- Execution: the complete file was submitted to the Script Console execution endpoint. The editor UI was not opened. Only the permission key and acknowledgement changed in configured copies; logic, limits and hidden-name settings stayed unchanged.
- A separate read-only probe confirmed the required Jira API signatures by reflection before the runs.
- A, default configuration: `BLOCKED / CONFIGURE_PERMISSION_KEY`.
- B, real app permission key with acknowledgement false: `BLOCKED / APP_SCOPE_NOT_CONFIRMED`.
- C, acknowledgement true: one run returned a non-empty `EXACT_SNAPSHOT`, `lostCount = 0`, `gainedCount = 0`, `readOnly = true` and `atomicSnapshot = false`.
- Independent REST-based check: separate Python code used full pagination and stable user keys, deriving its scope from active members of application-role groups. Within that scope, the union of selected groups equalled the union of all granted groups. The holder count agreed with C. This did **not** independently re-evaluate `hasAnyRole` or `hasPermission` for every user. The REST reads followed C by 1–3 minutes and used the same underlying Jira data.
- Recorded before/after grant-row snapshots were identical. The report records no permission, group or membership changes. The helper query had a 50,000-row cap; snapshot completeness cannot be re-established from the sanitised evidence alone.
- Additional runs with the same logic: a second app permission returned an independently confirmed non-empty `EXACT_SNAPSHOT`; a missing granted group produced `BLOCKED / UNRESOLVED_GROUP`; an uninstalled app's grants produced `BLOCKED / UNKNOWN_PERMISSION`; a built-in key produced `BLOCKED / BUILT_IN_PERMISSION`.
- The work-laptop report also includes successful offline tests, entry-point and demo checks on Groovy 4.0.8 / Java 11.0.11. These are distinct from the live Java 17.0.7 run and the repository-preparation machine's Java 17 checks.

Reported outcome: `RUNTIME PASS`, `INDEPENDENT_CHECK PASS`, overall `PASS` under the handoff plan. This is evidence for this file and configuration only, not approval to change grants or a general support guarantee.

## Not verified

Only the single live configuration above has been checked. Other Jira builds (including production), ScriptRunner/runtime combinations and editor static type-check warnings remain unverified. Live `OBSERVATIONS_CHANGED`, size/work-limit and `READ_OR_API_ERROR` paths were not exercised; they have offline coverage only. Server-side load was not measured.

No permissions have been changed using this repository. No deletion, rollback or remediation operation is implemented. Two matching observations do not prove atomicity or fresh directory caches; greedy selection does not prove minimality. Any change to the assembled script requires a new live check. See README and SECURITY for the supported scope and exclusions.
