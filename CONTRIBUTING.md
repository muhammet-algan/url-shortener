# Contributing to URL Shortener

Thank you for your interest in contributing to **URL Shortener**! We welcome bug fixes, improvements, and new feature suggestions.

## 🚀 Getting Started

1. **Fork the Repository** on GitHub.
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/your-username/url-shortener.git
   cd url-shortener
   ```
3. **Create a topic branch**:
   ```bash
   git checkout -b feature/my-new-feature
   ```

## 🛠️ Development Setup

- Ensure **Docker & Docker Compose** are installed and running.
- Start the entire stack:
  ```bash
  docker compose up --build
  ```
- Or run locally with Java 17 and Maven:
  ```bash
  mvn clean test
  ```

## 📋 Code Guidelines

- Follow standard Java naming conventions and Google Java Style Guide.
- Write unit tests for all new services and controller endpoints under `src/test/java/`.
- Ensure all tests pass before submitting your PR:
  ```bash
  mvn test
  ```

## 📬 Submitting a Pull Request

1. Commit your changes with clear, semantic commit messages (e.g. `feat:`, `fix:`, `docs:`, `test:`).
2. Push your branch to your fork:
   ```bash
   git push origin feature/my-new-feature
   ```
3. Open a **Pull Request** against `main` on the primary repository.
4. Describe your changes and reference any related issues.
