# Лабораторная работа 4: что было сделано

## Краткий итог

В проекте `Finance Tracker` реализованы все пункты лабораторной:

1. глобальная обработка ошибок через `@RestControllerAdvice`;
2. валидация входных данных через `@Valid`, `@Validated` и bean validation-аннотации;
3. единый JSON-формат ошибки для всех endpoint;
4. логирование через `logback` с уровнями и ротацией;
5. аспект AOP для логирования времени выполнения сервисных методов;
6. Swagger/OpenAPI с описанием endpoint и DTO.

Ниже по пунктам описано, что именно изменено в проекте.

---

## 1. Глобальная обработка ошибок

### Что добавлено

- `src/main/java/com/finance/tracker/exception/GlobalExceptionHandler.java`
- `src/main/java/com/finance/tracker/exception/ApiErrorResponse.java`
- `src/main/java/com/finance/tracker/exception/ApiErrorField.java`

### Идея реализации

Раньше ошибки в приложении отдавались разными способами:

- часть через `ResponseStatusException`;
- часть через стандартные ошибки Spring;
- часть через ошибки валидации тела запроса.

Из-за этого клиент видел ответы разного формата.

Теперь все исключения собираются в одном месте через `@RestControllerAdvice`, а клиент всегда получает единый JSON.

### Какие случаи обрабатываются

- ошибки `@Valid` для `@RequestBody` (`MethodArgumentNotValidException`);
- ошибки привязки query-параметров (`BindException`);
- ошибки валидации параметров метода и `@PathVariable`;
- `ConstraintViolationException` для ограничений вроде `@Positive`;
- ошибки типа параметров (`MethodArgumentTypeMismatchException`);
- некорректный JSON или неверный enum/date/time (`HttpMessageNotReadableException`);
- бизнес-ошибки через `ResponseStatusException`;
- ошибки ограничений БД (`DataIntegrityViolationException`);
- `405 Method Not Allowed`;
- `404 Endpoint not found`;
- неожиданные `500`.

### Единый формат ошибки

Теперь ошибка возвращается в таком виде:

```json
{
  "timestamp": "2026-03-19T19:01:36.578+03:00",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/users/0",
  "fieldErrors": [
    {
      "field": "id",
      "message": "must be greater than 0",
      "rejectedValue": "0"
    }
  ]
}
```

### Что это даёт

- одинаковый формат для Postman, Swagger UI и фронтенда;
- удобный разбор ошибок на клиенте;
- проще демонстрировать преподавателю `400`, `404`, `409`, `500`;
- логика обработки ошибок не размазана по контроллерам.

---

## 2. Валидация входных данных

### Что уже было

Во многих DTO уже были базовые bean validation-аннотации:

- `@NotBlank`
- `@NotNull`
- `@Email`
- `@Positive`
- `@DecimalMin`
- `@Size`
- `@PastOrPresent`

### Что дополнительно сделано

- на контроллеры добавлен `@Validated`;
- на `@PathVariable` добавлено `@Positive`, чтобы нельзя было передать `0` или отрицательный id;
- для поиска транзакций создан отдельный DTO:
  - `src/main/java/com/finance/tracker/dto/request/TransactionSearchRequest.java`
- поиск теперь принимает `@Valid @ModelAttribute TransactionSearchRequest`, а не набор невалидируемых параметров.

### Что валидируется в `TransactionSearchRequest`

- `page >= 0`;
- `size > 0`;
- `sortBy` не пустой;
- `queryMode` не `null`;
- строковые фильтры ограничены по длине;
- `minAmount` и `maxAmount` не могут быть отрицательными.

### Что изменилось в контроллерах

Пример:

```java
@GetMapping("/{id}")
public ResponseEntity<UserResponse> getById(@PathVariable("id") @Positive Long id) {
    return ResponseEntity.ok(userService.findById(id));
}
```

И для поиска:

```java
@GetMapping("/search")
public ResponseEntity<TransactionSearchPageResponse> search(
        @Valid @ModelAttribute TransactionSearchRequest request) {
    ...
}
```

### Результат

Некорректные входные данные отсекаются до попадания в бизнес-логику, а клиент получает понятный `400 Bad Request`.

---

## 3. Единый формат ошибки для всех endpoint

Это реализовано совместно с `ControllerAdvice`, но логически это отдельный пункт лабораторной.

### Что именно унифицировано

Для всех endpoint теперь используются одинаковые поля:

- `timestamp`
- `status`
- `error`
- `code`
- `message`
- `path`
- `fieldErrors`

### Где это особенно заметно

- `POST /api/v1/users` с невалидным email;
- `GET /api/v1/users/0`;
- `GET /api/v1/users/999999`;
- `POST /api/v1/accounts/transfer` с ошибкой выполнения;
- любой запрос на несуществующий URL.

### Почему это важно

- фронтенд может один раз написать разбор ошибки;
- Postman-демонстрация становится очень наглядной;
- отчёт проще оформлять, потому что формат не меняется от endpoint к endpoint.

---

## 4. Логирование через logback

### Что добавлено

- `src/main/resources/logback-spring.xml`
- `logs/` добавлена в `.gitignore`

### Как настроено

