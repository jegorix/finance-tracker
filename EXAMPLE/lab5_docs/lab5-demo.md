# Как демонстрировать лабораторную работу 5

## Что открыть заранее

Для демонстрации удобно держать открытыми:

1. терминал с запущенным приложением;
2. второй терминал для запуска тестов;
3. Postman;
4. Swagger UI;
5. клиент для PostgreSQL, например DBeaver или pgAdmin.

---

## 1. Подготовка окружения

### 1.1. Убедиться, что PostgreSQL запущен

В проекте используется локальная база `finance_tracker`, поэтому до демонстрации она должна быть доступна.

### 1.2. Запустить приложение

В терминале:

```bash
./mvnw spring-boot:run
```

После запуска приложение будет доступно по адресу:

- `http://localhost:8080`

### 1.3. Открыть Swagger UI

В браузере:

- `http://localhost:8080/swagger-ui.html`

Что показать:

- наличие endpoint `POST /api/v1/transactions/bulk`;
- описание параметра `transactional`;
- описание DTO `TransactionRequest`.

### 1.4. Настроить Postman environment

Создай environment, например `Finance Tracker Lab5`, и добавь переменные:

- `baseUrl = http://localhost:8080/api/v1`
- `userId`
- `accountId`
- `budgetId`

---

## 2. Подготовка тестовых данных

Перед демонстрацией bulk-операции удобно создать одного пользователя, один счёт и один бюджет.

### 2.1. Создать пользователя

Запрос:

```http
POST {{baseUrl}}/users
Content-Type: application/json
```

```json
{
  "username": "lab5_user",
  "email": "lab5_user@example.com"
}
```

Из ответа сохрани `id` в переменную `userId`.

### 2.2. Создать счёт

Запрос:

```http
POST {{baseUrl}}/accounts
Content-Type: application/json
```

```json
{
  "name": "Main Card",
  "type": "DEBIT",
  "balance": 1000.00,
  "userId": {{userId}}
}
```

Из ответа сохрани `id` в переменную `accountId`.

### 2.3. Создать бюджет

Запрос:

```http
POST {{baseUrl}}/budgets
Content-Type: application/json
```

```json
{
  "name": "March Budget",
  "limitAmount": 1500.00,
  "periodStart": "2026-03-01",
  "periodEnd": "2026-03-31",
  "userId": {{userId}}
}
```

Из ответа сохрани `id` в переменную `budgetId`.

---

## 3. Демонстрация тестов

### Что сделать

Во втором терминале выполнить:

```bash
./mvnw test
```

### Что показать преподавателю

- тесты проходят успешно;
- проверяется сервисный слой;
- в тестах используются `JUnit 5` и `Mockito`;
- среди сценариев есть проверка bulk-операции и обработка ошибок.

### Подпись в отчёте

`Рисунок 4.1 – Результат выполнения тестов`

---

# 5 РЕЗУЛЬТАТ РАБОТЫ ПРОГРАММЫ

## 5.1. Демонстрация корректного bulk-запроса

### Что отправить в Postman

```http
POST {{baseUrl}}/transactions/bulk?transactional=true
Content-Type: application/json
```

```json
[
  {
    "occurredAt": "2026-03-19T10:00:00",
    "amount": 250.00,
    "description": "Bulk salary part",
    "type": "INCOME",
    "accountId": {{accountId}}
  },
  {
    "occurredAt": "2026-03-19T12:30:00",
    "amount": 45.50,
    "description": "Bulk groceries",
    "type": "EXPENSE",
    "budgetId": {{budgetId}},
    "accountId": {{accountId}}
  }
]
```

### Что должно получиться

- статус `201 Created`;
- в ответе приходит список из двух созданных транзакций;
- у второй транзакции заполнен `budgetId`;
- обе записи сохраняются в БД.

### Как проверить состояние БД

В DBeaver или pgAdmin выполнить:

```sql
SELECT id, description, amount, type, budget_id, account_id
FROM transactions
WHERE description IN ('Bulk salary part', 'Bulk groceries')
ORDER BY id;
```

### Что сказать преподавателю

- bulk-операция принимает список объектов;
- все элементы обрабатываются одним endpoint;
- результат возвращается списком `TransactionResponse`.

### Подпись в отчёте

`Рисунок 5.1 – Демонстрация bulk запроса`

---

## 5.2. Демонстрация bulk-запроса с инициированной ошибкой

Для этого пункта нужно показать разницу между режимом с `@Transactional` и без общей транзакции.

### Шаг 1. Запрос с ошибкой в режиме `transactional=true`

Отправь:

```http
POST {{baseUrl}}/transactions/bulk?transactional=true
Content-Type: application/json
```

```json
[
  {
    "occurredAt": "2026-03-19T15:00:00",
    "amount": 70.00,
    "description": "Rollback demo item",
    "type": "EXPENSE",
    "budgetId": {{budgetId}},
    "accountId": {{accountId}}
  },
  {
    "occurredAt": "2026-03-19T16:00:00",
    "amount": 15.00,
    "description": "Rollback broken item",
    "type": "EXPENSE",
    "accountId": 999999
  }
]
```

### Что должно получиться

- статус `404 Not Found`;
- ошибка вида `Account not found: 999999`.

### Что показать по БД после этого

Выполни SQL:

```sql
SELECT id, description, amount, type, budget_id, account_id
FROM transactions
WHERE description IN ('Rollback demo item', 'Rollback broken item')
ORDER BY id;
```

Ожидаемый результат:

- запрос не должен вернуть строк;
- первая транзакция тоже не сохранится, потому что вся bulk-операция была в одной транзакции.

### Шаг 2. Тот же сценарий, но в режиме `transactional=false`

Отправь:

```http
POST {{baseUrl}}/transactions/bulk?transactional=false
Content-Type: application/json
```

```json
[
  {
    "occurredAt": "2026-03-19T17:00:00",
    "amount": 80.00,
    "description": "NoTx demo item",
    "type": "EXPENSE",
    "budgetId": {{budgetId}},
    "accountId": {{accountId}}
  },
  {
    "occurredAt": "2026-03-19T18:00:00",
    "amount": 20.00,
    "description": "NoTx broken item",
    "type": "EXPENSE",
    "accountId": 999999
  }
]
```

### Что должно получиться

- статус снова будет `404 Not Found`;
- но состояние БД уже будет другим.

### Что показать по БД после `transactional=false`

Выполни SQL:

```sql
SELECT id, description, amount, type, budget_id, account_id
FROM transactions
WHERE description IN ('NoTx demo item', 'NoTx broken item')
ORDER BY id;
```

Ожидаемый результат:

- строка `NoTx demo item` будет в таблице;
- строки `NoTx broken item` не будет;
- это и есть демонстрация частичного сохранения без общей транзакции.

### Что сказать преподавателю

- `createBulkTx(...)` работает с `@Transactional`, поэтому операция атомарна;
- `createBulkNoTx(...)` работает без общей транзакции;
- при ошибке одного элемента без транзакции система сохраняет уже успешно обработанные записи.

### Подпись в отчёте

`Рисунок 5.2 – Демонстрация bulk запроса с инициированной ошибкой`

---

## 6. Что кратко проговорить на защите

Можно сформулировать так:

1. В проект добавлена bulk-операция массового создания транзакций.
2. В сервисном слое использованы `Optional` и `Stream API`.
3. Реализованы два режима bulk-обработки: с транзакцией и без неё.
4. Разница между режимами подтверждается фактическим состоянием таблицы `transactions`.
5. Для сервисного слоя написаны unit-тесты на `JUnit` и `Mockito`.
