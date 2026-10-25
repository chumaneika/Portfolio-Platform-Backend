# Git workflow for the monorepository

This repository is one monorepository. The directories `admin-service`,
`api-gateway-service`, `discovery-service`, `identity-service`,
`portfolio-service` and `profile-service` share one Git history and one
integration branch: `main`.

## Rules

- `main` contains the current compatible state of all services.
- Do not develop directly in `main`.
- Use `feature/<name>` for new functionality and `fix/<name>` for fixes and
  service restores.
- Keep a task focused. Prefer commits such as
  `feat(profile-service): add public profile endpoint` and
  `fix(identity-service): reject expired token`.
- Test every changed service before merging. Each Maven service has its own
  wrapper, for example: `cd profile-service && ./mvnw clean test`.
- Merge completed tasks into local `main` with `--no-ff`. This keeps the task
  boundary visible in the shared monorepository history.
- The helper scripts never fetch, pull or push. Updating and publishing remain
  explicit user actions.

## Start a task

First make sure the current work is committed or stashed. Update local `main`
explicitly when needed, then create the task branch:

```bash
git switch main
git pull --ff-only origin main
./scripts/git-start-task.sh feature/public-profile
```

For a bug fix:

```bash
./scripts/git-start-task.sh fix/profile-validation
```

The start script requires a clean working tree and creates the new branch from
local `main`. It does not contact GitHub.

## Finish a task locally

Commit all task changes and run the tests of every affected service. Then:

```bash
./scripts/git-finish-task.sh
```

The script requires a clean `feature/*` or `fix/*` branch, switches to local
`main` and runs a `--no-ff` merge. It does not delete the task branch and does
not push anything. After verification, the branch can be deleted manually:

```bash
git branch -d feature/public-profile
```

Push only when you intentionally want to publish the result:

```bash
git push origin main
```

## Restore one service to a selected commit

Do not reset the whole monorepository. Create a fix branch and restore only the
service directory from the selected commit:

```bash
./scripts/git-start-task.sh fix/restore-profile-service
./scripts/git-restore-service.sh profile-service <commit>
git diff -- profile-service
cd profile-service
./mvnw clean test
cd ..
git add profile-service
git commit -m "fix(profile-service): restore service to <commit>"
./scripts/git-finish-task.sh
```

`git-restore-service.sh` accepts only a top-level Maven service directory,
refuses to run directly on `main`, and refuses to overwrite uncommitted changes
inside that service. It restores the chosen service snapshot in the working
tree without touching other service directories. The resulting rollback is a
new ordinary commit, so shared history is not rewritten.

To inspect suitable commits for one service:

```bash
git log --oneline -- profile-service
git show <commit> -- profile-service
```

If only one bad commit must be undone and that commit changed no other service,
`git revert <commit>` is also suitable. When a commit contains changes to
multiple services, prefer the directory restore workflow above so unrelated
services remain unchanged.
