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

Endpointy oznaczone 🔒 wymagają nagłówka `Authorization: Bearer <token>`.
Token zwracają `register` i `login` (ważny 30 dni, `JWT_TTL_DAYS`). Użytkownik jest brany z tokena,
nie z parametru `email`.

- `GET /api/health` – health check
- `GET /api/conditions/today?lat={lat}&lon={lon}` – bieżąca pogoda, AQI i ocena 0–100
- `GET /api/conditions/best-window?lat={lat}&lon={lon}&date=2026-11-10&durationMinutes=30` – najlepsze okno
  na aktywność w danym dniu (od dziś do 4 dni naprzód, start o pełnej godzinie 6:00–22:00, uwzględnia szansę opadów)
- `POST /api/auth/register` – rejestracja, zwraca token oraz `id` i `gender`
  - `{ "email": "anna@example.com", "firstName": "Anna", "gender": "Woman", "password": "Haslo!23" }`
  - `lastName`, `gender` i `passwordConfirm` opcjonalne (`gender: "-"` = nie podano);
    hasło: min. 8 znaków, wielka litera, znak specjalny
- `POST /api/auth/login` – `{ "email": "anna@example.com", "password": "Haslo!23" }`, zwraca token
- 🔒 `GET /api/auth/profile`, 🔒 `PUT /api/auth/profile` (`firstName`, `lastName`, `gender`) – profil
- 🔒 `POST /api/auth/change-password` – `{ "currentPassword": "...", "newPassword": "..." }`, 204
- 🔒 `DELETE /api/auth/account` – usuwa konto, plany i preferencje (wymóg Google Play), 204
- 🔒 `GET /api/plans`, 🔒 `POST /api/plans`, 🔒 `DELETE /api/plans/{id}` – zapisane plany (tylko własne)
- 🔒 `GET /api/preferences`, 🔒 `PUT /api/preferences` – `{ "defaultDuration": 30, "favoriteActivities": "walking,running" }`
- `GET /api/content/faq`, `GET /api/content/privacy` – treści z `src/main/resources/content/*.json`
  (polityka prywatności to **szkic do weryfikacji**: uzupełnij administratora danych)
- `GET /api/interests/options` – lista zainteresowań
- 🔒 `GET /api/interests/user`, 🔒 `POST /api/interests` (`{ "interests": ["Bieganie", "Spacer"] }`)
- 🔒 `GET /api/widget/status`

Błędy mają pole `code` do tłumaczenia w aplikacji: `EMAIL_TAKEN` (409), `INVALID_CREDENTIALS` (401),
`VALIDATION_ERROR` (400), `USER_NOT_FOUND` (404), `NO_CHANGES` (400). Pole `message` jest po polsku.

## Zmienne środowiskowe

| Zmienna | Domyślnie | Uwagi |
|---|---|---|
| `JWT_SECRET` | sekret deweloperski | **na serwerze obowiązkowo własny**, min. 32 znaki (`openssl rand -base64 48`) |
| `JWT_TTL_DAYS` | 30 | ważność tokena |
| `SPRING_DATASOURCE_*` | H2 w pamięci | Postgres: patrz docker-compose |
| `H2_CONSOLE_ENABLED` | false | tylko lokalnie |
