# Postman-сценарии для отчёта

Этот файл нужен для подготовки скриншотов к разделу лабораторной работы.

Показываем 5 рисунков:

- Рисунок 5.1 – Вызов GET метода со сложным JPQL запросом
- Рисунок 5.2 – Вызов GET метода со сложным native запросом
- Рисунок 5.3 – Вызов GET метода с пагинацией
- Рисунок 5.4 – Результат работы in-memory индекса
- Рисунок 5.5 – Демонстрация инвалидации in-memory индекса

## 0. Подготовка

### 0.1 Запусти приложение

```bash
./mvnw spring-boot:run
```

### 0.2 Залей demo-данные

Если demo-данные ещё не были загружены, выполни:

```bash
psql -U postgres -d finance_tracker -f docs/sql-demo-data.sql
```

### 0.3 Базовая переменная Postman

Создай переменную:

- `baseUrl = http://localhost:8080/api/v1`

### 0.4 Что важно смотреть в ответе

Для endpoint:

```http
GET {{baseUrl}}/transactions/search
```

смотри:

- `Body`
- `Headers`

Особенно важен header:

```http
X-Transaction-Search-Source
```

Он показывает:

- `DATABASE` — результат получен из БД
- `CACHE` — результат получен из in-memory индекса

## 1. Рисунок 5.1 – Сложный JPQL запрос

### Что показывать на скрине

- URL запроса в Postman
- JSON body ответа
- header `X-Transaction-Search-Source: DATABASE`

### Запрос

```http
GET {{baseUrl}}/transactions/search?budgetName=Anna&accountName=Card&minAmount=20&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

### Почему он подходит

Это всё ещё сложный запрос, потому что:

- есть фильтрация по вложенной сущности `budget.name`
- есть фильтрация по вложенной сущности `account.name`
- есть фильтрация по сумме
- есть пагинация

## 2. Рисунок 5.2 – Сложный native query

### Что показывать на скрине

- тот же запрос
- только `queryMode=NATIVE`
- JSON body ответа
- header `X-Transaction-Search-Source: DATABASE`

### Запрос

```http
GET {{baseUrl}}/transactions/search?budgetName=Anna&accountName=Card&minAmount=20&queryMode=NATIVE&page=0&size=5&sortBy=occurredAt&ascending=false
```

### Что сказать в отчёте

Логика фильтрации одинаковая, но в одном случае используется JPQL, а во втором native SQL.

## 3. Рисунок 5.3 – Пагинация

### Что показывать на скрине

Покажи, что:

- `page.size = 3`
- `page.number = 0`
- `page.totalElements` больше 3
- в `content` пришли только 3 записи

### Запрос

```http
GET {{baseUrl}}/transactions/search?queryMode=JPQL&page=0&size=3&sortBy=id&ascending=true
```

### Дополнительно

Если хочешь усилить демонстрацию, сделай ещё второй запрос:

```http
GET {{baseUrl}}/transactions/search?queryMode=JPQL&page=1&size=3&sortBy=id&ascending=true
```

Но для рисунка обычно хватает первой страницы.

## 4. Рисунок 5.4 – Результат работы in-memory индекса

### Идея демонстрации

Один и тот же запрос вызывается дважды подряд.

На первом вызове:

- `X-Transaction-Search-Source: DATABASE`

На втором вызове:

- `X-Transaction-Search-Source: CACHE`

Это и есть доказательство, что сработал in-memory индекс.

### Шаг 1. Первый вызов

```http
GET {{baseUrl}}/transactions/search?budgetName=Pavel&accountName=Salary&minAmount=70&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

Ожидаемо:

- `X-Transaction-Search-Source: DATABASE`

### Шаг 2. Сразу повтори тот же запрос

