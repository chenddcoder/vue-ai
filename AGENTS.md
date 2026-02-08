# AGENTS.md

This document provides guidelines for AI assistants working with this project.

## Project Overview

This is a Vue AI Platform with a frontend (Vue 3 + TypeScript) and backend (Spring Boot + SQLite).

## Technology Stack

### Frontend (vue-ai-platform)
- Vue 3
- TypeScript
- Vite
- Ant Design Vue 4
- Pinia (state management)
- Vue Router
- Monaco Editor

### Backend (vue-ai-server)
- Spring Boot 2.7.18
- SQLite (H2-compatible)
- Hutool utilities

## Package Manager

**pnpm** is used for frontend dependency management.

## Available Commands

### Frontend (vue-ai-platform)

```bash
# Install dependencies
pnpm install

# Start development server
pnpm dev

# Build for production
pnpm build

# Preview production build
pnpm preview
```

### Backend (vue-ai-server)

```bash
# Run Spring Boot application
cd vue-ai-server
mvn spring-boot:run
```

## Project Structure

```
vue-ai/
├── vue-ai-platform/        # Frontend application
│   ├── src/
│   │   ├── views/          # Page components
│   │   ├── components/     # Reusable components
│   │   ├── stores/         # Pinia stores
│   │   └── router/         # Vue Router config
│   └── vite.config.ts      # Vite configuration
│
└── vue-ai-server/          # Backend application
    └── src/main/java/      # Spring Boot application
```

## Common Development Tasks

### Adding New Dependencies

```bash
cd vue-ai-platform
pnpm add <package-name>
```

### Building the Project

```bash
cd vue-ai-platform
pnpm build
```

### Running Tests

Tests are configured with Playwright in `vue-ai-platform/tests/`.

```bash
cd vue-ai-platform
pnpm test
```

### Verifying Code

After completing any feature, always verify the code compiles without errors:

```bash
# Frontend: Run TypeScript check and build
cd vue-ai-platform
pnpm vue-tsc -b  # TypeScript type checking
npx cross-env NODE_OPTIONS=--max-old-space-size=4096 vite build  # Production build

# Backend: Compile the project
cd vue-ai-server
mvn compile
```

## Configuration Files

- `vue-ai-platform/package.json`: Frontend dependencies and scripts
- `vue-ai-platform/vite.config.ts`: Vite bundler configuration
- `vue-ai-platform/tsconfig.json`: TypeScript configuration
- `vue-ai-server/pom.xml`: Maven dependencies
- `vue-ai-server/src/main/resources/application.yml`: Spring Boot configuration
