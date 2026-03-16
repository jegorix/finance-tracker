BEGIN;

DELETE FROM transactions
WHERE id BETWEEN 3001 AND 3021;

DELETE FROM budget_category
WHERE budget_id BETWEEN 801 AND 809
   OR category_id BETWEEN 901 AND 909;

DELETE FROM categories
WHERE id BETWEEN 901 AND 909;

DELETE FROM accounts
WHERE id BETWEEN 1001 AND 1006;

DELETE FROM budgets
WHERE id BETWEEN 801 AND 809;

DELETE FROM users
WHERE id BETWEEN 701 AND 703;

INSERT INTO users (id, username, email)
VALUES
    (701, 'anna_demo_701', 'anna.demo.701@example.com'),
    (702, 'pavel_demo_702', 'pavel.demo.702@example.com'),
    (703, 'daria_demo_703', 'daria.demo.703@example.com');

INSERT INTO budgets (id, name, limit_amount, period_start, period_end, user_id)
VALUES
    (801, 'Anna Food June', 700.00, DATE '2026-06-01', DATE '2026-06-30', 701),
    (802, 'Anna Sport June', 220.00, DATE '2026-06-01', DATE '2026-06-30', 701),
    (803, 'Anna Trip July', 1600.00, DATE '2026-07-01', DATE '2026-07-31', 701),
    (804, 'Pavel Home June', 900.00, DATE '2026-06-01', DATE '2026-06-30', 702),
    (805, 'Pavel Auto June', 650.00, DATE '2026-06-01', DATE '2026-06-30', 702),
    (806, 'Pavel Tech July', 1500.00, DATE '2026-07-01', DATE '2026-07-31', 702),
    (807, 'Daria Cafe June', 350.00, DATE '2026-06-01', DATE '2026-06-30', 703),
    (808, 'Daria Study July', 800.00, DATE '2026-07-01', DATE '2026-07-31', 703),
    (809, 'Daria Family July', 1200.00, DATE '2026-07-01', DATE '2026-07-31', 703);

INSERT INTO categories (id, name, user_id)
VALUES
    (901, 'Anna Groceries', 701),
    (902, 'Anna Fitness', 701),
    (903, 'Anna Travel', 701),
    (904, 'Pavel Home', 702),
    (905, 'Pavel Car', 702),
    (906, 'Pavel Gadgets', 702),
    (907, 'Daria Cafe', 703),
    (908, 'Daria Study', 703),
    (909, 'Daria Family', 703);

INSERT INTO accounts (id, name, type, balance, user_id)
VALUES
    (1001, 'Anna Main Card', 'DEBIT', 2850.00, 701),
    (1002, 'Anna Cash Wallet', 'CASH', 340.00, 701),
    (1003, 'Pavel Salary Card', 'DEBIT', 5120.00, 702),
    (1004, 'Pavel Reserve Savings', 'SAVINGS', 12400.00, 702),
    (1005, 'Daria Family Card', 'DEBIT', 4310.00, 703),
    (1006, 'Daria Daily Cash', 'CASH', 190.00, 703);

INSERT INTO budget_category (budget_id, category_id)
VALUES
    (801, 901),
    (802, 902),
    (803, 903),
    (804, 904),
    (805, 905),
    (806, 906),
    (807, 907),
    (808, 908),
    (809, 909),
    (801, 902),
    (804, 905),
    (809, 907);

INSERT INTO transactions (id, occurred_at, amount, description, type, budget_id, account_id)
VALUES
    (3001, TIMESTAMP '2026-06-02 08:20:00', 54.30, 'Morning grocery run', 'EXPENSE', 801, 1001),
    (3002, TIMESTAMP '2026-06-04 18:45:00', 28.00, 'Fresh market vegetables', 'EXPENSE', 801, 1002),
    (3003, TIMESTAMP '2026-06-07 07:30:00', 35.00, 'Yoga class pass', 'EXPENSE', 802, 1001),
    (3004, TIMESTAMP '2026-06-09 20:15:00', 19.90, 'Protein snacks', 'EXPENSE', 802, 1002),
    (3005, TIMESTAMP '2026-07-03 10:00:00', 1200.00, 'Flight booking', 'EXPENSE', 803, 1001),
    (3006, TIMESTAMP '2026-07-05 13:40:00', 250.00, 'Hotel prepayment', 'EXPENSE', 803, 1001),
    (3007, TIMESTAMP '2026-07-06 09:15:00', 400.00, 'Travel refund from friend', 'INCOME', 803, 1001),
    (3008, TIMESTAMP '2026-06-03 12:10:00', 180.50, 'Home repair materials', 'EXPENSE', 804, 1003),
    (3009, TIMESTAMP '2026-06-08 17:25:00', 74.00, 'Cleaning supplies', 'EXPENSE', 804, 1003),
    (3010, TIMESTAMP '2026-06-10 08:00:00', 120.00, 'Fuel refill', 'EXPENSE', 805, 1003),
    (3011, TIMESTAMP '2026-06-12 19:30:00', 210.00, 'Car service deposit', 'EXPENSE', 805, 1003),
    (3012, TIMESTAMP '2026-07-02 11:45:00', 650.00, 'Laptop parts order', 'EXPENSE', 806, 1004),
    (3013, TIMESTAMP '2026-07-04 16:05:00', 89.99, 'Mechanical keyboard', 'EXPENSE', 806, 1003),
    (3014, TIMESTAMP '2026-07-07 09:00:00', 300.00, 'Sold old monitor', 'INCOME', 806, 1003),
    (3015, TIMESTAMP '2026-06-01 09:20:00', 12.50, 'Cappuccino and croissant', 'EXPENSE', 807, 1006),
    (3016, TIMESTAMP '2026-06-05 15:40:00', 24.00, 'Lunch with colleague', 'EXPENSE', 807, 1005),
    (3017, TIMESTAMP '2026-07-03 18:10:00', 199.00, 'English course payment', 'EXPENSE', 808, 1005),
    (3018, TIMESTAMP '2026-07-09 10:30:00', 65.00, 'Books and notebooks', 'EXPENSE', 808, 1005),
    (3019, TIMESTAMP '2026-07-12 14:00:00', 310.00, 'Kids activity subscription', 'EXPENSE', 809, 1005),
    (3020, TIMESTAMP '2026-07-14 20:20:00', 87.40, 'Family dinner', 'EXPENSE', 809, 1005),
    (3021, TIMESTAMP '2026-07-15 09:10:00', 150.00, 'Moved cash to family card', 'TRANSFER', 809, 1006);

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 1) FROM users), true);
SELECT setval(pg_get_serial_sequence('budgets', 'id'), (SELECT COALESCE(MAX(id), 1) FROM budgets), true);
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT COALESCE(MAX(id), 1) FROM categories), true);
SELECT setval(pg_get_serial_sequence('accounts', 'id'), (SELECT COALESCE(MAX(id), 1) FROM accounts), true);
SELECT setval(pg_get_serial_sequence('transactions', 'id'), (SELECT COALESCE(MAX(id), 1) FROM transactions), true);

COMMIT;
