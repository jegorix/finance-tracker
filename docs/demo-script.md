# Demo Script For Teacher (Postman)

## Postman Setup

1. Создайте Environment `Finance Tracker`.
2. Добавьте переменные:
   - `baseUrl = http://localhost:8080/api/v1`
   - `userId`
   - `account1Id`
   - `account2Id`
   - `budget1Id`
   - `budget2Id`
   - `categoryId`
   - `transactionId`
3. Для всех запросов используйте Header:
   - `Content-Type: application/json`

## 1) Тестовые данные (все сущности + 2 транзакции)

### 1.1 Create User

- Method: `POST`
- URL: `{{baseUrl}}/users`
- Body (raw JSON):

```json
{
  "username": "demo_user",
  "email": "demo_user@example.com"
}
```

Сохраните `id` из ответа в `userId`.

### 1.2 Create Accounts (2 шт)

- Method: `POST`
- URL: `{{baseUrl}}/accounts`
- Body:

```json
{
  "name": "Main Card",
  "type": "DEBIT",
  "balance": 2500.00,
  "userId": {{userId}}
}
```

Сохраните `id` в `account1Id`.

Второй запрос:

```json
{
  "name": "Cash Wallet",
  "type": "CASH",
  "balance": 300.00,
  "userId": {{userId}}
}
```

Сохраните `id` в `account2Id`.

### 1.3 Create Budgets (2 шт)

- Method: `POST`
- URL: `{{baseUrl}}/budgets`
- Body:

```json
{
  "name": "Food March",
  "limitAmount": 800.00,
  "periodStart": "2026-03-01",
  "periodEnd": "2026-03-31",
  "userId": {{userId}}
}
```

Сохраните `id` в `budget1Id`.

Второй запрос:

```json
{
  "name": "Transport March",
  "limitAmount": 400.00,
  "periodStart": "2026-03-01",
  "periodEnd": "2026-03-31",
  "userId": {{userId}}
}
```

Сохраните `id` в `budget2Id`.

### 1.4 Create Categories

- Method: `POST`
- URL: `{{baseUrl}}/categories`
- Body:

```json
{
  "name": "Groceries",
  "userId": {{userId}},
  "budgetIds": [{{budget1Id}}]
}
```

Второй запрос:

```json
{
  "name": "Taxi",
  "userId": {{userId}},
  "budgetIds": [{{budget2Id}}]
}
```

### 1.5 Create Transactions (2 шт)

- Method: `POST`
- URL: `{{baseUrl}}/transactions`
- Body:

```json
{
  "occurredAt": "2026-03-05T12:00:00",
  "amount": 120.50,
  "description": "Weekly groceries",
  "type": "EXPENSE",
  "budgetId": {{budget1Id}},
  "accountId": {{account1Id}}
}
```

Сохраните `id` первой транзакции в `transactionId`.

Второй запрос:

```json
{
  "occurredAt": "2026-03-05T15:30:00",
  "amount": 25.00,
  "description": "Taxi to office",
  "type": "EXPENSE",
  "budgetId": {{budget2Id}},
  "accountId": {{account2Id}}
}
```

### Вопросы для преподавателя (пункт 1)

1. Какие сущности участвуют в модели и как они связаны?
2. Почему для транзакции достаточно `accountId` и `budgetId`?
3. Почему в ответе бюджета возвращаются только его собственные поля и связи?

## 2) CRUD-операции (пример на Category)

### CREATE

- Method: `POST`
- URL: `{{baseUrl}}/categories`
- Body:

```json
{
  "name": "Coffee",
  "userId": {{userId}},
  "budgetIds": [{{budget1Id}}]
}
```

Сохраните `id` в `categoryId`.

### READ

- Method: `GET`
- URL: `{{baseUrl}}/categories/{{categoryId}}`

И общий список:

- Method: `GET`
- URL: `{{baseUrl}}/categories`

### UPDATE

