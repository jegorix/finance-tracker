# Лабораторная 6: инструкция по проверке

## Что реализовано

1. Асинхронная бизнес-операция на `@Async` + `CompletableFuture`:
   - `POST /api/v1/transactions/bulk/async`
   - возвращает `taskId`
   - статус проверяется через `GET /api/v1/transactions/bulk/async/{taskId}`
2. Потокобезопасные счётчики:
   - `AtomicInteger`
   - `synchronized`
3. Демонстрация race condition и решения:
   - `GET /api/v1/demo/race-condition`
4. JMeter-план для нагрузки:
   - `docs/jmeter/lab6-race-condition.jmx`

## Подготовка

1. Поднимите PostgreSQL с БД `finance_tracker`.
2. Убедитесь, что для PostgreSQL передан корректный пароль через переменную окружения `DB_PASSWORD`:

```bash
DB_PASSWORD=postgres ./mvnw spring-boot:run
```

Если пароль у пользователя `postgres` другой, подставьте свой.

3. Запустите приложение:

```bash
DB_PASSWORD=postgres ./mvnw spring-boot:run
```

4. Откройте Swagger:

```text
http://localhost:8080/swagger-ui.html
```

### Важно про URL

Если используете полный URL, запрос к пользователям должен быть таким:

```text
http://localhost:8080/api/v1/users
```

Если используете переменную `baseUrl = http://localhost:8080/api/v1`, то в клиенте дописывайте только хвост endpoint-а:

```text
/users
```

Не нужно:

- добавлять лишнюю `}`
- дублировать `/api/v1`

## Быстрая подготовка данных

Для асинхронного импорта достаточно `accountId`. `budgetId` в этом проекте необязателен, и для стабильной демонстрации его лучше не передавать.

### Postman: переменные окружения

Если используете Postman, заведите переменные окружения:

- `baseUrl = http://localhost:8080/api/v1`
- `userId`
- `accountId`
- `taskId`

### 1. Создать пользователя

- `POST /api/v1/users`

Полный URL:

```text
http://localhost:8080/api/v1/users
```

```json
{
  "username": "maria_sokolova_test",
  "email": "maria.sokolova.test@example.com"
}
```

Сохраните `id` как `userId`.

Если запускаете сценарий повторно, меняйте `username` и `email`, чтобы не получить ошибку из-за дубликатов.

### 2. Создать счёт

- `POST /api/v1/accounts`

```json
{
  "name": "Travel Debit Card",
  "type": "DEBIT",
  "balance": 2780.45,
  "userId": {{userId}}
}
```

Сохраните `id` как `accountId`.

### 3. Бюджет для этой демонстрации не нужен

Если передать несуществующий `budgetId`, задача уйдёт в `FAILED` с ошибкой вида `Budget not found: ...`.
Для сценария защиты это не нужно, поэтому ниже используется только `accountId`.

## Проверка требования 1: асинхронная бизнес-операция

### Запуск async bulk-операции

- `POST /api/v1/transactions/bulk/async?transactional=true`

```json
[
  {
    "occurredAt": "2026-04-05T10:00:00",
    "amount": 47.30,
    "description": "Supermarket purchase",
    "type": "EXPENSE",
    "accountId": {{accountId}}
  },
  {
    "occurredAt": "2026-04-05T11:00:00",
    "amount": 6.80,
    "description": "Coffee shop",
    "type": "EXPENSE",
    "accountId": {{accountId}}
  },
  {
    "occurredAt": "2026-04-05T12:00:00",
    "amount": 18.90,
    "description": "Taxi ride",
    "type": "EXPENSE",
    "accountId": {{accountId}}
  }
]
```

Ожидаемый результат:

- HTTP `202 Accepted`
- в ответе есть `taskId`
- в ответе есть `statusUrl`

Для Postman удобно сразу сохранить `taskId` в переменную окружения. Во вкладке `Tests` у запроса добавьте:

```javascript
const response = pm.response.json();
pm.environment.set("taskId", response.taskId);
```

Пример `curl`:

```bash
curl -X POST "http://localhost:8080/api/v1/transactions/bulk/async?transactional=true" \
  -H "Content-Type: application/json" \
  -d @bulk-transactions.json
```

Что показать на защите:

- `Рисунок 4.1`: ответ на запуск async-операции с `taskId`

## Проверка требования 1: просмотр статуса задачи

Сразу после старта вызовите:

- `GET /api/v1/transactions/bulk/async/{taskId}`

Ожидаемые поля:

- `taskId`
- `status`
- `transactional`
- `totalItems`
- `processedItems`
- `progressPercent`
- `createdTransactionIds`
- `summary`
- `errorMessage`

Нормальный сценарий статусов:

1. `PENDING`
2. `IN_PROGRESS`
3. `COMPLETED`

### Postman: как гарантированно увидеть `IN_PROGRESS`, потом `COMPLETED`

