# Как демонстрировать лабораторную работу 4

## Что открыть заранее

Для демонстрации удобно держать открытыми 4 окна:

1. терминал с запущенным приложением;
2. второй терминал с логами;
3. браузер со Swagger UI;
4. Postman.

---

## 1. Подготовка окружения

### 1.1. Убедиться, что PostgreSQL запущен

В `application.properties` используется:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_tracker
spring.datasource.username=postgres
spring.datasource.password=postgres
```

Значит до запуска приложения база `finance_tracker` должна быть доступна локально.

### 1.2. Запустить приложение

В терминале:

```bash
./mvnw spring-boot:run
```

Если приложение поднялось корректно, оно будет доступно на:

- `http://localhost:8080`

### 1.3. Открыть логи

Во втором терминале:

```bash
tail -f logs/application.log
```

И при желании отдельно:

```bash
tail -f logs/error.log
```

### 1.4. Открыть Swagger UI

В браузере:

- `http://localhost:8080/swagger-ui.html`

Что показать преподавателю:

- список endpoint;
- описание DTO;
- query/path/body параметры;
- схему unified error response.

---

## 2. Настройка Postman

### 2.1. Создать environment

Название: `Finance Tracker Lab4`

### 2.2. Добавить переменные

- `baseUrl = http://localhost:8080/api/v1`
- `userId`
- `account1Id`
- `account2Id`

### 2.3. Общие заголовки

Для JSON-запросов:

```http
Content-Type: application/json
```

---

## 3. Что показать по каждому пункту лабораторной

## Пункт 1. Глобальная обработка ошибок через `@ControllerAdvice`

### Что сделать в Postman

Отправить:

- `GET {{baseUrl}}/users/999999`

### Что должно получиться

- статус `404`;
- JSON единого формата;
- поля `status`, `error`, `code`, `message`, `path`.

### Что сказать преподавателю

- исключение выбрасывается в сервисе;
- контроллер его не ловит;
- обработка идёт глобально через `GlobalExceptionHandler`;
- формат ответа одинаковый для всех контроллеров.

---

## Пункт 2. Валидация через `@Valid`

### Что сделать в Postman

Отправить запрос:

- `POST {{baseUrl}}/users`

Body:

```json
{
  "username": "",
  "email": "wrong-email"
}
```

### Что показать

- статус `400`;
- код ошибки `VALIDATION_ERROR`;
- массив `fieldErrors`;
- ошибки по `username` и `email`.

### Что сказать преподавателю

- `@Valid` стоит на `@RequestBody`;
- ограничения заданы в `UserRequest`;
- ошибка превратилась в единый JSON через `ControllerAdvice`.

---

## Пункт 3. Единый формат ошибки для всех endpoint

### Что сделать в Postman

Показать 2 разных случая:

#### Случай 1. Невалидный path variable

- `GET {{baseUrl}}/users/0`

#### Случай 2. Несуществующий ресурс

- `GET {{baseUrl}}/users/999999`

### Что сравнить

В обоих случаях структура ответа одинаковая:

- `timestamp`
- `status`
- `error`
- `code`
- `message`
- `path`
- `fieldErrors` при необходимости

### Что сказать преподавателю

- меняется только содержимое, но не схема ответа;
- это удобно и для фронтенда, и для тестирования API.

---

## Пункт 4. Логирование через logback

Для этого пункта лучше показать и Postman, и терминал, и файловую систему/IDE.

### 4.1. Показать уровни логирования

Открой:

- `src/main/resources/logback-spring.xml`

Что показать:

- `com.finance.tracker = INFO`
- `org.springframework.web = WARN`
- `org.hibernate.SQL = WARN`
- `root = WARN`

### 4.2. Показать файлы логов

Открой папку:

- `logs/`

Что показать:

- `application.log`
- `error.log`
- `archive/`

### 4.3. Показать запись в `application.log`

Сначала создай пользователя:

- `POST {{baseUrl}}/users`

```json
{
  "username": "demo_user",
  "email": "demo_user@example.com"
}
```

Сохрани `id` в `userId`.

Потом создай два счёта:

- `POST {{baseUrl}}/accounts`

```json
{
  "name": "Main Card",
  "type": "DEBIT",
  "balance": 1000.00,
  "userId": {{userId}}
}
```

Сохрани `id` в `account1Id`.

Второй:

```json
{
  "name": "Cash Wallet",
  "type": "CASH",
  "balance": 200.00,
  "userId": {{userId}}
}
```

