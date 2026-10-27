# Local development: bootstrap the first administrator

Public registration intentionally assigns the CUSTOMER role. Do not add an unauthenticated endpoint that lets a caller choose ADMIN or SELLER.

For a local development database only, you can bootstrap the first administrator manually:

1. Start Auth Service and register the account you intend to use as the local administrator.
2. Connect to your local MySQL instance and select the jaggery_auth database.
3. Confirm the registered email address and then execute the following statement with your own address substituted:

   ```sql
   UPDATE users
   SET role = 'ADMIN'
   WHERE email = 'replace-with-your-registered-email'
     AND role = 'CUSTOMER';
   ```

4. Verify exactly one row changed. If zero or multiple rows are reported, stop and investigate rather than broadening the update.
5. Log in again to obtain a new JWT containing the ADMIN role.

**Never run the example unchanged, never commit database credentials, and do not use manual SQL as a production provisioning procedure.** Production requires an approved, audited bootstrap process and secret management. Do not expose MySQL publicly.

An administrator can approve an existing customer as a seller via the authenticated endpoint:

- POST /api/users/{id}/approve-seller
- Requires a valid bearer token with the ADMIN role.
- The promoted user must log in again to obtain a JWT with the new role.

The endpoint does not allow public callers to promote themselves. Keep an audit trail for role changes before production use.
