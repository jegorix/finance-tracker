# API Documentation

REST API Finance Tracker. Базовый URL: `http://localhost:8080`. Ответы — JSON.

Запуск:

```bash
docker compose up -d --build
```

---

### Users — `/api/v1/users`

- **GET** `/api/v1/users` — список пользователей
- **GET** `/api/v1/users/{id}` — пользователь по id
- **POST** `/api/v1/users` — создать пользователя (привязка к существующим счетам и транзакциям)
- **PATCH** `/api/v1/users/{id}` — обновить пользователя
- **DELETE** `/api/v1/users/{id}` — удалить пользователя
- **POST** `/api/v1/users/with-accounts-and-transactions?transactional=true|false` — создать пользователя вместе с новыми счетами и транзакциями.
  - Дополнительно в body доступен флаг `failAfterAccounts`:
    - `false` (по умолчанию) — обычное создание
    - `true` — искусственная ошибка сразу после сохранения всех счетов, до сохранения транзакций (для проверки атомарности)

---

### Accounts — `/api/v1/accounts`

- **GET** `/api/v1/accounts` — список счетов
- **GET** `/api/v1/accounts/{id}` — счёт по id
- **POST** `/api/v1/accounts` — создать счёт
- **PATCH** `/api/v1/accounts/{id}` — обновить счёт
- **DELETE** `/api/v1/accounts/{id}` — удалить счёт
- Поле `type` для счёта — enum `AccountType`: `CHECKING`, `SAVINGS`, `CREDIT`, `DEBIT`, `INVESTMENT`, `CASH`

---

### Transactions — `/api/v1/transactions`

- **GET** `/api/v1/transactions` — список транзакций (опционально по датам: `startDate`, `endDate`)
- **GET** `/api/v1/transactions?withBudget=true` — список транзакций с подгрузкой `budget` через `EntityGraph`
- **GET** `/api/v1/transactions?withUser=true` — список транзакций с подгрузкой `user` через `EntityGraph`
- **GET** `/api/v1/transactions/{id}` — транзакция по id
- **POST** `/api/v1/transactions` — создать транзакцию (привязка к бюджету, опционально к пользователю через `userId`)
- **PATCH** `/api/v1/transactions/{id}` — обновить транзакцию
- **DELETE** `/api/v1/transactions/{id}` — удалить транзакцию

---

### Categories — `/api/v1/categories`

- **GET** `/api/v1/categories` — список категорий
- **GET** `/api/v1/categories/{id}` — категория по id
- **POST** `/api/v1/categories` — создать категорию
- **PATCH** `/api/v1/categories/{id}` — обновить категорию
- **DELETE** `/api/v1/categories/{id}` — удалить категорию

---

### Budgets — `/api/v1/budgets`

- **GET** `/api/v1/budgets` — список бюджетов (базовый режим)
- **GET** `/api/v1/budgets/{id}` — бюджет по id
- **POST** `/api/v1/budgets` — создать бюджет
- **PATCH** `/api/v1/budgets/{id}` — обновить бюджет
- **DELETE** `/api/v1/budgets/{id}` — удалить бюджет

---

Ответы: 200 (данные), 201 (создано), 204 (удалено). Ошибки: 400 (валидация), 404 (не найдено).
