# SQL Demo Data

SQL-сценарий для PostgreSQL с дополнительными тестовыми данными, которые не пересекаются с тем, что уже есть у вас в базе.

Порядок создания сущностей:

`users -> budgets -> categories -> accounts -> budget_category -> transactions`

Сценарий ориентирован на текущее состояние вашей БД, где уже есть базовые тестовые пользователи и записи с `id` до `17` в `transactions`.

## Как запускать

```bash
psql -U postgres -d finance_tracker
```

Дальше вставьте SQL ниже.

## SQL

```sql
BEGIN;

-- 0. Опционально: очистка именно этих demo-данных, если вы уже запускали сценарий раньше
DELETE FROM transactions WHERE id IN (2001, 2002, 2003, 2004);
DELETE FROM budget_category WHERE budget_id IN (301, 302, 303, 304)
   OR category_id IN (401, 402, 403, 404);
DELETE FROM categories WHERE id IN (401, 402, 403, 404);
DELETE FROM accounts WHERE id IN (501, 502, 503, 504);
DELETE FROM budgets WHERE id IN (301, 302, 303, 304);
DELETE FROM users WHERE id IN (201, 202);

-- 1. Users
INSERT INTO users (id, username, email)
VALUES
    (201, 'Marina Lebedeva', 'marina.lebedeva.demo@example.com'),
    (202, 'Ilya Voronov', 'ilya.voronov.demo@example.com');

-- 2. Budgets
INSERT INTO budgets (id, name, limit_amount, period_start, period_end, user_id)
VALUES
    (301, 'Home Comfort April', 1200.00, DATE '2026-04-01', DATE '2026-04-30', 201),
    (302, 'Health April', 500.00, DATE '2026-04-01', DATE '2026-04-30', 201),
    (303, 'City Break May', 900.00, DATE '2026-05-01', DATE '2026-05-31', 202),
    (304, 'Learning May', 700.00, DATE '2026-05-01', DATE '2026-05-31', 202);

-- 3. Categories
INSERT INTO categories (id, name, user_id)
VALUES
    (401, 'Household Supplies', 201),
    (402, 'Pharmacy', 201),
    (403, 'Weekend Trips', 202),
    (404, 'Online Courses', 202);

-- 4. Accounts
INSERT INTO accounts (id, name, type, balance, user_id)
VALUES
    (501, 'Salary Card Marina', 'DEBIT', 4200.00, 201),
    (502, 'Home Cash Marina', 'CASH', 260.00, 201),
    (503, 'Travel Card Ilya', 'DEBIT', 3100.00, 202),
    (504, 'Reserve Savings Ilya', 'SAVINGS', 8000.00, 202);

-- 5. Category <-> Budget links
INSERT INTO budget_category (budget_id, category_id)
VALUES
    (301, 401),
    (302, 402),
    (303, 403),
    (304, 404);

-- 6. Transactions
INSERT INTO transactions (id, occurred_at, amount, description, type, budget_id, account_id)
VALUES
    (2001, TIMESTAMP '2026-04-03 19:10:00', 145.80, 'Supermarket restock', 'EXPENSE', 301, 501),
    (2002, TIMESTAMP '2026-04-06 09:00:00', 39.90, 'Pharmacy order', 'EXPENSE', 302, 502),
    (2003, TIMESTAMP '2026-05-02 08:40:00', 210.00, 'Train tickets for weekend trip', 'EXPENSE', 303, 503),
    (2004, TIMESTAMP '2026-05-05 20:15:00', 320.00, 'Spring Java course payment', 'EXPENSE', 304, 503);

-- 7. Синхронизация sequence после ручных id
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users), true);
SELECT setval('budgets_id_seq', (SELECT COALESCE(MAX(id), 1) FROM budgets), true);
SELECT setval('categories_id_seq', (SELECT COALESCE(MAX(id), 1) FROM categories), true);
SELECT setval('accounts_id_seq', (SELECT COALESCE(MAX(id), 1) FROM accounts), true);
SELECT setval('transactions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM transactions), true);

COMMIT;
```

## Что создается

- 2 пользователя
- 4 бюджета
- 4 категории
- 4 счета
- 4 связи в `budget_category`
- 4 транзакции

## Быстрая проверка

```sql
SELECT * FROM users WHERE id IN (201, 202) ORDER BY id;
SELECT * FROM budgets WHERE user_id IN (201, 202) ORDER BY id;
SELECT * FROM categories WHERE user_id IN (201, 202) ORDER BY id;
SELECT * FROM accounts WHERE user_id IN (201, 202) ORDER BY id;
SELECT * FROM budget_category WHERE budget_id IN (301, 302, 303, 304) ORDER BY budget_id, category_id;
SELECT * FROM transactions WHERE id IN (2001, 2002, 2003, 2004) ORDER BY id;
```

## Важно

- Имена и email в этом сценарии новые и не повторяют текущие записи из вашей базы.
- Названия бюджетов, категорий и счетов тоже не пересекаются с уже показанными вами данными.
- Сценарий рассчитан на текущую схему проекта, где денежные поля имеют тип `NUMERIC(19,2)`.
