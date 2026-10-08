# Storage for prior-authorization documents (synthetic PHI in this project).
# Every resource here must pass the policies in ../policy before it can be applied.

terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

variable "region" {
  type    = string
  default = "us-east-2"
}

variable "environment" {
  type    = string
  default = "dev"
}

locals {
  name = "prior-auth-${var.environment}"
  tags = {
    project     = "prior-auth-assistant"
    environment = var.environment
    data_class  = "phi"
  }
}

resource "aws_kms_key" "phi" {
  # checkov:skip=CKV2_AWS_64: Uses the default account key policy in the demo; production defines a least-privilege policy
  description             = "Encrypts PHI documents and logs for ${local.name}"
  enable_key_rotation     = true
  deletion_window_in_days = 30
  tags                    = local.tags
}

resource "aws_s3_bucket" "documents" {
  # checkov:skip=CKV_AWS_144: Cross-region replication is out of scope for this single-region prototype
  # checkov:skip=CKV2_AWS_62: No downstream consumers need S3 event notifications yet
  # checkov:skip=CKV_AWS_18: Access logging bucket omitted to keep the demo small; CloudTrail data events cover audit
  bucket = "${local.name}-documents"
  tags   = local.tags
}

resource "aws_s3_bucket_versioning" "documents" {
  bucket = aws_s3_bucket.documents.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.phi.arn
    }
    bucket_key_enabled = true
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id
  rule {
    id     = "expire-old-versions"
    status = "Enabled"
    filter {}
    noncurrent_version_expiration {
      noncurrent_days = 90
    }
    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

resource "aws_s3_bucket_public_access_block" "documents" {
  bucket                  = aws_s3_bucket.documents.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudwatch_log_group" "app" {
  name              = "/prior-auth/${var.environment}/app"
  retention_in_days = 365
  kms_key_id        = aws_kms_key.phi.arn
  tags              = local.tags
}

output "documents_bucket" {
  value = aws_s3_bucket.documents.bucket
}
