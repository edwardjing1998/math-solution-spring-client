# create variables:

export DATABRICKS_HOST="https://adb-7405618365754751.11.azuredatabricks.net"
export DATABRICKS_APP_URL="https://math-solution-service-7405618365754751.11.azure.databricksapps.com"
export DATABRICKS_CLIENT_ID="924f7402-f27f-49e4-87a8-330533d6a317"
export DATABRICKS_CLIENT_SECRET="dose1e4230651c30b785f44e9f2d1b4e8803"

lsof -i :8080

# call the API:

curl --include \
--show-error \
--max-time 300 \
--request POST \
"http://localhost:8888/api/math-solutions" \
--header "Content-Type: application/json" \
--data '{
"input_blob_path": "uploads/gemma3_math_problem.png",
"output_blob_path": "temporary/gemma3_math_solution_from_spring.html",
"language": "zh-CN"
}'