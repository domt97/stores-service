#!/bin/sh

profile=localdev
region=eu-west-1

echo "Creating DynamoDB table: ${profile}_tenant_info..."

aws dynamodb create-table \
  --endpoint-url=http://localhost:4566 \
  --region ${region} \
  --table-name ${profile}_tenant_info \
  --attribute-definitions \
    AttributeName=pk,AttributeType=S \
  --key-schema \
    AttributeName=pk,KeyType=HASH \
  --provisioned-throughput \
    ReadCapacityUnits=3,WriteCapacityUnits=3 2>&1 | grep -v "ResourceInUseException"

echo "Table ready (created or already exists)"
sleep 2

echo "Inserting test data..."

aws dynamodb put-item \
  --endpoint-url=http://localhost:4566 \
  --region ${region} \
  --table-name ${profile}_tenant_info \
  --item '{
    "pk": {
      "S": "7b7fdc98-2c7c-45a9-bc1c-cfd4d52a55b8"
    },
    "code": {
      "S": "DTVN"
    },
    "name": {
      "S": "Do Tran Coffee Vietnam"
    },
    "status": {
      "S": "ACTIVE"
    },
    "settings": {
      "M": {
        "timezone": {
          "S": "Asia/Ho_Chi_Minh"
        },
        "currency": {
          "S": "VND"
        },
        "locale": {
          "S": "vi-VN"
        }
      }
    },
    "createdAt": {
      "S": "2026-08-02T15:00:00Z"
    },
    "updatedAt": {
      "S": "2026-08-02T15:00:00Z"
    }
  }'

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to insert data"
  exit 1
fi

echo "Setup completed successfully"