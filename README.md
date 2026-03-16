# Finance Tracker

**Finance Tracker** — REST API для учета личных финансов: пользователи, счета, бюджеты, теги, транзакции и переводы между счетами.

**Стек:** Java 21, Spring Boot 4.0.3, Spring Web MVC, Spring Data JPA, PostgreSQL, Liquibase, springdoc-openapi.

## Что умеет сервис

- CRUD для `users`, `accounts`, `budgets`, `categories`, `transactions`
- перевод денег между счетами одного пользователя через `/api/v1/account/transfer`
- поиск пользователей по типу счета и диапазону бюджета через JPQL и native SQL
- фильтрация транзакций по диапазону дат
- пагинация и сортировка бюджетов
- единый формат ошибок, аспектное логирование сервисов и простой in-memory cache для чтения пользователей и поисковых запросов

## API

Полная документация: [docs/api.md](docs/api.md)

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Base URL: `http://localhost:8080`

Основные группы endpoint'ов:

- `/api/v1/users`
- `/api/v1/accounts`
- `/api/v1/account/transfer`
- `/api/v1/budgets`
- `/api/v1/categories`
- `/api/v1/transactions`

## Формат ошибок

Все ошибки API возвращаются в одном из двух JSON-форматов.

Validation error:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-03-10T12:00:00",
  "errors": {
    "username": "must not be blank"
  }
}
```

Обычная бизнес-ошибка:

```json
{
  "status": 404,
  "message": "User not found 1",
  "timestamp": "2026-03-10T12:00:00"
}
```

Дополнительно сервис возвращает:

- `Invalid value '...' for parameter '...'` для некорректных query/path параметров
- `Invalid value for field '...'` для невалидных enum/date значений в JSON
- `Malformed JSON request` для поврежденного JSON


## API Endpoints

Полная документация API находится: [Документация API](docs/api.md)

## Запуск приложения

### 1. Поднимите PostgreSQL локально

Создайте БД:

```sql
CREATE DATABASE finance_tracker;
```

Убедитесь, что доступы совпадают с `src/main/resources/application.properties`:
- `spring.datasource.url=jdbc:postgresql://localhost:5432/finance_tracker`
- `spring.datasource.username=postgres`
- `spring.datasource.password=postgres`

### 2. Запустите приложение

```bash
./mvnw spring-boot:run
```

## Транзакции и Entity Graph

**Транзакции:** создание пользователя с несколькими счетами и бюджетами (`POST /api/v1/users/create-accounts-and-budgets`) можно выполнять в одной транзакции (`?transactional=true`) — при ошибке всё откатывается; без транзакции (`?transactional=false`) при сбое в БД остаются частично сохранённые данные. Для демонстрации атомарности есть флаг `failAfterAccounts=true` в теле запроса: приложение падает сразу после сохранения всех счетов, до сохранения бюджетов.

**Модель транзакций:** у транзакций используется `occurredAt` (`LocalDateTime`), денежные поля переведены на `BigDecimal/NUMERIC(19,2)`, добавлены типы операций (`INCOME`, `EXPENSE`, `TRANSFER`) и обязательная связь с аккаунтом.

**Модель бюджетов:** у бюджетов есть временные рамки `periodStart`/`periodEnd`, и расчёт `spent` выполняется только по расходам (`EXPENSE`) внутри этого интервала.

**Entity Graph:** для демонстрации N+1 в транзакциях используется `@EntityGraph` для списка транзакций:
`GET /api/v1/transactions?withEntityGraph=true`.
