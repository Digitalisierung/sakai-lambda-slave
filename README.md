# sakai-lambda-slave
Backend service for sakai-ng-master.

### JAR direkt in Lambda hochladen.

```bash
aws lambda update-function-code \
--function-name ${LAMBDA_FUNCTION_NAME} \
--s3-bucket ${S3_LAMBDA_ART_BUCKET} \
--s3-key asset-service-lambda.jar \
--profile sakai-cdk-lab
```
