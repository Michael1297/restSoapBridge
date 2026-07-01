# REST SOAP Bridge

Универсальный мост REST → SOAP. Принимает JSON по HTTP, вызывает SOAP-операцию через Apache CXF и возвращает JSON.

Swagger UI показывает все доступные эндпоинты (в режиме `auto` — все операции из WSDL).

## Требования

- Java 17+
- Maven 3.9+
- Доступный SOAP-бэкенд (например, CXF на `http://host:8080/services/`)

## Сборка

```bash
mvn clean package -DskipTests
```

JAR-файл:

```
target/restSoapBridge-1.0-SNAPSHOT.jar
```

## Запуск

```bash
java -jar target/restSoapBridge-1.0-SNAPSHOT.jar
```

После старта:

| URL | Описание |
|-----|----------|
| `http://localhost:9194/swagger-ui.html` | Swagger UI |
| `http://localhost:9194/v3/api-docs` | OpenAPI JSON |
| `http://localhost:9194/api/...` | REST-эндпоинты моста |

Порт по умолчанию — **9194** (см. `server.port`).

---

## Настройки приложения

Все параметры задаются через `application.yml` или стандартные механизмы Spring Boot (переменные окружения, аргументы командной строки, внешние файлы).

### Основные параметры

```yaml
server:
  port: 9194

bridge:
  mappings-file: auto          # auto | путь к YAML-файлу маппингов
  auto:
    services-url: http://127.0.0.1:8080/services/
    path-prefix: /api
    http-method: POST

logging:
  level:
    io.github.connellite: DEBUG
```

### `bridge.mappings-file`

| Значение | Режим |
|----------|-------|
| `auto` | Маппинги генерируются при старте из WSDL всех сервисов на `bridge.auto.services-url` |
| `classpath:mappings.yml` | Ручной маппинг из файла внутри JAR |
| `file:/opt/bridge/mappings.yml` | Ручной маппинг из внешнего файла |
| `file:./config/mappings.yml` | Ручной маппинг из файла рядом с JAR (относительно рабочей директории) |

### `bridge.auto.*` (только для режима `auto`)

| Параметр | Описание | Пример |
|----------|----------|--------|
| `services-url` | URL страницы со списком CXF-сервисов | `http://127.0.0.1:8080/services/` |
| `path-prefix` | Префикс REST-путей | `/api` |
| `http-method` | HTTP-метод для всех сгенерированных эндпоинтов | `POST` |

В режиме `auto` для каждой SOAP-операции создаётся эндпоинт:

```
{path-prefix}/{ServiceName}/{operationName}
```

Пример: `POST /api/Auth/login`

---

## Переменные окружения

Spring Boot автоматически маппит свойства в переменные окружения (UPPER_SNAKE_CASE):

| Свойство | Переменная окружения |
|----------|---------------------|
| `server.port` | `SERVER_PORT` |
| `bridge.mappings-file` | `BRIDGE_MAPPINGS_FILE` |
| `bridge.auto.services-url` | `BRIDGE_AUTO_SERVICES_URL` |
| `bridge.auto.path-prefix` | `BRIDGE_AUTO_PATH_PREFIX` |
| `bridge.auto.http-method` | `BRIDGE_AUTO_HTTP_METHOD` |

Пример:

```bash
export SERVER_PORT=9194
export BRIDGE_MAPPINGS_FILE=file:/opt/bridge/mappings.yml
java -jar restSoapBridge-1.0-SNAPSHOT.jar
```

---

## Конфигурация после сборки (снаружи JAR)

Spring Boot позволяет переопределять настройки без пересборки.

### 1. Аргументы командной строки

```bash
java -jar restSoapBridge-1.0-SNAPSHOT.jar \
  --server.port=9194 \
  --bridge.mappings-file=file:./config/mappings.yml \
  --bridge.auto.services-url=http://192.168.1.10:8080/services/
```

### 2. Внешний `application.yml`

Положите файл рядом с JAR или в отдельную папку:

```
/opt/bridge/
├── restSoapBridge-1.0-SNAPSHOT.jar
├── application.yml          # переопределяет настройки из JAR
└── mappings.yml             # ручной маппинг (если нужен)
```

`application.yml`:

```yaml
server:
  port: 9194

bridge:
  mappings-file: file:./mappings.yml
  auto:
    services-url: http://192.168.1.10:8080/services/
    path-prefix: /api
    http-method: POST
```

Запуск из той же директории:

```bash
cd /opt/bridge
java -jar restSoapBridge-1.0-SNAPSHOT.jar
```

Spring Boot автоматически подхватит `./application.yml` из рабочей директории.

### 3. Явное указание каталога конфигурации

```bash
java -jar restSoapBridge-1.0-SNAPSHOT.jar \
  --spring.config.additional-location=file:./config/
```

Структура:

```
/opt/bridge/
├── restSoapBridge-1.0-SNAPSHOT.jar
└── config/
    ├── application.yml
    └── mappings.yml
```

`config/application.yml`:

