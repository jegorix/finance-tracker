# ОТЧЕТ О ЛАБОРАТОРНОЙ РАБОТЕ № 4

## Error logging/handling

по дисциплине «Программирование на языках высокого уровня»

Ниже приведён исправленный вариант отчёта, адаптированный под текущий проект `Finance Tracker`.
Шаблон из `EXAMPLE/lab4-nowicki.docx` был ориентирован на другой проект, поэтому в нём заменены:

- структура проекта;
- имена контроллеров, DTO и пакетов;
- листинги кода;
- описание демонстрации.

Общие части, такие как постановка задачи и заключение, сохранены по смыслу.

---

## 1 ПОСТАНОВКА ЗАДАЧИ

1. Реализовать глобальную обработку ошибок через `@ControllerAdvice`.
2. Добавить валидацию входных данных через `@Valid`.
3. Реализовать единый формат ошибки для всех endpoint.
4. Настроить логирование через `logback`:
   - уровни логирования;
   - ротация логов.
5. Реализовать аспект (AOP) для логирования времени выполнения сервисных методов.
6. Подключить Swagger/OpenAPI с описанием endpoint и DTO.

---

## 2 СТРУКТУРА ПРОЕКТА

Актуальная структура проекта с учётом лабораторной работы 4:

```text
finance-tracker
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com/finance/tracker
│   │   │       ├── FinanceTrackerApplication.java
│   │   │       ├── aop
│   │   │       │   └── ServiceLoggingAspect.java
│   │   │       ├── cache
│   │   │       │   ├── TransactionSearchCacheKey.java
│   │   │       │   ├── TransactionSearchIndex.java
│   │   │       │   └── TransactionSearchIndexInvalidator.java
│   │   │       ├── config
│   │   │       │   └── OpenApiConfig.java
│   │   │       ├── controller
│   │   │       │   ├── AccountController.java
│   │   │       │   ├── BudgetController.java
│   │   │       │   ├── CategoryController.java
│   │   │       │   ├── TransactionController.java
│   │   │       │   ├── UserController.java
│   │   │       │   └── api
│   │   │       │       ├── AccountControllerApi.java
│   │   │       │       ├── BudgetControllerApi.java
│   │   │       │       ├── CategoryControllerApi.java
│   │   │       │       ├── TransactionControllerApi.java
│   │   │       │       └── UserControllerApi.java
│   │   │       ├── domain
│   │   │       │   ├── Account.java
│   │   │       │   ├── AccountType.java
│   │   │       │   ├── Budget.java
│   │   │       │   ├── Category.java
│   │   │       │   ├── Transaction.java
│   │   │       │   ├── TransactionType.java
│   │   │       │   └── User.java
│   │   │       ├── dto
│   │   │       │   ├── request
│   │   │       │   │   ├── AccountRequest.java
│   │   │       │   │   ├── AccountUpdateRequest.java
│   │   │       │   │   ├── BudgetRequest.java
│   │   │       │   │   ├── BudgetUpdateRequest.java
│   │   │       │   │   ├── CategoryRequest.java
│   │   │       │   │   ├── CategoryUpdateRequest.java
│   │   │       │   │   ├── TransactionRequest.java
│   │   │       │   │   ├── TransactionSearchQueryMode.java
│   │   │       │   │   ├── TransactionSearchRequest.java
│   │   │       │   │   ├── TransactionUpdateRequest.java
│   │   │       │   │   ├── TransferDemoRequest.java
│   │   │       │   │   ├── UserRequest.java
│   │   │       │   │   ├── UserUpdateRequest.java
│   │   │       │   │   └── UserWithAccountsAndBudgetsCreateRequest.java
│   │   │       │   └── response
│   │   │       │       ├── AccountResponse.java
│   │   │       │       ├── BudgetResponse.java
│   │   │       │       ├── CategoryResponse.java
│   │   │       │       ├── PageMetadataResponse.java
│   │   │       │       ├── TransactionResponse.java
│   │   │       │       ├── TransactionSearchPageResponse.java
│   │   │       │       ├── TransactionSearchResult.java
│   │   │       │       ├── TransactionSearchSource.java
│   │   │       │       ├── TransferDemoResponse.java
│   │   │       │       └── UserResponse.java
│   │   │       ├── exception
│   │   │       │   ├── ApiException.java
│   │   │       │   ├── ApiErrorField.java
│   │   │       │   ├── ApiErrorResponse.java
│   │   │       │   ├── BadRequestException.java
│   │   │       │   ├── ConflictException.java
│   │   │       │   ├── DuplicateResourceException.java
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── LoggingException.java
│   │   │       │   └── ResourceNotFoundException.java
│   │   │       ├── mapper
│   │   │       │   ├── AccountMapper.java
│   │   │       │   ├── BudgetMapper.java
│   │   │       │   ├── CategoryMapper.java
│   │   │       │   ├── TransactionMapper.java
│   │   │       │   └── UserMapper.java
│   │   │       ├── repository
│   │   │       │   ├── AccountRepository.java
│   │   │       │   ├── BudgetRepository.java
│   │   │       │   ├── CategoryRepository.java
│   │   │       │   ├── TransactionRepository.java
│   │   │       │   └── UserRepository.java
│   │   │       └── service
│   │   │           ├── AccountService.java
│   │   │           ├── BudgetService.java
│   │   │           ├── CategoryService.java
│   │   │           ├── TransactionService.java
│   │   │           ├── UserService.java
│   │   │           └── impl
│   │   │               ├── AccountServiceImpl.java
│   │   │               ├── BudgetServiceImpl.java
│   │   │               ├── CategoryServiceImpl.java
│   │   │               ├── TransactionServiceImpl.java
│   │   │               └── UserServiceImpl.java
│   │   └── resources
│   │       ├── application.properties
│   │       ├── logback-spring.xml
└── README.md
```

