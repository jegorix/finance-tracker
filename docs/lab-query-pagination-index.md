# Лабораторная: JPQL, native query, пагинация и in-memory индекс

## Что было реализовано

В проект добавлен отдельный paged GET-метод для поиска транзакций по набору фильтров, включая фильтрацию по вложенным сущностям:

- `Transaction -> Budget.name`
- `Transaction -> Account.name`

Дополнительно реализованы:

1. JPQL-запрос через `@Query`
2. Аналогичный `native query`
3. Пагинация через `Pageable`
4. In-memory индекс на основе `HashMap<K, V>` для ранее запрошенных результатов
5. Инвалидация индекса при изменении данных

## Новый endpoint

```http
GET /api/v1/transactions/search
```

### Поддерживаемые query params

- `budgetName` — фильтр по имени бюджета
- `accountName` — фильтр по имени счета
- `minAmount` — минимальная сумма транзакции
- `maxAmount` — максимальная сумма транзакции
- `startDateTime` — нижняя граница по `occurredAt`
- `endDateTime` — верхняя граница по `occurredAt`
- `queryMode` — `JPQL` или `NATIVE`
- `page` — номер страницы
- `size` — размер страницы
- `sortBy` — поле сортировки, например `occurredAt` или `id`
- `ascending` — направление сортировки: `true` или `false`

### Что смотреть в Postman

Помимо JSON body, у этого endpoint есть служебный header:

- `X-Transaction-Search-Source: DATABASE`
- `X-Transaction-Search-Source: CACHE`

Он нужен для наглядной демонстрации работы in-memory индекса и его инвалидации.

### Примеры запросов

JPQL:

```http
GET /api/v1/transactions/search?budgetName=Food&accountName=Main&page=0&size=5&sortBy=occurredAt&ascending=false&queryMode=JPQL
```

Native:

```http
GET /api/v1/transactions/search?budgetName=Food&accountName=Main&page=0&size=5&sortBy=occurredAt&ascending=false&queryMode=NATIVE
```

С диапазоном суммы и даты:

```http
GET /api/v1/transactions/search?budgetName=Food&minAmount=20&maxAmount=300&startDateTime=2026-03-01T00:00:00&endDateTime=2026-03-31T23:59:59&queryMode=JPQL&page=0&size=10&sortBy=occurredAt&ascending=false
```

## Как это устроено

### 1. JPQL-запрос

JPQL-запрос находится в `TransactionRepository`.

Он делает:

- `LEFT JOIN t.budget b`
- `JOIN t.account a`
- фильтрацию по `b.name`
- фильтрацию по `a.name`
- фильтрацию по диапазону суммы
- фильтрацию по диапазону даты

Этот запрос работает на уровне JPA-сущностей и использует `@Query`.

### 2. Native query

Для той же логики добавлен отдельный `native query`.

Он работает уже напрямую по таблицам:

- `transactions`
- `accounts`
- `budgets`

Добавлен также `countQuery`, потому что для пагинации `Pageable` нужен не только основной запрос, но и подсчёт общего количества строк.

### 3. Пагинация

Пагинация реализована через `Pageable`.

В контроллере стоит:

- дефолтный размер страницы `5`
- дефолтная сортировка `occurredAt desc`

Во внешний JSON не отдаётся сырой Spring `Page`, потому что он сериализуется слишком многословно.

Вместо этого контроллер возвращает компактный объект:

- `content`
- `page.size`
- `page.number`
- `page.totalElements`
- `page.totalPages`

## In-memory индекс

### Что именно индексируется

Индекс хранит уже полученные результаты поиска транзакций.

Если пользователь повторно вызывает тот же запрос с теми же параметрами:

- тот же `queryMode`
- те же фильтры
- те же `page/size/sort`

то результат берётся не из БД, а из памяти.

Это можно увидеть в Postman по header `X-Transaction-Search-Source`.

### Где хранится индекс

Используется отдельный компонент:

- `TransactionSearchIndex`

Внутри него хранится:

```java
HashMap<TransactionSearchCacheKey, Page<TransactionResponse>>
```

То есть:

- `K` — составной ключ запроса
- `V` — готовая страница результатов

### Как устроен составной ключ

Создан отдельный класс:

- `TransactionSearchCacheKey`

В ключ включены все параметры, которые влияют на результат запроса:

- `queryMode`
- `budgetName`
- `accountName`
- `minAmount`
- `maxAmount`
- `startDateTime`
- `endDateTime`
- `pageNumber`
- `pageSize`
- `sort`

