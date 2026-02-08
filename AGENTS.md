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
- JDK 1.8
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
# Set JDK 1.8 (ensure JAVA_HOME points to JDK 1.8)
export JAVA_HOME=/path/to/jdk1.8

# Run Spring Boot application
cd vue-ai-server
mvn spring-boot:run

# Compile the project
cd vue-ai-server
mvn compile

# Package the application
cd vue-ai-server
mvn package
```

## Project Structure

```
vue-ai/
├── vue-ai-platform/        # Frontend application
│   ├── src/
│   │   ├── views/          # Page components
│   │   │   ├── EditorLayout.vue
│   │   │   ├── MarketLaunchpad.vue    # App market with window system
│   │   │   ├── Launchpad.vue          # Fullscreen launchpad
│   │   │   ├── MyApps.vue
│   │   │   ├── AIConfig.vue
│   │   │   └── Login.vue
│   │   ├── components/     # Reusable components
│   │   ├── stores/         # Pinia stores
│   │   ├── router/         # Vue Router config
│   │   ├── api/            # API definitions
│   │   └── assets/         # Static assets
│   └── vite.config.ts      # Vite configuration
│
└── vue-ai-server/          # Backend application
    └── src/main/java/      # Spring Boot application
```

## New Features (v0.5.0)

### MarketLaunchpad.vue
- App market with icon grid display
- Click-to-launch application
- Window management system (drag, minimize, maximize, close)
- Mac-style Dock bar at bottom
- Search and category filtering

### Launchpad.vue
- Fullscreen launchpad overlay
- App icon grid
- Same window management features as MarketLaunchpad

### Window Management Features
- Multiple windows support
- Drag to move windows
- Minimize to dock bar
- Maximize/fullscreen toggle
- Close windows
- Z-index management (click to bring to front)
- ESC key to minimize active window

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

## Development Workflow

### After Completing Development

After completing any feature implementation, always:

1. **Update README.md**
   - Document new features in the changelog
   - Update version number and feature status tables
   - Add new API endpoints to documentation
   - Update database schema if applicable

2. **Plan Next Version Iteration**
   - Review completed features and mark in changelog
   - Identify remaining features from current version plan
   - Prioritize features for next version (P1, P2, etc.)
   - Update version roadmap with new features and improvements

3. **Verify Code Quality**
   - Run frontend TypeScript check: `pnpm vue-tsc -b`
   - Run backend compilation: `mvn compile`
   - Fix any errors before committing

### Version Planning

- **P1 (High Priority)**: Core features that must be completed
- **P2 (Medium Priority)**: Important enhancements
- **P3 (Low Priority)**: Nice-to-have improvements

When planning versions:
- Group related features together
- Consider user impact and dependencies
- Set realistic goals for each iteration
