# Finance Tracker

**Finance Tracker** — это приложение для управления личными финансами, которое помогает пользователям контролировать доходы и расходы, а также анализировать своё финансовое состояние. Система предоставляет REST API для работы с данными о пользователях, счетах, транзакциях, категориях и бюджетах, обеспечивая удобное и структурированное взаимодействие с финансовой информацией.

**Стек:** Java 17 · Spring Boot 4 · Spring Data JPA · PostgreSQL

---

## Связи сущностей

![ER diagram](./docs/ER-diagram.png)

---

## API Endpoints

Полная документация API находится: [Документация API](docs/api.md)

## Запуск приложения

### 1. Поднимите PostgreSQL локально

Убедитесь, что PostgreSQL запущен локально на `localhost:5432`.

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

Если PostgreSQL не запущен или недоступен на `localhost:5432`, приложение завершится на старте, потому что Liquibase применяет миграции при инициализации.

## Транзакции и Entity Graph

**Транзакции:** создание пользователя с несколькими счетами и бюджетами (`POST /api/v1/users/create-accounts-and-budgets`) можно выполнять в одной транзакции (`?transactional=true`) — при ошибке всё откатывается; без транзакции (`?transactional=false`) при сбое в БД остаются частично сохранённые данные. Для демонстрации атомарности есть флаг `failAfterAccounts=true` в теле запроса: приложение падает сразу после сохранения всех счетов, до сохранения бюджетов.

**Модель транзакций:** у транзакций используется `occurredAt` (`LocalDateTime`), денежные поля переведены на `BigDecimal/NUMERIC(19,2)`, добавлены типы операций (`INCOME`, `EXPENSE`, `TRANSFER`) и обязательная связь с аккаунтом.

**Модель бюджетов:** у бюджетов есть временные рамки `periodStart`/`periodEnd`, и расчёт `spent` выполняется только по расходам (`EXPENSE`) внутри этого интервала.

**Entity Graph:** для демонстрации N+1 в транзакциях используется `@EntityGraph` для списка транзакций:
`GET /api/v1/transactions?withEntityGraph=true`.