Это важно, потому что запросы:

- с разным `page`
- с разным `size`
- с разной сортировкой
- с разными фильтрами

не должны считаться одинаковыми.

### Почему отдельно реализованы equals() и hashCode()

Так как `HashMap` использует `equals()` и `hashCode()` для поиска ключа, класс `TransactionSearchCacheKey` должен корректно сравнивать ключи.

Если бы `equals()` и `hashCode()` были реализованы неправильно, возникли бы проблемы:

- одинаковые запросы не находились бы в индексе
- разные запросы могли бы считаться одинаковыми

В этой реализации:

- `equals()` сравнивает все поля ключа
- `hashCode()` строится на основе тех же полей

Это обеспечивает корректную работу индекса.

## Инвалидация индекса

### Почему она нужна

Если данные в БД изменились, старые cached-результаты становятся неактуальными.

Например:

- изменилась транзакция
- изменилось имя счёта
- изменилось имя бюджета
- бюджет был удалён, а у транзакций `budget_id` стал `NULL`

Если индекс не очищать, клиент будет получать устаревшие данные.

### Как реализована инвалидация

Добавлен отдельный компонент:

- `TransactionSearchIndexInvalidator`

Он очищает индекс:

- либо сразу
- либо после успешного commit транзакции

Используется `TransactionSynchronizationManager`, чтобы инвалидация происходила после commit, а не до него.

Это важный момент:

- если транзакция откатится, индекс не должен очищаться как будто изменения сохранились

### В каких случаях индекс инвалидируется

Инвалидация подключена в сервисах, которые меняют данные, влияющие на поиск:

- `TransactionServiceImpl`
  - `create`
  - `update`
  - `patch`
  - `delete`
- `AccountServiceImpl`
  - `create`
  - `update`
  - `delete`
- `BudgetServiceImpl`
  - `create`
  - `update`
  - `delete`

## Какие файлы были добавлены или изменены

### Добавлены

- `src/main/java/com/finance/tracker/dto/request/TransactionSearchQueryMode.java`
- `src/main/java/com/finance/tracker/cache/TransactionSearchCacheKey.java`
- `src/main/java/com/finance/tracker/cache/TransactionSearchIndex.java`
- `src/main/java/com/finance/tracker/cache/TransactionSearchIndexInvalidator.java`
- `docs/lab-query-pagination-index.md`

### Изменены

- `src/main/java/com/finance/tracker/controller/TransactionController.java`
- `src/main/java/com/finance/tracker/service/TransactionService.java`
- `src/main/java/com/finance/tracker/service/impl/TransactionServiceImpl.java`
- `src/main/java/com/finance/tracker/repository/TransactionRepository.java`
- `src/main/java/com/finance/tracker/service/impl/AccountServiceImpl.java`
- `src/main/java/com/finance/tracker/service/impl/BudgetServiceImpl.java`
- `docs/api.md`

## Что можно показать на защите

### 1. Одинаковый запрос двумя способами

Показать два запроса:

```http
GET /api/v1/transactions/search?budgetName=Food&accountName=Main&queryMode=JPQL&page=0&size=5&sortBy=occurredAt&ascending=false
GET /api/v1/transactions/search?budgetName=Food&accountName=Main&queryMode=NATIVE&page=0&size=5&sortBy=occurredAt&ascending=false
```

И объяснить, что логика фильтрации одинаковая, отличается только способ доступа к БД.

### 2. Пагинацию

Показать:

```http
GET /api/v1/transactions/search?queryMode=JPQL&page=0&size=3&sortBy=id&ascending=true
GET /api/v1/transactions/search?queryMode=JPQL&page=1&size=3&sortBy=id&ascending=true
```

И объяснить, что:

- первая страница и вторая страница имеют разные ключи индекса
- поэтому кешируются отдельно

### 3. Инвалидацию

1. Выполнить поиск
2. Изменить транзакцию или бюджет
3. Повторить тот же поиск

И пояснить, что индекс был очищен после изменения данных, поэтому следующий запрос был пересчитан по БД заново.

## Итог

В лабораторной добавлен полноценный сценарий поиска:

- сложная фильтрация по вложенным сущностям
- две реализации запроса: JPQL и native query
- пагинация через `Pageable`
- in-memory индекс на `HashMap`
- корректный составной ключ
- инвалидация индекса при изменении данных

Это покрывает все пункты задания и даёт готовую основу для демонстрации на защите.
