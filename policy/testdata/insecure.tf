# Deliberately non-compliant: used to prove the policies catch real mistakes.
resource "aws_kms_key" "bad" {
  description = "no rotation"
}

resource "aws_s3_bucket" "bad" {
  bucket = "bad-bucket"
}

resource "aws_s3_bucket_acl" "bad" {
  bucket = aws_s3_bucket.bad.id
  acl    = "public-read"
}

resource "aws_s3_bucket_public_access_block" "bad" {
  bucket              = aws_s3_bucket.bad.id
  block_public_acls   = false
  block_public_policy = true
}

resource "aws_cloudwatch_log_group" "bad" {
  name              = "/bad"
  retention_in_days = 7
}
