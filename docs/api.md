# API Documentation

REST API Finance Tracker. Базовый URL: `http://localhost:8080`. Ответы — JSON.

Запуск:

```bash
./mvnw spring-boot:run
```

---

### Users - `/api/v1/users`

- **GET** `/api/v1/users` — список пользователей
- **GET** `/api/v1/users/{id}` — пользователь по id
- **POST** `/api/v1/users` — создать пользователя
- **PUT** `/api/v1/users/{id}` — обновить пользователя
- **DELETE** `/api/v1/users/{id}` — удалить пользователя
- **POST** `/api/v1/users/create-accounts-and-budgets?transactional=true|false` — создать пользователя вместе с новыми счетами и бюджетами.
  - Дополнительно в body доступен флаг `failAfterAccounts`:
    - `false` (по умолчанию) — обычное создание
    - `true` — искусственная ошибка сразу после сохранения всех счетов, до сохранения бюджетов (для проверки атомарности)
  - В body обязательно передаются массивы `accounts` и `budgets`.

---

### Accounts - `/api/v1/accounts`

- **GET** `/api/v1/accounts` — список счетов
- **GET** `/api/v1/accounts/{id}` — счёт по id
- **POST** `/api/v1/accounts` — создать счёт
- **POST** `/api/v1/accounts/transfer?transactional=true|false` — перевод между двумя счетами.
  - В body передаются `fromAccountId`, `toAccountId`, `amount`
  - Флаг `failAfterDebit`:
    - `false` (по умолчанию) — обычный перевод
    - `true` — искусственная ошибка сразу после списания с исходного счёта, до зачисления на целевой
  - При `transactional=true` списание откатывается вместе со всей операцией
  - При `transactional=false` списание с исходного счёта останется, а зачисление на целевой не выполнится
- **PUT** `/api/v1/accounts/{id}` — обновить счёт
- **DELETE** `/api/v1/accounts/{id}` — удалить счёт
- Поле `type` для счёта — enum `AccountType`: `CHECKING`, `SAVINGS`, `CREDIT`, `DEBIT`, `INVESTMENT`, `CASH`
- Для создания/обновления счёта нужен `userId`

---

### Transactions - `/api/v1/transactions`

- **GET** `/api/v1/transactions` — список транзакций
- **GET** `/api/v1/transactions?startDateTime=2026-03-01T00:00:00&endDateTime=2026-03-31T23:59:59` — фильтр по диапазону даты/времени
- **GET** `/api/v1/transactions?withEntityGraph=true` — список транзакций с `EntityGraph` (`budget`, `account`)
- **GET** `/api/v1/transactions/search?queryMode=JPQL|NATIVE&page=0&size=5&sortBy=occurredAt&ascending=false` — сложный поиск по транзакциям с фильтрацией по вложенным сущностям `budget` и `account`, плюс пагинация
  - Поддерживаемые query params:
    - `budgetName`
    - `accountName`
    - `minAmount`
    - `maxAmount`
    - `startDateTime`
    - `endDateTime`
    - `queryMode` = `JPQL` или `NATIVE`
    - `page`, `size`, `sortBy`, `ascending`
  - В ответе добавляется header `X-Transaction-Search-Source` со значением `DATABASE` или `CACHE`
  - JSON-ответ упрощён и содержит только:
    - `content`
    - `page.size`
    - `page.number`
    - `page.totalElements`
    - `page.totalPages`
- **GET** `/api/v1/transactions/{id}` — транзакция по id
- **POST** `/api/v1/transactions` — создать транзакцию (обязательны `accountId`, `budgetId`, `type`)
- **PUT** `/api/v1/transactions/{id}` — обновить транзакцию
- **PATCH** `/api/v1/transactions/{id}` — частично обновить транзакцию
- **DELETE** `/api/v1/transactions/{id}` — удалить транзакцию
- Поле времени транзакции: `occurredAt` (`LocalDateTime`)
- Поле типа транзакции: `type` (`INCOME`, `EXPENSE`, `TRANSFER`)

---

### Categories - `/api/v1/categories`

- **GET** `/api/v1/categories` — список категорий
- **GET** `/api/v1/categories/{id}` — категория по id
- **POST** `/api/v1/categories` — создать категорию
- **PUT** `/api/v1/categories/{id}` — обновить категорию
- **DELETE** `/api/v1/categories/{id}` — удалить категорию
- Категория принадлежит конкретному пользователю: при создании обязателен `userId`
- Категория существует только в контексте бюджетов этого же пользователя: при создании нужен хотя бы один `budgetId`

---

### Budgets - `/api/v1/budgets`

- **GET** `/api/v1/budgets` — список бюджетов (базовый режим)
- **GET** `/api/v1/budgets/{id}` — бюджет по id
- **POST** `/api/v1/budgets` — создать бюджет
- **PUT** `/api/v1/budgets/{id}` — обновить бюджет
- **PATCH** `/api/v1/budgets/{id}` — частично обновить бюджет
- **DELETE** `/api/v1/budgets/{id}` — удалить бюджет
- Для создания/обновления бюджета нужен `userId`
- Рамки бюджета задаются полями `periodStart` и `periodEnd` (`YYYY-MM-DD`)
- Для `PATCH` можно передавать только изменяемые поля, например только `name` и `limitAmount`

---

Ответы: 200 (данные), 201 (создано), 204 (удалено). Ошибки: 400 (валидация), 404 (не найдено), 409 (конфликт бизнес-правил/уникальности).
