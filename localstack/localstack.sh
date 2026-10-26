#!/bin/sh

profile=localdev
region=eu-west-1

#Queues
declare -a queues=(
		${profile}-store-products-created-queue
)

#Buckets
declare -a buckets=(
    ${profile}-store-products
)

echo "Creating SQS queues..."
for queue in "${queues[@]}"
do
	aws --endpoint-url=http://localhost:4566 sqs create-queue --attributes VisibilityTimeout=300 --queue-name $queue
done
echo "Created SQS queues successfully"


echo "Creating S3 buckets..."
for bucket in "${buckets[@]}"
do
	aws --endpoint-url=http://localhost:4566 s3 mb s3://$bucket
done
echo "Created S3 buckets successfully"

# Merchants live in Postgres (table merchants); create one with POST /v1/store/merchants.
echo "Setup completed successfully"
