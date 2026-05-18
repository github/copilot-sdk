# Create repository ruleset for github/copilot-sdk per issue ghcp-sp-95
# To revert: gh api repos/github/copilot-sdk/rulesets/<ID> -X DELETE

$payload = @'
{
  "name": "ghcp-sp-95-java-branch-protection",
  "target": "branch",
  "enforcement": "active",
  "conditions": {
    "ref_name": {
      "include": ["refs/heads/main"],
      "exclude": []
    }
  },
  "bypass_actors": [
    {
      "actor_id": 5,
      "actor_type": "RepositoryRole",
      "bypass_mode": "always"
    }
  ],
  "rules": [
    { "type": "deletion" },
    {
      "type": "pull_request",
      "parameters": {
        "required_approving_review_count": 1,
        "dismiss_stale_reviews_on_push": false,
        "require_code_owner_review": false,
        "require_last_push_approval": false,
        "required_review_thread_resolution": false,
        "allowed_merge_methods": ["merge", "squash", "rebase"]
      }
    },
    { "type": "non_fast_forward" }
  ]
}
'@

$response = $payload | gh api repos/github/copilot-sdk/rulesets -X POST --input -

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create ruleset. Exit code: $LASTEXITCODE"
    exit 1
}

$parsed = $response | ConvertFrom-Json
$rulesetId = $parsed.id

Write-Host "Ruleset created successfully." -ForegroundColor Green
Write-Host "  Name: $($parsed.name)"
Write-Host "  ID:   $rulesetId"
Write-Host ""
Write-Host "To revert this change:" -ForegroundColor Yellow
Write-Host "  gh api repos/github/copilot-sdk/rulesets/$rulesetId -X DELETE"
