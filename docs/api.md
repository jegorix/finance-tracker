# API Documentation

REST API Finance Tracker. Базовый URL: `http://localhost:8080`. Ответы — JSON.

---

### Users — `/api/v1/users`

- **GET** `/api/v1/users` — список пользователей
- **GET** `/api/v1/users/{id}` — пользователь по id
- **POST** `/api/v1/users` — создать пользователя (привязка к существующим счетам и транзакциям)
- **PATCH** `/api/v1/users/{id}` — обновить пользователя
- **DELETE** `/api/v1/users/{id}` — удалить пользователя
- **POST** `/api/v1/users/with-accounts?transactional=true|false` — создать пользователя и несколько новых счетов; параметр `transactional` задаёт одну транзакцию (откат при ошибке) или без неё. В теле можно передать `failAfterSecondAccount` для демо-ошибки после 2-го счёта.

---

### Accounts — `/api/v1/accounts`

- **GET** `/api/v1/accounts` — список счетов
- **GET** `/api/v1/accounts/{id}` — счёт по id
- **POST** `/api/v1/accounts` — создать счёт
- **PUT** `/api/v1/accounts/{id}` — обновить счёт
- **DELETE** `/api/v1/accounts/{id}` — удалить счёт

---

### Transactions — `/api/v1/transactions`

- **GET** `/api/v1/transactions` — список транзакций (опционально по датам: `startDate`, `endDate`)
- **GET** `/api/v1/transactions/{id}` — транзакция по id
- **POST** `/api/v1/transactions` — создать транзакцию (привязка к бюджету)
- **PATCH** `/api/v1/transactions/{id}` — обновить транзакцию
- **DELETE** `/api/v1/transactions/{id}` — удалить транзакцию

---

### Categories — `/api/v1/categories`

- **GET** `/api/v1/categories` — список категорий
- **GET** `/api/v1/categories/{id}` — категория по id
- **POST** `/api/v1/categories` — создать категорию
- **PUT** `/api/v1/categories/{id}` — обновить категорию
- **DELETE** `/api/v1/categories/{id}` — удалить категорию

---

### Budgets — `/api/v1/budgets`

- **GET** `/api/v1/budgets` — список бюджетов; `?withTransactions=true` — подгрузить транзакции (fetch join)
- **GET** `/api/v1/budgets/{id}` — бюджет по id
- **POST** `/api/v1/budgets` — создать бюджет
- **PUT** `/api/v1/budgets/{id}` — обновить бюджет
- **DELETE** `/api/v1/budgets/{id}` — удалить бюджет

---

Ответы: 200 (данные), 201 (создано), 204 (удалено). Ошибки: 400 (валидация), 404 (не найдено).