#### Appenders

- `CONSOLE` для вывода в консоль;
- `APPLICATION_FILE` для общего файла `logs/application.log`;
- `ERROR_FILE` для ошибок в `logs/error.log`.

#### Ротация

Используется `SizeAndTimeBasedRollingPolicy`.

Для `application.log`:

- имя архивов: `logs/archive/application.%d{yyyy-MM-dd}.%i.log.gz`
- размер файла: `10MB`
- хранение: `14` дней
- общий объём архивов: `1GB`

Для `error.log`:

- имя архивов: `logs/archive/error.%d{yyyy-MM-dd}.%i.log.gz`
- размер файла: `10MB`
- хранение: `30` дней
- общий объём архивов: `512MB`

#### Уровни логирования

- `com.finance.tracker = INFO`
- `org.springframework.web = WARN`
- `org.hibernate.SQL = WARN`
- `root = WARN`

### Что это даёт

- бизнес-логи приложения не теряются;
- ошибки пишутся отдельно;
- логи не растут бесконечно;
- преподавателю можно показать и консоль, и файловые логи.

---

## 5. AOP для логирования времени выполнения сервисных методов

### Что добавлено

- `src/main/java/com/finance/tracker/aop/ServiceLoggingAspect.java`

### Как работает аспект

Аспект перехватывает все методы из `com.finance.tracker.service.impl..*` через `@Around`.

Алгоритм:

1. до вызова сохраняется время старта;
2. вызывается оригинальный сервисный метод;
3. после выполнения считается время в миллисекундах;
4. в лог пишется:
   - успешное выполнение;
   - либо ошибка с временем до падения.

### Пример сообщений

```text
Service method UserServiceImpl.findById(..) completed in 3 ms
Service method AccountServiceImpl.transferTx(..) failed in 5 ms: Forced error right after money was debited from the source account
```

### Зачем это нужно

- видно, какие сервисные методы вызываются;
- можно быстро найти медленные операции;
- логирование времени вынесено из бизнес-кода и не засоряет сервисы.

---

## 6. Swagger/OpenAPI

### Что добавлено

- зависимость `org.springdoc:springdoc-openapi-starter-webmvc-ui`
- конфиг `src/main/java/com/finance/tracker/config/OpenApiConfig.java`
- пакет `src/main/java/com/finance/tracker/controller/api`
- свойства в `application.properties`:

```properties
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Что описано

- Swagger-описания endpoint вынесены в интерфейсы `controller/api/*ControllerApi.java`;
- интерфейсы помечены `@Tag`;
- методы интерфейсов помечены `@Operation`;
- request/response DTO описаны через `@Schema`.

### Что теперь доступно

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

### Что видно в Swagger

- список всех endpoint;
- параметры path/query/body;
- описания DTO;
- обязательность полей;
- enum-значения;
- схемы request/response.

---

## 7. Изменения в шаблоне отчёта из `EXAMPLE`

Файл `EXAMPLE/lab4-nowicki.docx` был явно сделан под другой проект. В нём были несоответствия:

- упоминались `Dockerfile` и `docker-compose.yaml`, которых нет в текущем проекте;
- использовались `TagController`, `TagRequest`, `TagResponse`, которых в проекте нет;
- структура и листинг кода не совпадали с реальным `Finance Tracker`.

Поэтому для текущего проекта я подготовил исправленную версию отчёта:

- `lab4_docs/lab4-nowicki-fixed.md`
- `lab4_docs/lab4-nowicki-fixed.docx`

В новой версии:

- сохранены общая логика и структура отчёта;
- заменены структура проекта и листинги кода;
- описание демонстрации привязано к реальным endpoint текущего проекта.

---

## 8. Проверка

### Что удалось проверить

- `./mvnw -q -DskipTests compile` — успешно;
- `./mvnw -q test` — успешно.

### Что не удалось довести до конца

`./mvnw -q -DskipTests verify` не завершился из-за внешней проблемы окружения:

- Maven попытался скачать `maven-jar-plugin`;
- в окружении возникла ошибка сертификата для `repo.maven.apache.org`.

То есть проблема не в коде проекта, а во внешнем доступе Maven к репозиторию.

---

## 9. Список ключевых файлов

- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/resources/logback-spring.xml`
- `src/main/java/com/finance/tracker/exception/ApiErrorResponse.java`
- `src/main/java/com/finance/tracker/exception/ApiErrorField.java`
- `src/main/java/com/finance/tracker/exception/GlobalExceptionHandler.java`
- `src/main/java/com/finance/tracker/aop/ServiceLoggingAspect.java`
- `src/main/java/com/finance/tracker/config/OpenApiConfig.java`
- `src/main/java/com/finance/tracker/dto/request/TransactionSearchRequest.java`
- `src/main/java/com/finance/tracker/controller/UserController.java`
- `src/main/java/com/finance/tracker/controller/AccountController.java`
- `src/main/java/com/finance/tracker/controller/BudgetController.java`
- `src/main/java/com/finance/tracker/controller/CategoryController.java`
- `src/main/java/com/finance/tracker/controller/TransactionController.java`
- `src/test/java/com/finance/tracker/controller/UserControllerValidationTest.java`
