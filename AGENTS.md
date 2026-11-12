# Repository collaboration instructions

This file is the shared source of truth for ChatGPT, Claude, Claude Code, and other coding agents.

## Before changing anything
- Read this file and the project README, then inspect the actual source tree and build configuration. Do not assume this repository is the same as another repository owned by the user.
- Report the detected app/platform, entry points, modules, build/test commands, and current Git branch before making substantial changes.
- Check recent commits and existing work. Preserve user changes and existing behavior unless the task explicitly asks otherwise.
- Never claim a build, test, install, or device check passed unless it was actually run and its result was observed.

## Safe Git workflow
- Treat `main` as protected working ground: do not commit directly to `main`, force-push, rewrite history, or merge a pull request unless the user explicitly approves that exact action.
- Create one focused branch per task using `feat/<short-name>`, `fix/<short-name>`, or `chore/<short-name>`.
- Keep changes small and scoped. Do not let two agents edit the same files concurrently.
- Before handing work to another agent, provide: goal, branch, files changed, decisions, commands run and results, remaining work, and known risks.
- Open a pull request for review. Explain behavior changes, tests, risks, and rollback. Wait for user approval before merging.
- Do not delete files, repositories, branches, releases, workflows, or user data unless the user explicitly authorizes the specific deletion.

## Project-specific accuracy
- Discover the real language, framework, modules, and commands from repository files. Do not copy build instructions from another project.
- Prefer the smallest maintainable change. Follow existing naming, formatting, architecture, and dependency conventions.
- Add or update tests when practical. If tests cannot be run, state why and give exact manual verification steps.
- Do not upgrade dependencies, SDKs, Gradle, Kotlin, or toolchains unless required by the task and explained first.
- For Android projects, inspect the manifest, application IDs, min/target SDKs, permissions, exported components, and signing/build setup before changing them.
- For TV/remote-control projects, preserve D-pad navigation, focus behavior, fullscreen behavior, and compatibility with the actual target device.

## Security and privacy
- Never commit API keys, access tokens, passwords, signing keys, keystores, private certificates, or personal data.
- Keep local secrets and machine-specific files out of Git; use documented placeholders such as `.env.example` only when relevant.
- Treat network input as untrusted; validate URLs, intents, commands, and user-provided data.
- Do not weaken transport security or permissions without explaining the security impact and obtaining approval when the change is material.

## Agent handoff
Use this compact format when passing work between agents:

- Goal:
- Repository / branch:
- Current state:
- Files changed:
- Checks run and results:
- Decisions / constraints:
- Next step:
