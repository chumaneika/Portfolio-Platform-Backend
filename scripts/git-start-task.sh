#!/usr/bin/env bash

set -euo pipefail

usage() {
	echo "Usage: $0 <feature/name|fix/name>" >&2
	exit 2
}

[[ $# -eq 1 ]] || usage

branch_name=$1
if [[ ! $branch_name =~ ^(feature|fix)/[a-zA-Z0-9][a-zA-Z0-9._/-]*$ ]]; then
	echo "Branch must start with feature/ or fix/ and contain no spaces." >&2
	exit 2
fi

repository_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
	echo "This command must be run inside a Git repository." >&2
	exit 1
}
cd "$repository_root"

if [[ -n $(git status --porcelain) ]]; then
	echo "The working tree is not clean. Commit or stash changes before starting a task." >&2
	exit 1
fi

if ! git show-ref --verify --quiet refs/heads/main; then
	echo "Local main branch does not exist." >&2
	exit 1
fi

if git show-ref --verify --quiet "refs/heads/$branch_name"; then
	echo "Local branch '$branch_name' already exists." >&2
	exit 1
fi

git switch main
git switch -c "$branch_name"

echo "Created '$branch_name' from local main."
echo "No remote operations were performed."
