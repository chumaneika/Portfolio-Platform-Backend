#!/usr/bin/env bash

set -euo pipefail

repository_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
	echo "This command must be run inside a Git repository." >&2
	exit 1
}
cd "$repository_root"

task_branch=$(git branch --show-current)
if [[ ! $task_branch =~ ^(feature|fix)/.+$ ]]; then
	echo "Current branch must be feature/* or fix/*." >&2
	exit 1
fi

if [[ -n $(git status --porcelain) ]]; then
	echo "The working tree is not clean. Commit or stash changes before merging." >&2
	exit 1
fi

git switch main
git merge --no-ff "$task_branch"

echo "Merged '$task_branch' into local main."
echo "The task branch was kept. Delete it after verification with:"
echo "  git branch -d '$task_branch'"
echo "No remote operations were performed."
