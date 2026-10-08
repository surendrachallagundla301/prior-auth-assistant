# Guardrails for infrastructure that stores PHI. Run with:
#   conftest test --parser hcl2 --policy policy infra/
package main

import rego.v1

# Blocks parsed from HCL come through as lists; normalise to individual resources.
resources(rtype) := [r |
	some name
	some r in as_list(input.resource[rtype][name])
]

as_list(x) := x if is_array(x)
as_list(x) := [x] if not is_array(x)

deny contains msg if {
	some key in resources("aws_kms_key")
	not key.enable_key_rotation == true
	msg := "KMS keys protecting PHI must have enable_key_rotation = true"
}

deny contains msg if {
	some block in resources("aws_s3_bucket_public_access_block")
	some flag in ["block_public_acls", "block_public_policy", "ignore_public_acls", "restrict_public_buckets"]
	not block[flag] == true
	msg := sprintf("S3 public access block must set %s = true", [flag])
}

deny contains msg if {
	some acl in resources("aws_s3_bucket_acl")
	acl.acl in {"public-read", "public-read-write", "authenticated-read"}
	msg := sprintf("S3 ACL '%s' would expose PHI", [acl.acl])
}

deny contains msg if {
	count(input.resource.aws_s3_bucket) > 0
	count(object.get(input.resource, "aws_s3_bucket_server_side_encryption_configuration", {})) == 0
	msg := "Every S3 bucket must have a server-side encryption configuration"
}

deny contains msg if {
	some cfg in resources("aws_s3_bucket_server_side_encryption_configuration")
	some rule in as_list(cfg.rule)
	some def in as_list(rule.apply_server_side_encryption_by_default)
	def.sse_algorithm != "aws:kms"
	msg := "PHI buckets must use SSE-KMS (aws:kms), not S3-managed keys"
}

deny contains msg if {
	some lg in resources("aws_cloudwatch_log_group")
	not lg.retention_in_days >= 365
	msg := "Log groups must retain logs for at least 365 days for audit purposes"
}

deny contains msg if {
	some lg in resources("aws_cloudwatch_log_group")
	not lg.kms_key_id
	msg := "Log groups must be encrypted with a customer-managed KMS key"
}
