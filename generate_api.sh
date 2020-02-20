wget https://raw.githubusercontent.com/slackapi/slack-api-specs/master/web-api/slack_web_openapi_v2.json -O swagger_spec/slack_web_openapi_v2.json
./gradlew openApiGenerate