```yaml
bridge:
  mappings-file: file:./config/mappings.yml
```

### 4. Только внешний файл маппингов (остальное — из JAR)

```bash
java -jar restSoapBridge-1.0-SNAPSHOT.jar \
  --bridge.mappings-file=file:/etc/rest-soap-bridge/mappings.yml
```

### 5. Docker / systemd

```bash
# docker run
docker run -p 9194:9194 \
  -e BRIDGE_MAPPINGS_FILE=auto \
  -e BRIDGE_AUTO_SERVICES_URL=http://soap-backend:8080/services/ \
  -v /host/config/mappings.yml:/config/mappings.yml \
  rest-soap-bridge:latest
```

---

## Настройка маппинга (ручной режим)

При `bridge.mappings-file: file:./mappings.yml` (или `classpath:mappings.yml`) маппинги описываются в YAML.

### Структура файла

```yaml
mappings:
  - path: /api/auth/login       # REST-путь
    method: POST                # HTTP-метод
    soap:
      service: Auth         # имя SOAP-сервиса (для Swagger-тега)
      operation: login          # имя SOAP-операции
      wsdl: http://127.0.0.1:8080/services/Auth?wsdl
    request:                    # REST JSON → SOAP-аргументы
      "$.user": "$.login.user"
      "$.pass": "$.login.pass"
    response:                   # SOAP-ответ → REST JSON
      "#root.return": "$.token"
```

### Формат путей

| Направление | Формат | Пример | Библиотека |
|-------------|--------|--------|------------|
| REST request (ключ) | [JsonPath](https://github.com/json-path/JsonPath) | `$.user` | Jayway JsonPath |
| SOAP request (значение) | JsonPath | `$.login.user` | Jayway JsonPath |
| SOAP response (ключ) | [SpEL](https://docs.spring.io/spring-framework/reference/core/expressions.html) | `#root.return` | Spring Expression Language |
| REST response (значение) | JsonPath | `$.token` | Jayway JsonPath |

#### Request

Ключ — поле во входящем JSON-теле. Значение — куда положить аргумент в структуре SOAP-вызова.

Пример: REST-тело `{"user":"admin","pass":"secret"}` с маппингом `"$.user": "$.login.user"` передаёт в SOAP аргумент `user=admin` внутри группы `login`.

#### Response

Ключ — SpEL-выражение для чтения из SOAP-ответа (`#root` — корневой объект ответа CXF). Значение — куда записать поле в исходящий JSON.

Пример: `"#root.return": "$.token"` → из SOAP-объекта читается свойство `return`, в REST-ответе появляется `{"token":"..."}`.

### Несколько эндпоинтов

```yaml
mappings:
  - path: /api/auth/login
    method: POST
    soap:
      service: Auth
      operation: login
      wsdl: http://127.0.0.1:8080/services/Auth?wsdl
    request:
      "$.user": "$.login.user"
      "$.pass": "$.login.pass"
    response:
      "#root.return": "$.token"

  - path: /api/custom/endpoint
    method: POST
    soap:
      service: SomeService
      operation: doSomething
      wsdl: http://127.0.0.1:8080/services/SomeService?wsdl
    request:
      "$.id": "$.request.id"
    response:
      "#root.result": "$.result"
```

---

## Автоматический режим (`auto`)

При `bridge.mappings-file: auto` мост:

1. Загружает HTML-страницу `bridge.auto.services-url`
2. Находит все WSDL-ссылки (CXF services page)
3. Парсит WSDL и XSD
4. Генерирует маппинги вида:

```yaml
# генерируется автоматически, в файл не пишется
path: /api/Auth/login
method: POST
request:
  "$.user": "$.login.user"
response:
  "#root.return": "$.return"
```

Ручной `mappings.yml` в этом режиме **не используется**.

---

## Пример запроса

```bash
curl -X POST http://localhost:9194/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"user":"admin","pass":"secret"}'
```

Ответ (при успешной авторизации):

```json
{"token":"<session-id>"}
```

---

## Типичные ошибки

| HTTP | Причина |
|------|---------|
| 404 | Нет маппинга для `METHOD + path` |
| 401 | SOAP `AccessDeniedException` |
| 502 | Ошибка SOAP или недоступен бэкенд |

---

## Рекомендуемая структура для продакшена

```
/etc/rest-soap-bridge/
├── application.yml       # порт, URL SOAP-бэкенда, режим
└── mappings.yml          # ручные маппинги (если не auto)

/var/lib/rest-soap-bridge/
└── restSoapBridge-1.0-SNAPSHOT.jar
```

Запуск:

```bash
java -jar /var/lib/rest-soap-bridge/restSoapBridge-1.0-SNAPSHOT.jar \
  --spring.config.additional-location=file:/etc/rest-soap-bridge/application.yml \
  --bridge.mappings-file=file:/etc/rest-soap-bridge/mappings.yml
```

Или положить `application.yml` в рабочую директорию и запускать из неё — Spring Boot подхватит его автоматически.
