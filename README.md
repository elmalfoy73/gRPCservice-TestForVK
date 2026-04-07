# gRPC KV Service
Тестовое задание для VK
gRPC сервис на Java для хранения key-value данных в Tarantool 3.2.x.

## Стек
- Java 17
- gRPC / Protobuf
- Tarantool 3.2.x + tarantool-java-sdk 1.5.0

## Запуск

### 1. Запустить Tarantool
```bash
docker-compose up -d
```

### 2. Запустить сервер
```bash
./gradlew run
```
Сервер запускается на порту `9090`.

## API

| Метод | Описание |
|-------|----------|
| `Put(key, value)` | Сохранить или перезаписать значение |
| `Get(key)` | Получить значение по ключу |
| `Delete(key)` | Удалить запись |
| `Range(key_since, key_to)` | Stream записей в диапазоне ключей |
| `Count()` | Количество записей в БД |

## Примеры
```bash
grpcurl -plaintext -proto src/main/proto/kv.proto \
  -d '{"key": "hello", "value": "d29ybGQ="}' \
  localhost:9090 kv.KvService/Put

grpcurl -plaintext -proto src/main/proto/kv.proto \
  -d '{"key": "hello"}' \
  localhost:9090 kv.KvService/Get

grpcurl -plaintext -proto src/main/proto/kv.proto \
  localhost:9090 kv.KvService/Count
```
