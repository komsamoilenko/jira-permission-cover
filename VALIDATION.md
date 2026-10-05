# Validation record

Date: 2026-10-05.

## Executed locally

- Groovy 3.0.25, Java 17, Windows.
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

The GitHub Actions workflow reruns the offline tests, rebuilds the distribution, rejects uncommitted generated-file differences, checks the assembled entry point and runs the demo. Consult the repository's Actions tab for the result on a particular commit.

Official Actions are pinned by commit SHA. The downloaded Groovy core is pinned to 3.0.25 and checked against SHA-256:

```text
ad009e985dd84e4f524f4ed1751866da5bef816b691851bfbcefa48a01180a07
```

## Not verified

No current public version has been run against live Jira/ScriptRunner. No permissions have been changed using this repository. No deletion, rollback or remediation operation is implemented. Two matching observations do not prove atomicity or fresh directory caches. See README and SECURITY for the supported scope and exclusions.