В отличие от шаблона из `EXAMPLE`, в текущем проекте используются `CategoryController` и `Category DTO`, а не `TagController`.

---

## 3 ЛИСТИНГ КОДА

### 3.1 Глобальная обработка ошибок

Файл `GlobalExceptionHandler.java`

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(...) { ... }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(...) { ... }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(...) { ... }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(...) { ... }
}
```

Обработчик перехватывает ошибки валидации, ошибки параметров, бизнес-ошибки и неожиданные исключения, после чего формирует единый JSON-ответ.

### 3.2 Единый формат ошибки

Файл `ApiErrorResponse.java`

```java
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiErrorResponse {
    private final OffsetDateTime timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final List<ApiErrorField> fieldErrors;
}
```

Файл `ApiErrorField.java`

```java
@Getter
@AllArgsConstructor
public class ApiErrorField {
    private final String field;
    private final String message;
    private final String rejectedValue;
}
```

### 3.3 Валидация query-параметров поиска

Файл `TransactionSearchRequest.java`

```java
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Query parameters for transaction search with pagination and sorting")
public class TransactionSearchRequest {

    @Size(max = 50)
    private String budgetName;

    @Size(max = 50)
    private String accountName;

    @DecimalMin(value = "0.00")
    private BigDecimal minAmount;

    @NotNull
    @PositiveOrZero
    private Integer page = 0;

    @NotNull
    @Positive
    private Integer size = 5;
}
```

Использование в контроллере:

```java
@GetMapping("/search")
public ResponseEntity<TransactionSearchPageResponse> search(
        @Valid @ModelAttribute TransactionSearchRequest request) {
    ...
}
```

### 3.4 Аспект для времени выполнения

Файл `ServiceLoggingAspect.java`

```java
@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("within(com.finance.tracker.service.impl..*)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        String method = joinPoint.getSignature().toShortString();
        ...
    }
}
```

Аспект логирует время выполнения методов сервисного слоя и отдельно фиксирует неуспешные вызовы.

### 3.5 Конфигурация `logback`

Файл `logback-spring.xml`

```xml
<appender name="APPLICATION_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_DIR}/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>${LOG_DIR}/archive/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>10MB</maxFileSize>
        <maxHistory>14</maxHistory>
        <totalSizeCap>1GB</totalSizeCap>
    </rollingPolicy>
</appender>
```

Отдельно настроен `ERROR_FILE` и уровни логирования.

### 3.6 Swagger/OpenAPI

Файл `OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Tracker API")
                        .version("v1"));
    }
}
```

Swagger-описания endpoint вынесены в интерфейсы `controller/api/*ControllerApi.java`, где используются `@Tag` и `@Operation`, а DTO описаны через `@Schema`.

---

## 4 ДЕМОНСТРАЦИЯ РАБОТЫ

### 4.1 Подготовка

1. Запустить PostgreSQL.
2. Запустить приложение:

```bash
./mvnw spring-boot:run
```

3. Открыть:
   - Postman;
   - браузер со Swagger UI;
   - терминал с `tail -f logs/application.log`;
   - терминал с `tail -f logs/error.log`.

### 4.2 Демонстрация валидации и единого формата ошибки

#### Пример 400

Запрос:

- `POST /api/v1/users`

```json
{
  "username": "",
  "email": "wrong-email"
}
```

Результат:

- `400 Bad Request`
- JSON формата `ApiErrorResponse`
- массив `fieldErrors`

#### Пример 404

Запрос:

- `GET /api/v1/users/999999`

Результат:

- `404 Not Found`
- та же структура JSON

### 4.3 Демонстрация Swagger/OpenAPI

Открыть:

- `http://localhost:8080/swagger-ui.html`

Показать:

- группы endpoint;
- описание DTO;
- схему `TransactionSearchRequest`;
- схему `ApiErrorResponse`.

### 4.4 Демонстрация логирования и AOP

Сначала создать пользователя и два счёта, затем выполнить запрос:

- `GET /api/v1/users/{id}`

В `application.log` показать строку вида:

```text
Service method UserServiceImpl.findById(..) completed in ... ms
```

Затем отправить:

- `POST /api/v1/accounts/transfer?transactional=true`

```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 50.00,
  "failAfterDebit": true
}
```

Показать:

- ошибку `500` в Postman;
- запись в `error.log`;
- запись аспекта о неуспешном вызове.

### 4.5 Демонстрация ротации

Показать файл `logback-spring.xml` и параметры:

- `SizeAndTimeBasedRollingPolicy`
- `maxFileSize`
- `maxHistory`
- `totalSizeCap`

Также показать папку:

- `logs/archive`

Если нужна живая демонстрация, временно уменьшить `maxFileSize` и выполнить серию запросов.

---

## 5 ЗАКЛЮЧЕНИЕ

В ходе лабораторной работы реализована глобальная обработка ошибок через `@ControllerAdvice`, добавлена валидация входных данных с помощью `@Valid` и `@Validated`, введён единый формат `error-response` для всех endpoint. Настроено логирование через `logback`, включая уровни логирования и ротацию логов. Также реализован AOP-аспект для логирования времени выполнения сервисных методов и подключён Swagger/OpenAPI для документирования endpoint и DTO.

Исправленный вариант отчёта приведён в соответствии с текущим проектом `Finance Tracker`, поэтому структура проекта, листинги кода и сценарии демонстрации теперь соответствуют реальному состоянию приложения.