1. Отправьте `POST {{baseUrl}}/transactions/bulk/async?transactional=true`.
2. Сразу после ответа отправьте `GET {{baseUrl}}/transactions/bulk/async/{{taskId}}`.
3. Из-за настроенных задержек в приложении задача обычно будет в `IN_PROGRESS`.
4. Подождите `10` секунд.
5. Снова отправьте `GET {{baseUrl}}/transactions/bulk/async/{{taskId}}`.
6. Теперь статус должен быть `COMPLETED`.

Почему это работает:

- в приложении уже есть задержка `lab6.async.initial-delay-ms=1000`
- и задержка на каждый элемент `lab6.async.per-item-delay-ms=700`
- для `3` транзакций это даёт достаточно времени, чтобы увидеть промежуточный статус

Пример `curl`:

```bash
curl "http://localhost:8080/api/v1/transactions/bulk/async/<TASK_ID>"
```

Если задача завершается слишком быстро, увеличьте задержку для наглядности:

```properties
lab6.async.initial-delay-ms=1500
lab6.async.per-item-delay-ms=1200
```

После изменения перезапустите приложение.

Если в ответе приходит `FAILED`, проверьте:

- существует ли `accountId`
- не передаёте ли случайно несуществующий `budgetId`
- не дублируете ли `/api/v1` в URL
- используете ли `{{baseUrl}} = http://localhost:8080/api/v1`

Что показать на защите:

- `Рисунок 4.2`: ответ endpoint статуса, желательно в состоянии `IN_PROGRESS`

## Проверка требований 2 и 3: race condition и решение

Вызовите:

- `GET /api/v1/demo/race-condition`

Ожидаемый смысл ответа:

- `unsafeCounter` показывает потерянные обновления
- `synchronizedCounter` достигает ожидаемого значения
- `atomicCounter` достигает ожидаемого значения
- `threadCount = 50`
- `incrementsPerThread = 1000`
- `expectedValue = 50000`

Пример `curl`:

```bash
curl "http://localhost:8080/api/v1/demo/race-condition"
```

На что обратить внимание в ответе:

- у `unsafeCounter` поле `matchesExpected = false`
- у `unsafeCounter` поле `lostUpdates > 0`
- у `synchronizedCounter` и `atomicCounter` поле `matchesExpected = true`

Что показать на защите:

- `Рисунок 4.3`: полный ответ race-condition demo с тремя секциями

## Проверка через Swagger

Если сдаёте через Swagger UI, достаточно открыть:

1. `Async Transactions`
2. `Concurrency Demo`

И выполнить:

1. `POST /api/v1/transactions/bulk/async`
2. `GET /api/v1/transactions/bulk/async/{taskId}`
3. `GET /api/v1/demo/race-condition`

Для демонстрации статусов в Swagger логика такая же:

1. отправить `POST`
2. сразу открыть `GET` по `taskId` и показать `IN_PROGRESS`
3. подождать `10` секунд
4. повторить `GET` и показать `COMPLETED`

## Нагрузочное тестирование JMeter

Готовый план:

```text
docs/jmeter/lab6-race-condition.jmx
```

### Вариант 1. Через GUI

1. Откройте JMeter.
2. Загрузите файл `docs/jmeter/lab6-race-condition.jmx`.
3. Запустите тест.
4. Откройте `Summary Report`.
5. Сделайте скрин с метриками.

### Вариант 2. Через CLI

```bash
jmeter -n \
  -t docs/jmeter/lab6-race-condition.jmx \
  -l docs/jmeter/lab6-race-condition-results.jtl \
  -e \
  -o docs/jmeter/lab6-race-condition-report
```

После выполнения откройте:

```text
docs/jmeter/lab6-race-condition-report/index.html
```

### Что показать преподавателю по JMeter

- число запросов (`# Samples`)
- среднее время ответа (`Average`)
- 95-й перцентиль, если используете HTML Report
- `Error %`
- `Throughput`

## Минимальный сценарий для устной сдачи

1. Запустить приложение.
2. В Swagger отправить `POST /api/v1/transactions/bulk/async`.
3. Показать ответ с `taskId`.
4. Сразу открыть `GET /api/v1/transactions/bulk/async/{taskId}`.
5. Показать переход статуса задачи.
6. Вызвать `GET /api/v1/demo/race-condition`.
7. Пояснить, почему unsafe-счётчик теряет обновления, а `synchronized` и `AtomicInteger` нет.
8. Открыть результат JMeter и кратко прокомментировать метрики.

## Что отвечать по требованиям

- Для асинхронности используется `@Async` и `CompletableFuture`.
- `taskId` генерируется при постановке bulk-операции в фон.
- Статус хранится в in-memory storage на `ConcurrentHashMap`.
- Потокобезопасность показана двумя способами: `synchronized` и `AtomicInteger`.
- Race condition воспроизводится на `ExecutorService` с `50` потоками.
- Исправление показано на synchronized/atomic-счётчиках.
