# math-solution-spring-client

Java 21 / Spring Boot 3.5 sample that calls the deployed Databricks App `math-solution-service`.

Flow:

```text
React or curl -> Spring Boot -> Databricks OAuth M2M -> Databricks App -> Azure/Gemma
```

The application uses a dedicated caller service principal, caches its short-lived OAuth access token in memory, refreshes it five minutes before expiration, and retries a Databricks App call once when it receives HTTP 401.

## Prerequisites

1. Create a Databricks service principal named `math-solution-api-client`.
2. Enable Workspace access for it.
3. Generate a Databricks OAuth secret. Use the secret **value**, not the secret ID.
4. Grant the service principal `CAN USE` on the Databricks App `math-solution-service`.
5. Ensure the Databricks App is RUNNING and its `/api/health` endpoint works.

Keep this caller identity separate from:

- `github-math-solution-deployer`, which deploys and manages the app.
- The Azure Storage service principal, which reads and writes blobs.

## Configuration

Set environment variables. Never commit real credentials.

```bash
export DATABRICKS_HOST="https://adb-7405618365754751.11.azuredatabricks.net"
export DATABRICKS_APP_URL="https://math-solution-service-7405618365754751.11.azure.databricksapps.com"
export DATABRICKS_CLIENT_ID="APPLICATION_ID_OF_math-solution-api-client"
export DATABRICKS_CLIENT_SECRET="DATABRICKS_OAUTH_SECRET_VALUE"
```

For production, inject the secret from Azure Key Vault or your platform secret manager.

## Run

```bash
mvn spring-boot:run
```

## Test

```bash
mvn test
```

## Call the Spring Boot API

```bash
curl --include \
  --show-error \
  --max-time 300 \
  --request POST \
  "http://localhost:8080/api/math-solutions" \
  --header "Content-Type: application/json" \
  --data '{
    "input_blob_path": "uploads/gemma3_math_problem.png",
    "output_blob_path": "temporary/gemma3_math_solution_from_spring.html",
    "language": "zh-CN"
  }'
```

Expected response:

```json
{
  "status": "completed",
  "input_blob_path": "uploads/gemma3_math_problem.png",
  "output_blob_path": "temporary/gemma3_math_solution_from_spring.html",
  "content_type": "text/html; charset=utf-8"
}
```

Health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

## Security behavior

- The OAuth access token is kept in memory only.
- The access token is never returned by a controller or written to logs.
- A token is reused until five minutes before expiration.
- An HTTP 401 from the Databricks App invalidates the cached token and causes one retry.
- Other Databricks errors are translated into a sanitized `502 Bad Gateway` response.
- The client secret must be rotated before its expiration.

## Relevant Databricks endpoints

```text
POST https://<workspace>/oidc/v1/token
POST https://<app-url>/api/v1/solutions
```

Official documentation:

- https://docs.databricks.com/aws/en/dev-tools/databricks-apps/connect-local
