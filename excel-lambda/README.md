

    aws iam create-role --role-name ACMELambdaExecutionRole --assume-role-policy-document '{"Version": "2012-10-17","Statement": [{ "Effect": "Allow", "Principal": {"Service": "lambda.amazonaws.com"}, "Action": "sts:AssumeRole"}]}'


    LAMBDA_ROLE_ARN="arn:aws:iam::687441526170:role/ACMELambdaExecutionRole" sh target/manage.sh create