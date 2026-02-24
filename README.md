# Finance Tracker

**Finance Tracker** — это REST API для управления личными финансами, разработанное на Spring Boot. Приложение позволяет вести учёт доходов и расходов, сохраняя информацию о каждой транзакции: сумму, категорию и дату. API предоставляет возможность получать список всех операций, фильтровать их по категориям и просматривать детали конкретной транзакции.


## Лабораторные работы

### Лаба 1 — Basic REST Service
- Spring Boot приложение.
- REST API для сущности Transaction.
- GET с `@PathVariable` и `@RequestParam`.
- Слои: Controller → Service → Repository.
- DTO + Mapper (MapStruct).
- Checkstyle.

## Сущности

### Transaction (Транзакция)

| Поле | Тип | Описание |
|------|-----|----------|
| id | Long | Первичный ключ, автоинкремент |
| amount | BigDecimal | Сумма транзакции |
| category | String | Категория (Food, Transport, и т.д.) |
| date | LocalDate | Дата транзакции |

## Запуск

### Требования
- Java 17+
- Maven
- PostgreSQL

### Быстрый старт

1. Создать базу данных в PostgreSQL:
   ```sql
   CREATE DATABASE finance_tracker;
   ```

2. Указать параметры подключения в `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/finance_tracker
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   ```

3. Запустить приложение:
   ```bash
   mvn spring-boot:run
   ```

## API

| Метод | URL | Описание |
|-------|-----|----------|
| GET | `/api/transactions/{id}` | Получить транзакцию по ID |
| GET | `/api/transactions?category={category}` | Поиск транзакций по категории (регистронезависимый) |
