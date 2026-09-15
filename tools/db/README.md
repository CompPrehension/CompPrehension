# dbtool — инструменты для работы со схемой БД

Утилита объединяет три операции над схемой базы данных, которые неудобно выполнять через Spring Boot:

1. запуск Liquibase CLI против произвольной базы;
2. сравнение Hibernate-модели (entity-классов) с реальной схемой;
3. схлопывание накопившихся миграций в baseline.

Maven-плагин `liquibase:diff` с `liquibase-hibernate7` в этом проекте неработоспособен (конфликт
classloader'ов с `hypersistence-utils`), поэтому Liquibase, Hibernate и entity-классы модулей `core` и `bkt`
собираются в один исполняемый jar и запускаются в одном процессе.

## Сборка

```bash
./mvnw -q -Ptools -pl tools/db -am package -DskipTests
```

Модуль подключается Maven-профилем `tools` и не участвует в сборке проекта по умолчанию.
Результат — `tools/db/target/dbtool.jar`. После изменения entity-классов jar нужно пересобрать.

## Подключение к базе данных

Параметры подключения задаются одним из способов:

- явно: `--host HOST --port PORT --db NAME --user USER --password PASSWORD`;
- из файла окружения: `--env FILE` (по умолчанию `.env/compprehension-env.env` в корне репозитория);
  используются переменные `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`.

Явные параметры имеют приоритет над файлом окружения.

Корень репозитория определяется автоматически по текущему каталогу (поиск вверх до
`modules/server/src/main/resources/db/changelog/db.changelog-master.xml`); его можно задать явно через `--root DIR`.
Рабочие файлы (отчёты, временные changelog'и, резервные копии) размещаются в `tools/db/target/work/`.

## Команды

```bash
java -jar tools/db/target/dbtool.jar liquibase update          # любая команда Liquibase CLI
java -jar tools/db/target/dbtool.jar liquibase status --verbose
java -jar tools/db/target/dbtool.jar diff-entities             # entity-классы ↔ БД, краткая сводка
java -jar tools/db/target/dbtool.jar diff-entities --full      # полный отчёт Liquibase
java -jar tools/db/target/dbtool.jar squash --keep 2026-09-11-01
```

Команде `liquibase` передаются все аргументы, не распознанные как параметры подключения; URL, учётные
данные, `--search-path` и `--changelog-file` подставляются автоматически.

## diff-entities

Reference — Hibernate-модель пакета `org.vstu.compprehension.entities` (модули `core` и `bkt`),
target — база данных. Сводка отбрасывает известный шум: таблицы Liquibase и JobRunr, различия в написании типов
(`bigint` ↔ `BIGINT(19)`, `varchar(255 BYTE)`), `enum` ↔ `varchar` для `@Enumerated(STRING)`,
`tinyint` ↔ `int` для ordinal-enum, имена FK и индексы под ними, порядок колонок в составных PK.

Что Hibernate-модель выразить не может и что всегда присутствует в отчёте:

- функциональные и prefix-индексы (`ux_permission_scope_kind_item`, `idx_message`);
- PK у `@JoinTable` (`role_permission`);
- FK у таблиц с plain-колонками вместо relation (`bkt_*`);
- FK у связи с `@NotFound(IGNORE)` (`interaction.feedback_id`);
- `*_id_text` generated-колонки (вспомогательные для IDE, в entity намеренно отсутствуют);
- `datetime(6)` ↔ `datetime`: Hibernate по умолчанию требует микросекунды, в базе их нет.

Для работы нужны два патча в `src/main/java`:

- `org/vstu/compprehension/tools/db/DiffMySQLDialect.java` — маппинг `JsonType` → `json` для DDL;
- `liquibase/ext/hibernate/snapshot/ColumnSnapshotGenerator.java` — копия класса из
  `liquibase-hibernate7` с правками (разбор `GENERATED ALWAYS AS`, NPE на default'ах). Оригинальный класс
  исключается из jar при сборке (см. `pom.xml`), копия занимает его место. **При обновлении
  `liquibase-hibernate7` патч нужно переносить на новую версию** (правки помечены `// PATCH`).

## squash

Схлопывает миграции из `db.changelog-master.xml` в один changeset-baseline:

```
db/changelog/baseline/<date>-baseline-changelog.xml
db/changelog/baseline/schema.sql   # SHOW CREATE TABLE всех таблиц
db/changelog/baseline/data.sql     # INSERT'ы для таблиц, в которых есть seed-данные
```

Порядок работы:

1. Создаётся временная схема `<db>_squash_a_*`, на неё накатываются только схлопываемые include'ы.
2. DDL и данные снимаются в `baseline/`. Предыдущие baseline-файлы сохраняются в `target/work/baseline-backup/`.
3. Создаётся вторая временная схема `<db>_squash_b_*` только из нового baseline.
4. Схемы A и B сравниваются: DDL побайтно, данные через `CHECKSUM TABLE`. При расхождении `baseline/`
   восстанавливается из резервной копии, сгенерированные файлы переносятся в `target/work/baseline-failed/`,
   changelog'и не удаляются.
5. Переписывается master (baseline + `--keep`), схлопнутые файлы удаляются, временные схемы удаляются.

| Флаг | Назначение |
|---|---|
| `--keep SUBSTRING` | подстрока пути include'а, который остаётся после baseline; повторяемый |
| `--date YYYY-MM-DD` | дата baseline (по умолчанию текущая) |
| `--author NAME` | автор changeset'а |
| `--marker-table TABLE` | таблица, отсутствие которой означает пустую базу (по умолчанию `exercise`) |
| `--keep-schemas` | не удалять временные схемы |

Правила:

- В `--keep` должно попасть всё, что ещё **не применено на всех окружениях** (прод, стенды): на существующих
  базах baseline помечается `MARK_RAN` и ничего не выполняет, а сохранённые миграции выполняются как обычно.
  После того как они применены везде, их можно схлопнуть следующим `squash`.
- Baseline применяется на пустую базу по условию `not tableExists(--marker-table)`.
- Все include'ы master должны находиться в `modules/server/src/main/resources`; иначе утилита завершается
  с ошибкой. Файлы из других модулей работают только через classpath jar'а и ломают локальный запуск.
- **Не следует перегенерировать baseline под той же датой после того, как он применён хотя бы на одной базе**:
  checksum `sqlFile` считается по содержимому, и Liquibase откажет в `update`. Для локальной базы это лечится
  `delete from DATABASECHANGELOG where id = '<date>-baseline'` и повторным `update`; для остальных —
  новым `squash` с новой датой.
- Baseline MySQL-специфичен (как и исходные миграции — `BIN_TO_UUID`, JSON-функции, generated columns).
- Таблицы `DATABASECHANGELOG*` и `jobs_jobrunr_*` в baseline не входят (первые ведёт Liquibase,
  вторые создаёт JobRunr).

После squash: `git status`, просмотр `baseline/`, затем `dbtool liquibase update` на своей базе.
