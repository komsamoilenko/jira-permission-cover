# Security and scope

This is an experimental read-only analyser, not an access-control or approval mechanism. Do not use it for privileged/admin permission changes. No deletion or remediation code is included.

The normal output hides real group and user identities. It still contains counts; do not assume those are safe to publish. Enabling group names is intended only for your authorised local investigation. The host may retain Script Console history. The script does not make external requests or write a report file.

Do not put secrets, employer data, real group/permission keys, user identities, internal URLs or full console output in public issues. A synthetic reproduction is preferred. If your account and the repository expose GitHub's private vulnerability-reporting flow, use it for sensitive findings; otherwise start with a non-sensitive description and request a private channel without disclosing the sensitive data.

No supported-production-version matrix or security-response SLA is offered. Validate the exact file on a representative test deployment before considering live use. Two matching reads are not a transaction or cache-freshness guarantee.