- Method: `PUT`
- URL: `{{baseUrl}}/categories/{{categoryId}}`
- Body:

```json
{
  "name": "Coffee and snacks",
  "userId": {{userId}},
  "budgetIds": [{{budget1Id}}]
}
```

### DELETE

- Method: `DELETE`
- URL: `{{baseUrl}}/categories/{{categoryId}}`

### Дополнительно: PUT для Transaction

- Method: `PUT`
- URL: `{{baseUrl}}/transactions/{{transactionId}}`
- Body:

```json
{
  "occurredAt": "2026-03-08T09:00:00",
  "amount": 200.00,
  "description": "Gambling winnings updated",
  "type": "INCOME",
  "budgetId": {{budget1Id}},
  "accountId": {{account1Id}}
}
```

`PUT` требует полное тело запроса, даже если меняется только одно поле.

### Дополнительно: PATCH для Transaction

- Method: `PATCH`
- URL: `{{baseUrl}}/transactions/{{transactionId}}`
- Body:

```json
{
  "amount": 215.50,
  "description": "Gambling winnings final"
}
```

`PATCH` позволяет отправить только те поля, которые нужно изменить.

### Вопросы для преподавателя (пункт 2)

1. Какие HTTP статусы возвращаются на CRUD?
2. Где и как валидируются входные DTO?
3. Что произойдет со связями при удалении?

## 3) N+1 и решение через `@EntityGraph`

Важно: смотреть SQL-логи приложения (`spring.jpa.show-sql=true`).

### 3.1 Без оптимизации

- Method: `GET`
- URL: `{{baseUrl}}/transactions`

### 3.2 С `EntityGraph`

- Method: `GET`
- URL: `{{baseUrl}}/transactions`
- Query Params:
  - `withEntityGraph = true`

### Вопросы для преподавателя (пункт 3)

1. Что такое N+1?
2. Как `@EntityGraph` уменьшает количество запросов?
3. Чем отличается от `fetch join`?

## 4) Частичное сохранение без транзакции и rollback с транзакцией

Эндпоинт: `/users/create-accounts-and-budgets?transactional=true|false`

### 4.1 Без транзакции (частичное сохранение)

- Method: `POST`
- URL: `{{baseUrl}}/users/create-accounts-and-budgets`
- Query Params:
  - `transactional = false`
- Body:

```json
{
  "username": "no_tx_user",
  "email": "no_tx_user@example.com",
  "failAfterAccounts": true,
  "accounts": [
    { "name": "NoTx Card", "type": "DEBIT", "balance": 1000.00, "userId": {{userId}} }
  ],
  "budgets": [
    {
      "name": "NoTx Budget",
      "limitAmount": 500.00,
      "periodStart": "2026-03-01",
      "periodEnd": "2026-03-31",
      "userId": {{userId}},
      "categoryIds": []
    }
  ]
}
```

Проверка: сделайте `GET /users`, `GET /accounts`, `GET /budgets`.

### 4.2 С транзакцией (полный rollback)

- Method: `POST`
- URL: `{{baseUrl}}/users/create-accounts-and-budgets`
- Query Params:
  - `transactional = true`
- Body:

```json
{
  "username": "tx_user",
  "email": "tx_user@example.com",
  "failAfterAccounts": true,
  "accounts": [
    { "name": "Tx Card", "type": "DEBIT", "balance": 1000.00, "userId": {{userId}} }
  ],
  "budgets": [
    {
      "name": "Tx Budget",
      "limitAmount": 500.00,
      "periodStart": "2026-03-01",
      "periodEnd": "2026-03-31",
      "userId": {{userId}},
      "categoryIds": []
    }
  ]
}
```

Проверка: снова `GET /users`, `GET /accounts`, `GET /budgets`.

### Вопросы для преподавателя (пункт 4)

1. Почему без транзакции данные сохраняются частично?
2. Почему с `@Transactional` выполняется rollback?
3. Какие операции обязательно делать транзакционными в проде?