```http
GET {{baseUrl}}/transactions/search?budgetName=Pavel&accountName=Salary&minAmount=70&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

Ожидаемо:

- `X-Transaction-Search-Source: CACHE`

### Что брать на скрин

Лучше брать второй запрос, где явно видно:

- тот же URL
- тот же JSON body
- header `X-Transaction-Search-Source: CACHE`
- и в терминале строку `Transaction search cache HIT [...]`

## 5. Рисунок 5.5 – Демонстрация инвалидации in-memory индекса

### Идея демонстрации

Нужно показать последовательность:

1. Выполнили поиск
2. Повторили поиск и получили `CACHE`
3. Изменили данные
4. Ещё раз выполнили тот же поиск
5. Получили снова `DATABASE`, потому что индекс инвалидировался

### Шаг 1. Прогрей кеш

Вызови два раза подряд:

```http
GET {{baseUrl}}/transactions/search?budgetName=Anna&accountName=Card&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

На втором вызове должно быть:

- `X-Transaction-Search-Source: CACHE`

### Шаг 2. Измени данные, которые влияют на поиск

Измени имя бюджета `801`, потому что фильтр использует `budgetName`.

```http
PATCH {{baseUrl}}/budgets/801
Content-Type: application/json

{
  "name": "Anna Food June Updated",
  "limitAmount": 700.00
}
```

### Шаг 3. Повтори тот же запрос поиска

Используй уже обновлённый фильтр:

```http
GET {{baseUrl}}/transactions/search?budgetName=Updated&accountName=Card&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

Ожидаемо:

- `X-Transaction-Search-Source: DATABASE`

### Что это доказывает

После изменения бюджета индекс был очищен.

Поэтому следующий запрос не взял старые данные из памяти, а заново пошёл в БД.

### Что можно показать в терминале

Для рисунков 5.4 и 5.5 можно дополнительно вставить скрин из консоли приложения.

Ожидаемые строки:

```text
Transaction search cache MISS [...]
Transaction search loading from DATABASE [...]
Transaction search result cached [...]
Transaction search cache HIT [...]
Transaction search cache INVALIDATED after commit [entriesRemoved=...]
```

## 6. Краткий сценарий по рисункам

### Рисунок 5.1

```http
GET {{baseUrl}}/transactions/search?budgetName=Anna&accountName=Card&minAmount=20&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

### Рисунок 5.2

```http
GET {{baseUrl}}/transactions/search?budgetName=Anna&accountName=Card&minAmount=20&queryMode=NATIVE&page=0&size=5&sortBy=occurredAt&ascending=false
```

### Рисунок 5.3

```http
GET {{baseUrl}}/transactions/search?queryMode=JPQL&page=0&size=3&sortBy=id&ascending=true
```

### Рисунок 5.4

Два одинаковых вызова подряд:

```http
GET {{baseUrl}}/transactions/search?budgetName=Pavel&accountName=Salary&minAmount=70&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

На втором вызове нужен header:

```http
X-Transaction-Search-Source: CACHE
```

### Рисунок 5.5

1. Два раза вызвать:

```http
GET {{baseUrl}}/transactions/search?budgetName=Anna&accountName=Card&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

2. Изменить бюджет:

```http
PATCH {{baseUrl}}/budgets/801
Content-Type: application/json

{
  "name": "Anna Food June Updated",
  "limitAmount": 700.00
}
```

3. Повторить поиск:

```http
GET {{baseUrl}}/transactions/search?budgetName=Updated&accountName=Card&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
```

На этом последнем запросе нужен header:

```http
X-Transaction-Search-Source: DATABASE
```

## 7. Что можно написать под скринами

### Для рисунка 5.4

Повторный вызов того же GET-запроса вернул header `X-Transaction-Search-Source: CACHE`, что подтверждает использование in-memory индекса.

### Для рисунка 5.5

После изменения данных индекс был инвалидирован, поэтому следующий вызов того же поиска вернул header `X-Transaction-Search-Source: DATABASE`, то есть данные были заново получены из базы.
