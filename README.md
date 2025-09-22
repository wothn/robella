# Robella - AI API Gateway

Robella is an AI API gateway that provides unified access to multiple AI service providers (OpenAI, Anthropic/Claude, Qwen, etc.). It allows users to access any model through any endpoint with flexible routing and load balancing.

## Features

- OpenAI API compatible endpoints (`/v1/chat/completions`, `/v1/models`)
- Anthropic native API support (`/anthropic/v1/messages`)
- Multi-AI provider support with unified interface
- Real-time streaming response handling
- Dynamic model routing and load balancing
- JWT authentication with GitHub OAuth
- Comprehensive request logging and analytics
- Web-based management interface

## Architecture

The system is structured in three layers:
- **Provider**: AI service providers (OpenAI, Anthropic, etc.) with authentication and API endpoints
- **Model**: AI models (gpt-4, claude-2, etc.) with capabilities exposed to users
- **VendorModel**: Connects Provider and Model, defining specific implementation and pricing

## Technology Stack

### Backend
- **Java**: 21 with virtual threads
- **Framework**: Spring Boot 3.3.10 (MVC, not WebFlux)
- **Database**: PostgreSQL with MyBatis-Plus
- **Authentication**: JWT with GitHub OAuth
- **Build**: Maven 3.8+

### Frontend
- **Framework**: React 18.3.1 with TypeScript
- **Build Tool**: Vite
- **UI**: Shadcn UI with Radix UI primitives
- **Styling**: Tailwind CSS

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL database

### Environment Variables

Set the following environment variables before running the application:

```bash
export POSTGRES_USERNAME="your-db-username"
export POSTGRES_PASSWORD="your-db-password"
export JWT_SECRET="your-jwt-secret"
export GITHUB_CLIENT_ID="your-github-client-id"
export GITHUB_CLIENT_SECRET="your-github-client-secret"
```

### Build Project

```bash
mvn clean package
```

### Run Application

```bash
java -jar target/robella-0.1.0.jar
```

### Development Mode

```bash
mvn spring-boot:run
```

The application runs on port 10032 by default.


## Frontend Development

### Technology Stack

- React 18.3.1 with TypeScript
- Vite build tool
- Tailwind CSS
- Shadcn UI components
- Radix UI primitives

### Development Setup

```bash
cd web
npm install
npm run dev
```

### Build Production

```bash
cd web
npm run build
```

### Code Quality

```bash
cd web
npm run lint        # Run linting
tsc --noEmit        # Type checking
```

### Project Structure

```
web/
├── src/
│   ├── components/     # React components
│   ├── lib/           # Utilities and configuration
│   ├── assets/        # Static assets
│   ├── App.tsx        # Main application component
│   └── main.tsx       # Application entry point
├── dist/              # Build output
├── public/            # Public assets
└── package.json       # Dependencies
```

### Development Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run lint` - Run linting
- `npm run preview` - Preview production build

## Configuration

### Key Configuration Files

- `src/main/resources/application.yml` - Main application configuration
- `src/main/resources/schema.sql` - Database schema
- `web/vite.config.ts` - Frontend build configuration

### Database Schema

The application uses PostgreSQL with the following main entities:
- **Users** - Authentication and role management
- **Providers** - AI service provider configurations
- **Models** - Available AI models with capabilities
- **VendorModels** - Model to provider mappings
- **ApiKeys** - API key management with rate limiting
- **RequestLog** - Request logging and analytics


## License

[MIT License](LICENSE)