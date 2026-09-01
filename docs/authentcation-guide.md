# NGGEEN Authentication — Phase Dependency Matrix

| Phase | Milestone | Key Deliverables | Prerequisites |
|------|-----------|------------------|---------------|
| **Phase 1** | Foundation | Argon2id password hashing, email verification, CAPTCHA (e.g. Turnstile), IP/user rate limiting | `User` table |
| **Phase 2** | Session security | Short-lived access JWT, rotating refresh tokens in `HttpOnly; Secure; SameSite` cookies, server-side session revoke, anti-phishing email code | Phase 1 |
| **Phase 3** | MFA | TOTP 2FA (RFC 6238), QR enrollment, `2FA_PENDING` login gate, hashed single-use backup codes | Phase 2 |
| **Phase 4** | Policy controls | Step-up auth for high-risk actions, 24h withdrawal lock after credential changes, withdrawal address allowlist + activation delay | Phase 3 |
| **Phase 5** | API key engine | HMAC-SHA256 and/or Ed25519 request signing, timestamp + `recvWindow`, scopes (`READ_ONLY`, `SPOT_TRADE`, `WITHDRAW`), optional IP allowlist | Phase 2 *(Phase 4 required before enabling `WITHDRAW` scope)* |
| **Phase 6** | Passkeys / WebAuthn | FIDO2 registration + assertion, passwordless and/or step-up via passkey | Phase 3 |
| **Phase 7** | Adaptive security | Device/session signals, geo/ASN anomaly challenges, security event alerts | Phase 2 *(Phase 5 optional for API anomaly signals)* |



## Is this all you need?

Still outside this matrix (handle separately):
- matching engine / ledger / risk (already in trading phases)
- full KYC/AML product flows
- custody / cold wallet operations
- infra WAF/DDoS beyond basic rate limits and CAPTCHA