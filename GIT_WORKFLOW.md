# Git workflow

Это один репозиторий для всех сервисов. Ветка `main` содержит их актуальное
состояние. Для каждой задачи создавайте временную ветку `feature/*` или `fix/*`.

Перед переключением веток сохраните текущие изменения коммитом или `git stash`.

## Начать задачу

```bash
git switch main
git pull --ff-only origin main
git switch -c feature/profile-links
```

Для исправления ошибки используйте имя вида `fix/profile-validation`.

## Завершить задачу

Проверьте изменённые сервисы, создайте коммит и слейте ветку в локальную `main`:

```bash
cd profile-service
./mvnw clean test
cd ..
git add profile-service
git commit -m "feat(profile-service): add links"
git switch main
git merge --no-ff feature/profile-links
```

Обычный `git merge feature/profile-links` тоже подходит: при возможности Git
выполнит fast-forward. После проверки можно удалить временную ветку командой
`git branch -d feature/profile-links`. Отправка на GitHub выполняется отдельно:
`git push origin main`.

## Вернуть один сервис к выбранному коммиту

Найдите нужный коммит: `git log --oneline -- profile-service`. Начните с чистого
рабочего дерева и создайте ветку исправления:

```bash
git switch main
git switch -c fix/restore-profile-service
git restore --source=<commit> --staged --worktree -- profile-service/
git diff --cached -- profile-service/
cd profile-service
./mvnw clean test
cd ..
git commit -m "fix(profile-service): restore previous version"
git switch main
git merge --no-ff fix/restore-profile-service
```

`git restore` берёт состояние только указанной папки из выбранного коммита.
После коммита откат становится частью обычной истории, а другие сервисы
сохраняют текущее состояние. Если плохой коммит затронул исключительно этот
сервис, его также можно отменить командой `git revert <commit>`.
