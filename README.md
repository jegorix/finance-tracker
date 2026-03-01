# Finance Tracker

**Finance Tracker** — это приложение для управления личными финансами, которое помогает пользователям контролировать доходы и расходы, а также анализировать своё финансовое состояние. Система предоставляет REST API для работы с данными о пользователях, счетах, транзакциях, категориях и бюджетах, обеспечивая удобное и структурированное взаимодействие с финансовой информацией.

**Стек:** Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL

---

## Связи сущностей

![ER diagram](./docs/ER-diagram.png)

---

## API Endpoints

Полная документация API находится: [Документация API](docs/api.md)

## Запуск приложения

### 1. Подготовьте `.env`

Создайте в корне проекта файл `.env`:

```env
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=finance_tracker
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
```

### 2. Запустите приложение

```bash
docker compose up -d --build
```

### 3. Проверьте, что API поднялось

```bash
curl http://localhost:8080/api/v1/users
```

После запуска API доступно по адресу: `http://localhost:8080`.

### 4. Полезные команды

Логи:

```bash
docker compose logs -f
```

Остановка:

```bash
docker compose down
```

## Транзакции и Entity Graph

**Транзакции:** создание пользователя с несколькими счетами и транзакциями (`POST /api/v1/users/with-accounts-and-transactions`) можно выполнять в одной транзакции (`?transactional=true`) — при ошибке всё откатывается; без транзакции (`?transactional=false`) при сбое в БД остаются частично сохранённые данные. Для демонстрации атомарности есть флаг `failAfterAccounts=true` в теле запроса: приложение падает сразу после сохранения всех счетов, до сохранения транзакций.

**Типы счетов:** поле `Account.type` переведено на enum `AccountType` со значениями `CHECKING`, `SAVINGS`, `CREDIT`, `DEBIT`, `INVESTMENT`, `CASH`.

**Entity Graph:** для демонстрации N+1 в транзакциях используется `@EntityGraph` для списка транзакций:
`GET /api/v1/transactions?withBudget=true` и `GET /api/v1/transactions?withUser=true`.

## SonarQube Cloud

Ссылка на [Sonar Analysis](https://sonarcloud.io/summary/new_code?id=ekuzm_FinanceTracker)