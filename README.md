# Jira permission cover

A read-only Groovy example for investigating overlapping **app-provided global permission grants** in Jira Data Center. It looks for a smaller combination of already-granted groups that covers the same current users within an explicitly limited scope. It may select every group; a reduction is not guaranteed.

**This is an analysis tool, not a permission-removal tool. `EXACT_SNAPSHOT` is not approval to delete anything.** No write, SQL, REST, cache-flush or membership-change operation is included.

## Why it exists

A large group can overlap several smaller groups without covering everybody in them. Keeping the two biggest groups is not a reliable way to preserve a permission.

This example uses a greedy set-cover calculation: choose the existing group covering the most people still uncovered, then repeat. It checks exact set equality rather than relying on matching headcounts. It does **not** prove the smallest possible solution.

The repository is a new, AI-assisted educational implementation of this approach. It includes no permission-removal or remediation code. All included examples use fictional identities.

## Status and compatibility

The [validation record](VALIDATION.md) distinguishes offline tests, the reported live check, independent model reviews and remaining limits.

- **Experimental; live-checked on one configuration.** The 2026-10-06 work-laptop report records a successful check on non-production Jira Data Center 9.12.8 + ScriptRunner 8.18.0 (Groovy 4.0.8, Java 17.0.7). It applies to the assembled file with SHA-256 `73EB56261BDD45E2C374F76724183969FDE22C6150D9F6805C36B6CD4BFE67A7`, with only the permission key and acknowledgement configured locally. Other live configurations are untested; a changed script needs a new live check. See [VALIDATION.md](VALIDATION.md) for the results and the independent comparison's limits.
- Designed against Jira Data Center 9.x APIs; official references below identify the precise documentation versions reviewed.
- Offline behaviour tests run on Groovy 3.0.25 and 4.0.8 with Java 17. They exercise the actual implementation and controlled service doubles. They do not validate Jira's caches, directories, licensing configuration or installed apps.
- Not for Jira Cloud, project permissions, issue-security levels, admin grants or application-access management.

## Use

1. Read the scope and limitations below. Review the script before executing it. Start on a representative non-production instance; this version has been live-checked only on the configuration listed above.
2. Open [`dist/plan-permission-cover.groovy`](dist/plan-permission-cover.groovy), the assembled single-file script.
3. In your **local copy only**, replace `REPLACE_WITH_APP_PERMISSION_KEY` with the exact key of one installed, non-administrative app's global permission. Confirm the app uses ordinary Jira group-grant semantics. Do not commit that configured copy.
4. Set `APP_PERMISSION_CONFIRMED = true` only after that review. Built-in Jira permission keys are blocked separately.
5. Copy the **entire configured file** into the editor in **ScriptRunner Script Console**, then run it. No server-side file installation is required. It makes two full read passes. Choose a quiet period: read-only does not mean cost-free. The configured caps stop analysis rather than truncate results, but an API may already have fetched a large collection before its size can be checked.
6. Keep `SHOW_GROUP_NAMES = false` for redacted group labels. To see the actual group names for a local investigation, explicitly set it to `true`. Never post real outputs, including counts, without reviewing their sensitivity.

You do not need to install the two source files into a ScriptRunner script root. The `dist` file contains both the entry point and the planner.

## What it checks

- The permission descriptor exists, and both raw grant records and named-group lookup agree.
- No anonymous grant or effective anonymous access is present.
- Every granted group and returned user identity resolves. Null/error results are not silently converted to empty groups.
- The target population consists of active `ApplicationUser`s for whom `hasAnyRole` is true and `hasPermission` is true. `hasAnyRole` means an application role backed by a licence, potentially with exceeded limits; it is not a login or spare-seat check.
- The union of **all** granted-group members in that scope equals the effective holder set. A mismatch blocks analysis as unsupported/inconsistent data.
- Two materialised observations agree on known/scoped user keys, effective holders and each group's scoped membership. A changed observation blocks the result. An observed addition or removal of any known user key, including an out-of-scope account, produces `OBSERVATIONS_CHANGED`.
- The selected groups cover the target exactly; selection and tie-breaking are deterministic.
- `MAX_MEMBERSHIP_CHECKS` bounds the aggregate memberships materialised per observation and the greedy calculation's membership-check budget (default 2,000,000). Exceeding either budget blocks the result without returning a partial plan. This is an operation cap, not a wall-clock timeout on Jira API calls.
- `MAX_USERS` counts all users returned by `getAllApplicationUsers`, before filtering by active status or application role, not just the scoped holders.

Identity comparison uses stable Jira user keys. No usernames or user keys appear in normal output. Internally collected snapshots remain in memory for the duration of the console run; the script itself does not save or transmit them. The host application may retain console execution/output history.

## Reading the output

| Status | Meaning |
| --- | --- |
| `EXACT_SNAPSHOT` | Selected groups match the observed target set. Review only; not a deletion recommendation. |
| `EMPTY_SCOPE` | No scoped holders. No candidate removal list is returned. Out-of-scope accounts can still matter. |
| `UNSUPPORTED_COVERAGE` | The full group union and effective holder set disagree, or coverage could not be completed. No candidate removal list is returned. |
| `BLOCKED` | Configuration, incomplete data, anonymous access, size limits, API failure or changing observations prevented analysis. |

