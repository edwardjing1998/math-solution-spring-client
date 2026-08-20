# Deploy math-solution-spring-client to OpenShift

This setup builds a Java 21 container, pushes it to GitHub Container Registry
(GHCR), and deploys the immutable commit image to OpenShift.

## Repository files

```text
Dockerfile
.dockerignore
.github/workflows/deploy-openshift.yml
openshift/deployment.yaml
openshift/service.yaml
openshift/route.yaml
openshift/kustomization.yaml
```

## 1. Create the OpenShift project

Log in as a user that can create projects and grant namespace roles:

```bash
oc login --server="https://api.your-cluster.example.com:6443"
oc new-project math-solution
```

If the project already exists:

```bash
oc project math-solution
```

## 2. Create the deployment service account

```bash
oc create serviceaccount github-actions-deployer \
  --namespace math-solution

oc adm policy add-role-to-user edit \
  -z github-actions-deployer \
  --namespace math-solution
```

Generate a token using a duration allowed by your cluster policy:

```bash
oc create token github-actions-deployer \
  --namespace math-solution \
  --duration=720h
```

Store the resulting token in GitHub as `OPENSHIFT_TOKEN`. Rotate it before it
expires. Do not commit it.

Get the cluster API URL:

```bash
oc whoami --show-server
```

Store that value in GitHub as `OPENSHIFT_SERVER`. A GitHub-hosted runner must be
able to reach this URL. Use a self-hosted runner if the cluster API is private.

## 3. Create the Databricks runtime secret

Create this secret once in OpenShift. Use the caller service principal, not the
GitHub deployment service principal.

```bash
oc create secret generic math-solution-databricks \
  --namespace math-solution \
  --from-literal=DATABRICKS_HOST="https://adb-7405618365754751.11.azuredatabricks.net" \
  --from-literal=DATABRICKS_APP_URL="https://math-solution-service-7405618365754751.11.azure.databricksapps.com" \
  --from-literal=DATABRICKS_CLIENT_ID="YOUR_CALLER_APPLICATION_ID" \
  --from-literal=DATABRICKS_CLIENT_SECRET="YOUR_CALLER_SECRET_VALUE"
```

Use the secret **value**, not the secret ID. The caller service principal needs
`CAN USE` permission on the Databricks App.

For an existing secret, replace it safely:

```bash
oc create secret generic math-solution-databricks \
  --namespace math-solution \
  --from-literal=DATABRICKS_HOST="https://adb-7405618365754751.11.azuredatabricks.net" \
  --from-literal=DATABRICKS_APP_URL="https://math-solution-service-7405618365754751.11.azure.databricksapps.com" \
  --from-literal=DATABRICKS_CLIENT_ID="YOUR_CALLER_APPLICATION_ID" \
  --from-literal=DATABRICKS_CLIENT_SECRET="YOUR_CALLER_SECRET_VALUE" \
  --dry-run=client -o yaml | oc apply -f -
```

## 4. Configure GHCR image pulling

If the GHCR package is public, remove `imagePullSecrets` from
`openshift/deployment.yaml`.

For a private GHCR package, create a GitHub personal access token with
`read:packages`, and run:

```bash
oc create secret docker-registry ghcr-pull-secret \
  --namespace math-solution \
  --docker-server=ghcr.io \
  --docker-username="YOUR_GITHUB_USERNAME" \
  --docker-password="YOUR_GITHUB_PAT" \
  --docker-email="YOUR_EMAIL"
```

The PAT is stored only in OpenShift. Do not add it to the repository.

## 5. Configure GitHub

In the GitHub repository, open:

```text
Settings -> Environments -> New environment -> production
```

Add these environment secrets:

| Secret | Value |
| --- | --- |
| `OPENSHIFT_SERVER` | Output of `oc whoami --show-server` |
| `OPENSHIFT_TOKEN` | Token for `github-actions-deployer` |

The workflow uses the built-in `GITHUB_TOKEN` to publish to GHCR, so no separate
push token is required. Its permissions are limited to `contents: read` and
`packages: write`.

## 6. Commit and deploy

```bash
git add Dockerfile .dockerignore .github/workflows/deploy-openshift.yml openshift
git commit -m "Add OpenShift deployment"
git push origin main
```

The workflow will test the application, build and push two image tags, apply
the OpenShift resources, set the Deployment to the immutable commit tag, and
wait for rollout completion.

## 7. Verify from OpenShift

```bash
oc get deployment,pods,service,route \
  --namespace math-solution

oc rollout status deployment/math-solution-spring-client \
  --namespace math-solution

oc logs deployment/math-solution-spring-client \
  --namespace math-solution \
  --follow
```

Get the public route:

```bash
oc get route math-solution-spring-client \
  --namespace math-solution \
  -o jsonpath='https://{.spec.host}{"\n"}'
```

Health check:

```bash
curl --fail --show-error \
  "https://ROUTE_HOST/actuator/health"
```

Call the API:

```bash
curl --include \
  --show-error \
  --max-time 300 \
  --request POST \
  "https://ROUTE_HOST/api/math-solutions" \
  --header "Content-Type: application/json" \
  --data '{
    "input_blob_path": "uploads/gemma3_math_problem.png",
    "output_blob_path": "temporary/gemma3_math_solution_from_openshift.html",
    "language": "zh-CN"
  }'
```

## Troubleshooting

```bash
oc describe pod -l app.kubernetes.io/name=math-solution-spring-client \
  --namespace math-solution

oc get events \
  --namespace math-solution \
  --sort-by='.lastTimestamp'
```

- `ImagePullBackOff`: verify `ghcr-pull-secret` and GHCR package access.
- `CreateContainerConfigError`: verify `math-solution-databricks` exists.
- Readiness probe failure: inspect application logs and `/actuator/health`.
- HTTP 401/403 from Databricks: verify the caller application ID, secret value,
  and the Databricks App `CAN USE` permission.
- GitHub cannot reach OpenShift: use a self-hosted runner with network access to
  the cluster API.
