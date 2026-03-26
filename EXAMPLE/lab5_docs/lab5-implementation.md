# Лабораторная работа 5: что было сделано

## Краткий итог

В проекте `Finance Tracker` реализованы все пункты лабораторной:

1. добавлена bulk-операция с бизнес-смыслом;
2. в сервисном слое используются `Stream API` и `Optional`;
3. bulk-операция реализована в двух режимах: с транзакцией и без неё;
4. написаны unit-тесты сервисного слоя на `JUnit 5` и `Mockito`.

Ниже по пунктам описано, что именно было изменено.

---

## 1. Bulk-операция

### Что реализовано

В качестве bulk-операции добавен массовый импорт транзакций:

- `POST /api/v1/transactions/bulk`

Операция принимает:

- список объектов `TransactionRequest`;
- query-параметр `transactional`, который определяет режим выполнения:
  - `true` — все элементы создаются в одной транзакции;
  - `false` — операция выполняется без общей транзакции.

### Почему это имеет бизнес-смысл

Для финансового трекера массовое создание транзакций является естественным сценарием:

- импорт выписки из банка;
- разовое добавление набора доходов и расходов;
- перенос исторических операций из другого сервиса.

### Какие файлы были изменены

- `src/main/java/com/finance/tracker/controller/api/TransactionControllerApi.java`
- `src/main/java/com/finance/tracker/controller/TransactionController.java`
- `src/main/java/com/finance/tracker/service/TransactionService.java`
- `src/main/java/com/finance/tracker/service/impl/TransactionServiceImpl.java`

### Как устроено на уровне контроллера

В `TransactionController` добавлен endpoint:

```java
@PostMapping("/bulk")
public ResponseEntity<List<TransactionResponse>> createBulk(
        @RequestBody List<TransactionRequest> requests,
        @RequestParam(defaultValue = "true") boolean transactional) {
    List<TransactionResponse> response = transactional
            ? transactionService.createBulkTx(requests)
            : transactionService.createBulkNoTx(requests);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

Контроллер только принимает запрос и делегирует выполнение сервису.

---

## 2. Использование Stream API и Optional

### Где используется `Optional`

`Optional` применяется в сервисном слое для безопасной обработки необязательных данных.

#### Пример 1. Проверка bulk-запроса

В методе `createBulkInternal(...)`:

```java
List<TransactionRequest> bulkRequests = Optional.ofNullable(requests)
        .filter(items -> !items.isEmpty())
        .orElseThrow(() -> new BadRequestException(BULK_REQUEST_EMPTY_MESSAGE));
```

Что это даёт:

- защита от `null`;
- защита от пустого списка;
- компактная запись проверки.

#### Пример 2. Необязательный `budgetId`

В методе `createTransactionEntity(...)`:

```java
Budget budget = Optional.ofNullable(request.getBudgetId())
        .map(this::getBudget)
        .orElse(null);
```

Что это даёт:

- если `budgetId` передан, бюджет загружается из БД;
- если `budgetId` не передан, в транзакции сохраняется `null`.

### Где используется `Stream API`

`Stream API` используется для обработки коллекций.

#### Пример 1. Массовое создание транзакций

```java
return bulkRequests.stream()
        .map(this::createTransactionEntity)
        .map(transaction -> transactionMapper.toResponse(transaction, true, true))
        .toList();
```

Смысл цепочки:

1. взять каждый запрос из списка;
2. преобразовать его в сохранённую сущность `Transaction`;
3. преобразовать сущность в `TransactionResponse`;
4. собрать результат в список.

#### Пример 2. Преобразование сущностей в response DTO

```java
return transactions.stream()
        .map(transaction -> transactionMapper.toResponse(transaction, true, true))
        .toList();
```

### Почему это полезно

- код короче и читается проще;
- уменьшается количество ручных циклов;
- бизнес-логика выражена декларативно.

---

## 3. Транзакционность bulk-операции

### Что реализовано

В сервисе добавлены два метода:

- `createBulkTx(List<TransactionRequest> requests)`
- `createBulkNoTx(List<TransactionRequest> requests)`

Оба метода используют одну общую внутреннюю бизнес-логику, но отличаются режимом транзакции.

### Режим с транзакцией

```java
@Transactional
public List<TransactionResponse> createBulkTx(List<TransactionRequest> requests) {
    return createBulkInternal(requests);
}
```

Если хотя бы один элемент списка вызывает ошибку:

- Spring откатывает всю операцию;
- в БД не остаётся частично сохранённых транзакций.

### Режим без общей транзакции

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public List<TransactionResponse> createBulkNoTx(List<TransactionRequest> requests) {
    return createBulkInternal(requests);
}
```

Если ошибка возникает на середине списка:

- уже выполненные сохранения не откатываются;
- в БД остаётся частичный результат.

### Какую разницу это показывает

Это наглядно демонстрирует смысл `@Transactional`:

- с транзакцией операция атомарна;
- без транзакции система может перейти в частично обновлённое состояние.

Именно эту разницу удобно показывать преподавателю в Postman и через SQL-запрос к таблице `transactions`.

---

## 4. Unit-тесты сервисного слоя

### Что написано

Для лабораторной добавлены unit-тесты сервиса транзакций:

- `src/test/java/com/finance/tracker/service/impl/TransactionServiceImplTest.java`

### Какие сценарии покрыты

#### Успешное массовое создание

Тест `createBulkTxShouldCreateAllTransactions()` проверяет:

- корректное создание нескольких транзакций;
- формирование списка ответов;
- вызов `transactionRepository.save(...)` нужное число раз;
- вызов инвалидации поискового индекса.

#### Пустой bulk-запрос

Тест `createBulkTxShouldRejectEmptyRequest()` проверяет:

- выброс `BadRequestException`;
- отсутствие обращений к репозиториям.

#### Бизнес-конфликт владельцев

Тест `createShouldFailWhenBudgetOwnerAndAccountOwnerDoNotMatch()` проверяет:

- выброс `ConflictException`, если бюджет и счёт принадлежат разным пользователям;
- отсутствие сохранения транзакции.

#### Patch с необязательными полями

Тест `patchShouldKeepCurrentValuesWhenOptionalFieldsAreMissing()` проверяет:

- сохранение существующих значений, если часть полей не передана;
- корректный результат `TransactionResponse`.

### Почему используется Mockito

`Mockito` позволяет:

- изолировать сервисный слой от реальной БД;
- подменить поведение репозиториев;
- проверить вызовы зависимостей.

Такой тест проверяет именно бизнес-логику сервиса, а не работу Spring MVC или PostgreSQL.

### Дополнительная настройка

Для корректной работы Mockito в текущем окружении добавлен файл:

- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

Со значением:

```text
mock-maker-subclass
```

Это нужно для стабильного запуска тестов на текущем JDK.

---

## 5. Что получилось в итоге

После выполнения лабораторной в проекте появился законченный сценарий массовой обработки транзакций:

- есть bulk-endpoint;
- есть выбор режима выполнения с транзакцией и без неё;
- есть использование `Optional` и `Stream API` в сервисе;
- есть unit-тесты на ключевые сценарии.

Таким образом лабораторная работа 5 реализована не формально, а как полезная часть реального функционала проекта `Finance Tracker`.
