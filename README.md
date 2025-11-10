# 🎮 Bomber Dino Backend

<div align="center">
  <img src="/assets/images/bomberdino-logo.png" alt="Bomber Dino Logo" width="35%">
</div>

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-Real--Time-010101?style=for-the-badge&logo=socketdotio&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

</div>

---

## 📋 **Table of Contents**

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [Testing](#-testing)
- [Project Structure](#-project-structure)
- [Team Members](#-team-members)
- [License](#-license)
- [Additional Resources](#-additional-resources)

---

## 🌟 **Overview**

**Bomber Dino Backend** is a high-performance *RESTful API* and *WebSocket server* designed to power a real-time multiplayer game experience. Built with **Spring Boot 3.2.2** and **Java 17**, this backend service provides robust game state management, player synchronization, and secure authentication mechanisms.

The system supports multiple concurrent game sessions with low-latency communication, ensuring smooth gameplay for competitive multiplayer matches.

---

## ✨ **Features**

### 🔐 **Security & Authentication**
- **JWT-based authentication** with token refresh mechanisms
- **Role-based access control** (RBAC) for different user types
- **Secure password hashing** using industry-standard algorithms
- **CORS configuration** for controlled frontend access

### 🎮 **Game Management**
- **Real-time game state synchronization** via WebSocket
- **Concurrent session handling** for multiple game rooms
- **Player matchmaking** and lobby system
- **Game statistics tracking** and leaderboard support

### 🚀 **Performance & Scalability**
- **Asynchronous processing** for non-blocking operations
- **Connection pooling** for optimized database access
- **Health monitoring** with Spring Boot Actuator
- **Profile-based configuration** (development, staging, production)

### 📊 **Monitoring & Quality**
- **Comprehensive error handling** with global exception management
- **Structured logging** with correlation IDs
- **Code coverage** tracking with JaCoCo (80%+ target)
- **API documentation** with Swagger/OpenAPI

---

## 🛠️ **Tech Stack**

### **Backend Framework**
- **Spring Boot** `3.2.2` - Enterprise-grade Java framework
- **Spring Web** - RESTful API development
- **Spring WebSocket** - Real-time bidirectional communication
- **Spring Data JPA** - Database abstraction layer

### **Development Tools**
- **Lombok** - Boilerplate code reduction
- **ModelMapper** `3.2.2` - Object mapping utilities
- **Spring Boot DevTools** - Hot reload for development

### **API Documentation**
- **SpringDoc OpenAPI** `2.3.0` - Swagger UI integration

### **Testing & Quality**
- **JUnit 5** - Unit testing framework
- **Spring Boot Test** - Integration testing support
- **JaCoCo** `0.8.12` - Code coverage analysis
- **SonarQube** - Code quality and security analysis

### **Build & Deployment**
- **Maven** `3.9+` - Dependency management and build automation
- **Azure DevOps** - CI/CD pipeline integration

---

## 🏗️ **Architecture**

The project follows a **layered architecture** pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────┐
│         Frontend (React/TypeScript)         │
│           WebSocket Client + REST           │
└───────────────┬─────────────────────────────┘
                │ HTTPS + WSS
                ↓
┌─────────────────────────────────────────────┐
│       Spring Boot Backend (Java 17)         │
│  ┌───────────────────────────────────────┐  │
│  │    Controller Layer (REST + WS)       │  │
│  └──────────────┬────────────────────────┘  │
│                 ↓                           │
│  ┌───────────────────────────────────────┐  │
│  │      Service Layer (Business Logic)   │  │
│  └──────────────┬────────────────────────┘  │
│                 ↓                           │
│  ┌───────────────────────────────────────┐  │
│  │   Repository Layer (Data Access)      │  │
│  └───────────────────────────────────────┘  │
└───────────────┬─────────────────────────────┘
                │
                ↓
┌─────────────────────────────────────────────┐
│         Database (PostgreSQL/MySQL)         │
└─────────────────────────────────────────────┘
```

### **Component Responsibilities**

| Layer | Components | Purpose |
|-------|-----------|---------|
| **Controller Layer** | `controller/rest/`, `controller/websocket/` | Handle HTTP requests and WebSocket connections |
| **Service Layer** | `service/`, `service/impl/` | Business logic, game rules, validation |
| **Repository Layer** | `repository/` | Database operations, JPA queries |
| **Model Layer** | `model/entity/`, `model/dto/`, `model/enums/` | Data structures and transfer objects |
| **Security Layer** | `security/` | JWT handling, authentication filters |
| **Config Layer** | `config/` | Application configuration, CORS, WebSocket setup |

---

## 📦 **Prerequisites**

Before installation, ensure you have the following installed:

- **Java Development Kit (JDK)** `17` or higher - [Download Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)
- **Apache Maven** `3.9+` - [Installation Guide](https://maven.apache.org/install.html)
- **Git** - [Download](https://git-scm.com/downloads)
- **IDE** (recommended): [IntelliJ IDEA](https://www.jetbrains.com/idea/) or [Eclipse](https://www.eclipse.org/)

### **Optional Tools**
- **Postman** - API testing - [Download](https://www.postman.com/downloads/)
- **Docker** - Containerization - [Download](https://www.docker.com/get-started/)

---

## 🚀 **Installation**

### **1️⃣ Clone the Repository**

```bash
git clone https://github.com/your-org/BomberDino-Backend.git
cd BomberDino-Backend
```

### **2️⃣ Verify Java Installation**

```bash
java -version
# Expected output: openjdk version "17.x.x" or higher
```

### **3️⃣ Verify Maven Installation**

```bash
mvn -version
# Expected output: Apache Maven 3.9.x or higher
```

### **4️⃣ Install Dependencies**

```bash
mvn clean install
```

This command will:
- Download all required dependencies
- Compile the source code
- Run unit tests
- Generate the JAR package

---

## ⚙️ **Configuration**

### **1️⃣ Create Environment File**

Copy the example environment file:

```bash
cp .env.example .env
```

### **2️⃣ Configure Application Profiles**

**File**: `src/main/resources/application-dev.yml`

```yaml
# Development Configuration
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bomber_dino_dev
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

# JWT Configuration
jwt:
  secret: your-secret-key-min-32-characters
  expiration: 86400000  # 24 hours in milliseconds

# CORS Configuration
cors:
  allowed-origins: http://localhost:3000,http://localhost:5173
```

**File**: `src/main/resources/application-prod.yml`

```yaml
# Production Configuration
server:
  port: ${PORT:8080}

spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

# JWT Configuration
jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:3600000}  # 1 hour default

# CORS Configuration
cors:
  allowed-origins: ${ALLOWED_ORIGINS}
```

### **🔒 Generate JWT Secret Key**

**Windows (PowerShell):**
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

**macOS/Linux:**
```bash
openssl rand -base64 32
```

---

## ▶️ **Running the Application**

### **Development Mode**

Start the Spring Boot application with the **development profile**:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API will be available at:
- **API Base URL:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **Health Check:** `http://localhost:8080/actuator/health`

### **Production Mode**

Run with the **production profile**:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### **Running the JAR File**

After building the project, run the packaged JAR:

```bash
java -jar target/backend-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### **Debug Mode**

Enable remote debugging on port 5005:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

Connect your IDE debugger to `localhost:5005`.

---

## 📖 **API Documentation**

### **Interactive Documentation**

Once the server is running, access the interactive API documentation:

- **Swagger UI** (OpenAPI 3.0): `http://localhost:8080/swagger-ui/index.html`
  - Test endpoints directly in the browser
  - View request/response schemas
  - See authentication requirements

### **Available Endpoints**

#### **🔐 Authentication**
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/v1/auth/register` | Register new user account | ❌ No |
| `POST` | `/api/v1/auth/login` | Login and receive JWT token | ❌ No |
| `POST` | `/api/v1/auth/refresh` | Refresh expired JWT token | ✅ Yes |
| `POST` | `/api/v1/auth/logout` | Invalidate current token | ✅ Yes |

#### **🎮 Game Management**
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/v1/games/create` | Create new game room | ✅ Yes |
| `GET` | `/api/v1/games/{id}` | Get game details | ✅ Yes |
| `POST` | `/api/v1/games/{id}/join` | Join existing game | ✅ Yes |
| `DELETE` | `/api/v1/games/{id}/leave` | Leave game room | ✅ Yes |
| `GET` | `/api/v1/games/active` | List active game rooms | ✅ Yes |

#### **🔌 WebSocket Endpoints**
| Endpoint | Description | Protocol |
|----------|-------------|----------|
| `/ws/game` | Real-time game state updates | WebSocket |
| `/ws/lobby` | Lobby and matchmaking | WebSocket |

### **WebSocket Message Examples**

**Client → Server (Player Movement):**
```json
{
  "type": "PLAYER_MOVE",
  "playerId": "user123",
  "position": {
    "x": 150,
    "y": 200
  },
  "timestamp": 1697123456789
}
```

**Server → Client (Game State Update):**
```json
{
  "type": "GAME_STATE_UPDATE",
  "gameId": "game456",
  "players": [
    {
      "id": "user123",
      "position": { "x": 150, "y": 200 },
      "health": 100
    }
  ],
  "timestamp": 1697123456790
}
```

---

## 🧪 **Testing**

### **Run All Tests**

```bash
mvn test
```

### **Run Tests with Coverage**

```bash
mvn clean test jacoco:report
```

View the coverage report at: `target/site/jacoco/index.html`

### **Run Specific Test Class**

```bash
mvn test -Dtest=UserServiceTest
```

### **Skip Tests During Build**

```bash
mvn clean install -DskipTests
```

### **Test Coverage Requirements**

The project enforces **80% minimum code coverage** for:
- Service layer methods
- Controller endpoints
- Business logic components

**Excluded from coverage:**
- DTOs and model classes
- Configuration classes
- Main application class
- Security filters

---

## 📂 **Project Structure**

```
BomberDino-Backend/
│
├── assets/
│   ├── docs/                        # Project documentation
│   ├── images/                      # Diagrams and screenshots
│   └── videos/                      # Demo videos
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── arsw/
│   │   │           └── bomberdino/
│   │   │               │
│   │   │               ├── BomberDinoApplication.java
│   │   │               │
│   │   │               ├── config/
│   │   │               │   ├── AsyncConfig.java
│   │   │               │   ├── CorsConfig.java
│   │   │               │   ├── JacksonConfig.java
│   │   │               │   ├── SecurityConfig.java
│   │   │               │   └── WebSocketConfig.java
│   │   │               │
│   │   │               ├── controller/
│   │   │               │   ├── rest/
│   │   │               │   │   └── v1/
│   │   │               │   └── websocket/
│   │   │               │
│   │   │               ├── exception/
│   │   │               │   ├── BusinessException.java
│   │   │               │   ├── GlobalExceptionHandler.java
│   │   │               │   ├── ResourceNotFoundException.java
│   │   │               │   └── ValidationException.java
│   │   │               │
│   │   │               ├── model/
│   │   │               │   ├── dto/
│   │   │               │   │   ├── request/
│   │   │               │   │   └── response/
│   │   │               │   ├── entity/
│   │   │               │   └── enums/
│   │   │               │
│   │   │               ├── repository/
│   │   │               │
│   │   │               ├── security/
│   │   │               │   ├── JwtAuthenticationFilter.java
│   │   │               │   ├── JwtTokenProvider.java
│   │   │               │   └── UserDetailsServiceImpl.java
│   │   │               │
│   │   │               ├── service/
│   │   │               │   └── impl/
│   │   │               │
│   │   │               ├── util/
│   │   │               │   ├── Constants.java
│   │   │               │   └── DateUtils.java
│   │   │               │
│   │   │               └── validation/
│   │   │                   ├── annotation/
│   │   │                   └── validator/
│   │   │
│   │   └── resources/
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── arsw/
│                   └── bomberdino/
│                       ├── TestBomberDinoApplication.java
│                       ├── controller/
│                       ├── integration/
│                       ├── repository/
│                       ├── service/
│                       └── util/
│
├── .env.example                     # Environment variables template
├── .gitignore                       # Git ignore rules
├── LICENSE                          # MIT License
├── pom.xml                          # Maven dependencies
└── README.md                        # This file
```

### **Key Files Explained**

| File | Purpose |
|------|---------|
| `BomberDinoApplication.java` | Spring Boot application entry point |
| `config/WebSocketConfig.java` | WebSocket endpoint configuration |
| `config/SecurityConfig.java` | JWT authentication and authorization |
| `config/CorsConfig.java` | Cross-origin resource sharing setup |
| `exception/GlobalExceptionHandler.java` | Centralized exception handling |
| `security/JwtTokenProvider.java` | JWT token generation and validation |
| `util/Constants.java` | Application-wide constant values |

---

## 🛡️ **Security Considerations**

### **Authentication Flow**

1. User sends credentials to `/api/v1/auth/login`
2. Server validates credentials and generates JWT token
3. Client stores token (recommended: memory, not localStorage)
4. Client includes token in `Authorization: Bearer <token>` header
5. Server validates token on each protected endpoint request

### **Best Practices Implemented**

- ✅ **JWT tokens** with configurable expiration
- ✅ **Password hashing** with BCrypt (cost factor: 12)
- ✅ **CORS** restricted to allowed origins only
- ✅ **Input validation** with Bean Validation annotations
- ✅ **SQL injection prevention** via JPA parameterized queries
- ✅ **Rate limiting** on authentication endpoints
- ✅ **Secure headers** (HSTS, X-Frame-Options, CSP)

---

## 🚢 **Deployment**

### **Azure App Service Deployment**

**Prerequisites:**
- Azure CLI installed
- Azure subscription active

**Steps:**

1. **Build the application:**
```bash
mvn clean package -DskipTests
```

2. **Create App Service (Linux):**
```bash
az webapp create \
  --resource-group bomber-dino-rg \
  --plan bomber-dino-plan \
  --name bomber-dino-backend \
  --runtime "JAVA:17-java17"
```

3. **Configure environment variables:**
```bash
az webapp config appsettings set \
  --resource-group bomber-dino-rg \
  --name bomber-dino-backend \
  --settings \
    SPRING_PROFILES_ACTIVE=prod \
    DATABASE_URL=$DB_URL \
    JWT_SECRET=$JWT_SECRET
```

4. **Deploy JAR file:**
```bash
az webapp deploy \
  --resource-group bomber-dino-rg \
  --name bomber-dino-backend \
  --src-path target/backend-1.0-SNAPSHOT.jar \
  --type jar
```

### **Docker Deployment**

**File**: `Dockerfile` (create in project root)

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/backend-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

**Build and run:**
```bash
# Build Docker image
docker build -t bomber-dino-backend:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=$DB_URL \
  -e JWT_SECRET=$JWT_SECRET \
  --name bomber-dino-backend \
  bomber-dino-backend:latest
```

---

## 🐛 **Common Issues & Solutions**

### **Issue: Port 8080 already in use**

**Solution:**
```bash
# Find process using port 8080
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # macOS/Linux

# Kill the process (replace PID)
taskkill /PID <PID> /F        # Windows
kill -9 <PID>                 # macOS/Linux
```

### **Issue: Maven build fails with "package does not exist"**

**Solution:**
```bash
mvn clean install -U
```

The `-U` flag forces Maven to update dependencies.

### **Issue: WebSocket connection refused**

**Checklist:**
1. Verify `WebSocketConfig.java` has correct endpoint mapping
2. Check CORS configuration includes WebSocket origins
3. Ensure client uses `ws://` (dev) or `wss://` (prod) protocol
4. Confirm firewall allows WebSocket traffic

### **Issue: JWT token expired immediately**

**Solution:**

Check `application.yml` JWT expiration configuration:
```yaml
jwt:
  expiration: 86400000  # 24 hours in milliseconds
```

---

## 👥 **Team Members**

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/hakki17">
        <img src="https://github.com/hakki17.png" width="100px;" alt="Maria Paula Sánchez Macías"/>
        <br />
        <sub><b>Maria Paula Sánchez Macías</b></sub>
      </a>
      <br />
      <sub>Full-Stack Developer</sub>
    </td>
    <td align="center">
      <a href="https://github.com/JAPV-X2612">
        <img src="https://github.com/JAPV-X2612.png" width="100px;" alt="Jesús Alfonso Pinzón Vega"/>
        <br />
        <sub><b>Jesús Alfonso Pinzón Vega</b></sub>
      </a>
      <br />
      <sub>Backend Developer</sub>
    </td>
    <td align="center">
      <a href="https://github.com/JuanEstebanMedina">
        <img src="https://github.com/JuanEstebanMedina.png" width="100px;" alt="Juan Esteban Medina Rivas"/>
        <br />
        <sub><b>Juan Esteban Medina Rivas</b></sub>
      </a>
      <br />
      <sub>Frontend Developer</sub>
    </td>
  </tr>
</table>

---

## 📄 **License**

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🔗 **Additional Resources**

### **Official Documentation**
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring WebSocket Guide](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io - JSON Web Tokens](https://jwt.io/introduction)

### **Tools & Libraries**
- [Maven Central Repository](https://mvnrepository.com/)
- [Lombok Documentation](https://projectlombok.org/features/)
- [JaCoCo Maven Plugin](https://www.eclemma.org/jacoco/trunk/doc/maven.html)
- [SpringDoc OpenAPI](https://springdoc.org/)

### **Best Practices**
- [Baeldung Spring Boot Tutorials](https://www.baeldung.com/spring-boot)
- [Spring Boot Best Practices](https://springframework.guru/spring-boot-best-practices/)
- [RESTful API Design Best Practices](https://restfulapi.net/)
- [WebSocket Security Best Practices](https://owasp.org/www-community/websockets)

### **Community**
- [Stack Overflow - Spring Boot](https://stackoverflow.com/questions/tagged/spring-boot)
- [Spring Community Forum](https://community.spring.io/)
- [GitHub - Spring Projects](https://github.com/spring-projects)

---

<div align="center">
  <p>Made with ❤️ for multiplayer gaming enthusiasts</p>
  <p>🌟 Star this repository if you find it helpful!</p>
</div>