`selectedGroups` is the combination found, not a proven minimum. `unselectedGroups` means omitted from that combination, **not safe to delete**. When redacted, `Group 1`, `Group 2`, etc. refer to the sorted group list within this run; labels can change between runs.

`selectedGroups` lists groups in greedy selection order, not by label number.

`lostCount` and `gainedCount` describe set differences within the scope. They are not predictions of every effect a real permission change would have.

Common blocking reasons include `CONFIGURE_PERMISSION_KEY`, `APP_SCOPE_NOT_CONFIRMED`, `BUILT_IN_PERMISSION`, `UNKNOWN_PERMISSION`, `ANONYMOUS_ACCESS`, `ANONYMOUS_OR_EMPTY_GRANT`, `UNRESOLVED_GROUP`, `UNRESOLVED_USER`, `GRANT_READS_DISAGREE`, `OBSERVATIONS_CHANGED`, `USER_LIMIT`, `GROUP_LIMIT`, `MEMBERSHIP_LIMIT`, `TOTAL_MEMBERSHIP_LIMIT`, `WORK_BUDGET`, `INVALID_BOOLEAN_RESULT` and `READ_OR_API_ERROR`. For an API error, diagnose locally; raw exception messages are deliberately not exposed because they can contain private data.

Unexpected Java `Exception`s caught inside `PermissionCover.run` become `READ_OR_API_ERROR`. JVM `Error`s (for example, `NoClassDefFoundError`) and failures before that protected block are not covered by this redaction and may expose diagnostic text in the console. Review all output before sharing it.

## What an exact result does NOT establish

- Preservation for inactive accounts, unlicensed users, portal-only customers or any other excluded accounts.
- Preservation after somebody joins/leaves a group or gains/loses application access.
- Atomicity: two equal reads do not eliminate races, an intermediate change that is later reversed, or stale caches. This tool does not lock Jira or clear its caches.
- The future organisational meaning of a group, app-specific behaviour, directory-policy correctness, or ownership approval.
- Safety of deleting groups. This tool only reasons about one global permission's group assignments, and changes neither.

Before any real change, the permission owner must review **all** affected populations and future access policy, obtain appropriate approval, plan recovery and perform fresh validation. This repository intentionally does not implement that change workflow.

## Fictional example

| Already-granted group | Members |
| --- | --- |
| alpha | u1, u2, u3 |
| beta | u3, u4 |
| gamma | u4, u5 |
| delta | u6 |

The target is u1–u6. `alpha` and `beta` miss u5 and u6. The planner chooses `alpha`, `gamma`, `delta`; the selected union is the same six identities. These are illustrative numbers, not production data.

Run `groovy examples/demo.groovy` from the repository root to calculate this example without Jira.

## Develop and test

With Java 17, run the following from the repository root under both Groovy 3.0.25 and 4.0.8:

```text
groovy tests/run.groovy
groovy tools/build.groovy
groovy tests/entrypoint.groovy
git diff --exit-code -- dist/plan-permission-cover.groovy
```

The core has no external dependencies beyond Groovy/JDK. Tests cover exact coverage, overlap, deterministic ties, non-minimal greedy results, absent/extra holders, empty scope, null input, redaction, limits, an adversarial high-overlap computation budget, invalid boolean results and fail-closed reads. An additional test compiles and executes the assembled console file against small API doubles; that is still not a live Jira integration test.

Edit `src/PermissionCover.groovy` or `scripts/console-entrypoint.groovy`, not the generated `dist` copy. Rebuild and rerun the full suite. The build task writes only the local distribution file and never contacts Jira.

Changes to the generated script require a new live check before claiming the live validation applies to that version.

## References

- [GlobalPermissionManager, Jira 9.12.17](https://docs.atlassian.com/software/jira/docs/api/9.12.17/com/atlassian/jira/security/GlobalPermissionManager.html)
- [GroupManager, Jira 9.4.1](https://docs.atlassian.com/software/jira/docs/api/9.4.1/com/atlassian/jira/security/groups/GroupManager.html)
- [UserManager, Jira 9.4.1](https://docs.atlassian.com/software/jira/docs/api/9.4.1/com/atlassian/jira/user/util/UserManager.html)
- [ApplicationRoleManager, Jira 9.12.1](https://docs.atlassian.com/software/jira/docs/api/9.12.1/com/atlassian/jira/application/ApplicationRoleManager.html)

These are API references, not proof of compatibility with your deployment. Version 9.12.1 pages for some types were unavailable during review, so the actual versions consulted are identified above.

## Licence and reporting

MIT; see [LICENSE](LICENSE). Independent community example, not an Atlassian or Adaptavist product.

See [SECURITY.md](SECURITY.md) before reporting a problem. Do not include real permission keys, group names, user details, console dumps, credentials or employer data in a public issue.