Сохрани `id` в `account2Id`.

После этого в терминале с `tail -f logs/application.log` покажи:

- запись о выполнении сервисного метода;
- обычные `INFO`/`WARN` сообщения приложения.

### 4.4. Показать `error.log`

Отправь специальный запрос:

- `POST {{baseUrl}}/accounts/transfer?transactional=true`

```json
{
  "fromAccountId": {{account1Id}},
  "toAccountId": {{account2Id}},
  "amount": 50.00,
  "failAfterDebit": true
}
```

### Что показать

- запрос упадёт с `500`;
- в `logs/error.log` появится запись об ошибке;
- в `logs/application.log` тоже будут строки по этому вызову;
- аспект покажет, за сколько миллисекунд метод завершился с ошибкой.

### 4.5. Как объяснить ротацию

В `logback-spring.xml` покажи:

- `SizeAndTimeBasedRollingPolicy`
- `maxFileSize`
- `maxHistory`
- `totalSizeCap`
- шаблон имени архивов

### Если преподаватель попросит живую демонстрацию ротации

Есть два варианта:

1. Просто показать конфигурацию и папку `archive`, если архивы уже появились.
2. Временно уменьшить `maxFileSize`, например до `100KB`, перезапустить приложение и сделать много однотипных запросов из Postman Runner.

В обычной защите чаще достаточно показать саму конфигурацию и объяснить, как она работает.

---

## Пункт 5. AOP для логирования времени выполнения сервисных методов

### Что сделать

Снова отправь любой рабочий запрос, например:

- `GET {{baseUrl}}/users/{{userId}}`

или

- `GET {{baseUrl}}/accounts/{{account1Id}}`

### Что показать в логе

Строку вида:

```text
Service method UserServiceImpl.findById(..) completed in 3 ms
```

### Потом покажи ошибочный вызов

Можно использовать тот же `transfer` с `failAfterDebit=true`.

### Что показать

Строку вида:

```text
Service method AccountServiceImpl.transferTx(..) failed in 5 ms: ...
```

### Что сказать преподавателю

- логика не вставлена вручную в каждый сервис;
- время меряется аспектом;
- используется `@Around` advice.

---

## Пункт 6. Swagger/OpenAPI

### Что открыть

В браузере:

- `http://localhost:8080/swagger-ui.html`

### Что показать

1. Контроллеры `Users`, `Accounts`, `Budgets`, `Categories`, `Transactions`.
2. Описание endpoint через `@Tag` и `@Operation`, вынесенные в интерфейсы `controller/api/*ControllerApi.java`.
3. Схемы DTO:
   - `UserRequest`
   - `AccountRequest`
   - `TransactionRequest`
   - `TransactionSearchRequest`
   - `ApiErrorResponse`
4. Доступность JSON-схемы OpenAPI:
   - `http://localhost:8080/api-docs`

### Что сказать преподавателю

- документация генерируется автоматически;
- DTO и endpoint описаны аннотациями, при этом Swagger-описание endpoint хранится отдельно от реализации контроллеров;
- Swagger можно использовать для ручного тестирования API.

---

## 4. Что вставлять в раздел демонстрации отчёта

Для 4 раздела отчёта удобно сделать такие скриншоты:

1. Swagger UI с раскрытым endpoint `POST /api/v1/users`.
2. Swagger UI со схемой `TransactionSearchRequest` или `UserRequest`.
3. Postman с ошибкой `400 VALIDATION_ERROR`.
4. Postman с ошибкой `404 RESOURCE_NOT_FOUND`.
5. Терминал или лог-файл со строкой AOP: `completed in ... ms`.
6. Терминал или `error.log` со строкой про ошибочный вызов `transfer`.
7. Папка `logs` и `logs/archive`.
8. Фрагмент `logback-spring.xml` с ротацией.

---

## 5. Короткий сценарий защиты на 3-5 минут

1. Запусти приложение и сразу открой `swagger-ui.html`.
2. Покажи, что endpoint и DTO документированы.
3. В Postman отправь невалидный `POST /users` и покажи `400`.
4. Отправь `GET /users/999999` и покажи `404`.
5. Открой `tail -f logs/application.log`.
6. Выполни успешный запрос и покажи AOP-лог времени.
7. Выполни ошибочный `POST /accounts/transfer?transactional=true` с `failAfterDebit=true`.
8. Покажи `error.log` и конфиг ротации в `logback-spring.xml`.

Этого достаточно, чтобы закрыть все требования лабораторной.
