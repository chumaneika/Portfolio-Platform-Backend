#!/usr/bin/env bash

set -euo pipefail

usage() {
	echo "Usage: $0 <service-directory> <commit>" >&2
	echo "Example: $0 profile-service c8d0de9" >&2
	exit 2
}

[[ $# -eq 2 ]] || usage

service_directory=$1
source_revision=$2

repository_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
	echo "This command must be run inside a Git repository." >&2
	exit 1
}
cd "$repository_root"

current_branch=$(git branch --show-current)
if [[ ! $current_branch =~ ^(feature|fix)/.+$ ]]; then
	echo "Restore a service only from a feature/* or fix/* branch, never directly from main." >&2
	exit 1
fi

if [[ $service_directory == */* || $service_directory == .* ]]; then
	echo "Service must be a top-level repository directory." >&2
	exit 2
fi

if [[ ! -d $service_directory || ! -f $service_directory/pom.xml ]]; then
	echo "'$service_directory' is not a top-level Maven service directory." >&2
	exit 2
fi

if ! source_commit=$(git rev-parse --verify "$source_revision^{commit}" 2>/dev/null); then
	echo "'$source_revision' does not resolve to a commit." >&2
	exit 2
fi

if ! git cat-file -e "$source_commit:$service_directory" 2>/dev/null; then
	echo "'$service_directory' does not exist in commit '$source_revision'." >&2
	exit 2
fi

if [[ -n $(git status --porcelain -- "$service_directory") ]]; then
	echo "'$service_directory' has uncommitted changes. Commit or stash them first." >&2
	exit 1
fi

git restore --source "$source_commit" --worktree -- "$service_directory"

echo "Restored '$service_directory' in the working tree from $source_commit."
echo "Other service directories were not changed. Review, test and commit the result."
echo "Nothing was committed, merged or pushed automatically."
