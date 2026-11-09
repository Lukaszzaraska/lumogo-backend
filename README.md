# LumoGo Backend

Backend aplikacji mobilnej LumoGo dla biegaczy i osób aktywnych.

## Stack
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- H2 Database (runtime)

## Uruchomienie
### Windows
1. Otwórz terminal w katalogu projektu
2. Uruchom: `run.bat`

### PowerShell
1. Otwórz terminal w katalogu projektu
2. Uruchom: `powershell -ExecutionPolicy Bypass -File .\run.ps1`

Jeśli `mvnw.cmd` w PowerShell nie działa, użyj `run.bat`.

Aplikacja będzie dostępna pod `http://localhost:8080` (oraz na adresie IP serwera, jeśli jest wystawiona z zewnątrz)

Jeśli chcesz, aby backend był dostępny z zewnątrz, upewnij się, że port 8080 jest otwarty w firewallu i serwer nasłuchuje na `0.0.0.0`.

> Backend jest skonfigurowany do nasłuchiwania na `0.0.0.0`, więc frontend na innym urządzeniu może korzystać z `http://<IP_SERWERA>:8080`.

## Docker i PostgreSQL
1. `docker compose up --build`
2. Backend dostępny pod `http://localhost:8080` (lub inny port jeśli zmienisz zmienną `SERVER_PORT`)
3. Dla dostępu z zewnątrz użyj adresu serwera: `http://<IP_SERWERA>:8080/api/health`
4. Baza PostgreSQL dostępna pod `postgresql://postgres:postgres@localhost:5432/lumogo`

## Ngrok
1. Uruchom backend lokalnie (port 8080) lub w Dockerze (port 8080)
2. Otwórz terminal i wpisz: `ngrok http 8080`
3. Skopiuj wygenerowany adres publiczny, np. `https://xxxxxx.ngrok.io`
4. Frontend może korzystać z tego adresu dla połączeń do API

## Swagger
- UI Swagger: `http://localhost:8080/swagger-ui/index.html`
- API docs: `http://localhost:8080/v3/api-docs`

## Endpoints
- `GET /api/health` - health check
- `GET /api/test` - example test endpoint for frontend integration
- `GET /api/conditions/today?lat={latitude}&lon={longitude}` - today’s weather + air quality summary
- `POST /api/auth/register` - rejestracja nowego użytkownika
  - payload: `{ "firstName": "Anna", "lastName": "Kowalska", "email": "anna@example.com", "password": "Haslo!23", "passwordConfirm": "Haslo!23" }`
  - `lastName` jest opcjonalne
  - hasło: co najmniej 8 znaków, jedna wielka litera i jeden znak specjalny (ta sama reguła co w aplikacji)
  - `409 Conflict`, jeśli konto z tym e-mailem już istnieje
- `POST /api/auth/login` - logowanie użytkownika
  - payload: `{ "email": "anna@example.com", "password": "Haslo!23" }`
- `GET /api/interests/options` - lista dostępnych zainteresowań do wyboru
- `GET /api/interests/user?email=anna@example.com` - pobierz zapisane zainteresowania użytkownika
- `POST /api/interests` - zapisanie wybranych zainteresowań po rejestracji
  - payload: `{ "email": "anna@example.com", "interests": ["Bieganie", "Spacer", "Trekking"] }`

