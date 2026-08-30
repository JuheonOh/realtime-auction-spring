# Security Policy

## Supported version

Security fixes are applied to the `main` branch. Older revisions are not supported.

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability or exposed credential. Use GitHub's private vulnerability reporting for this repository and include:

- affected commit and component
- reproduction steps or proof of concept
- expected impact
- suggested remediation, when available

Do not access data that is not yours, disrupt a running service, or retain copied credentials or personal data.

## Response process

Reports are triaged by severity and reproducibility. Confirmed credential exposure is handled by rotating the credential first, then removing it from source and history. Confirmed application vulnerabilities are fixed with regression coverage before disclosure is coordinated.
