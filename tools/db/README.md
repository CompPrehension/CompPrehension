# Инструменты для работы со схемой БД

`dbtool.py` — обёртка над Liquibase CLI и парой вспомогательных Java-классов. Решает три задачи:

1. запуск Liquibase против локальной БД без Spring Boot;
2. сравнение Hibernate-модели (entity-классы) с реальной схемой БД;
3. схлопывание накопившихся миграций в baseline.

Maven-плагин `liquibase:diff` с `liquibase-hibernate6` в этом проекте не работает (конфликт classloader'ов
с `hibernate-types`), поэтому всё запускается на плоском classpath, который собирается из
`mvn dependency:build-classpath` + `target/classes` модулей.

## Требования

- JDK 21, Maven, Python 3.11+ (только стандартная библиотека)
- доступный MySQL; параметры берутся из `.env/compprehension-env.env` (`MYSQL_HOST`, `MYSQL_DATABASE`,
  `MYSQL_USER`, `MYSQL_PASSWORD`) или из флагов `--host --port --db --user --password`

## Команды

```bash
python tools/db/dbtool.py prepare            # mvn compile + сборка classpath и helper-классов
python tools/db/dbtool.py liquibase update   # любая команда Liquibase CLI против БД из .env
python tools/db/dbtool.py liquibase status --verbose
python tools/db/dbtool.py diff-entities      # entity-классы ↔ БД, краткая сводка
python tools/db/dbtool.py diff-entities --full
python tools/db/dbtool.py squash --keep 2026-09-11-01
```

`prepare` выполняется автоматически, если `build/classpath.argfile` ещё нет. После изменения entity-классов
или pom нужно пересобрать: `--rebuild` у любой команды или `prepare` явно.

## diff-entities

Reference — Hibernate-модель пакета `org.vstu.compprehension.entities` (модули `core` и `bkt`),
target — БД. Сводка отбрасывает известный шум: таблицы Liquibase и JobRunr, различия в написании типов
(`bigint` ↔ `BIGINT(19)`, `varchar(255 BYTE)`), `enum` ↔ `varchar` для `@Enumerated(STRING)`,
`tinyint` ↔ `int` для ordinal-enum, имена FK и индексы под ними, порядок колонок в составных PK.

Что Hibernate-модель выразить не может и что всегда будет в отчёте:

- функциональные и prefix-индексы (`ux_permission_scope_kind_item`, `idx_message`);
- PK у `@JoinTable` (`role_permission`);
- FK у таблиц с plain-колонками вместо relation (`bkt_*`);
- FK у связи с `@NotFound(IGNORE)` (`interaction.feedback_id`);
- `*_id_text` generated-колонки (сахар для IDE, в entity намеренно нет);
- `datetime(6)` ↔ `datetime`: Hibernate 6 по умолчанию хочет микросекунды, в БД их нет.

Для работы нужны два патча, лежат в `src/`:

- `dbtools/DiffMySQLDialect.java` — маппинг `JsonType` → `json` для DDL;
- `liquibase/ext/hibernate/snapshot/ColumnSnapshotGenerator.java` — копия класса из
  `liquibase-hibernate6` с правками (разбор `GENERATED ALWAYS AS`, NPE на default'ах). Кладётся раньше
  jar'а в classpath и затеняет оригинал. **При апгрейде `liquibase-hibernate6` патч нужно переносить
  на новую версию** (правки помечены `// PATCH`).

## squash

Схлопывает миграции из `db.changelog-master.xml` в один changeset-baseline:

```
db/changelog/baseline/<date>-baseline-changelog.xml
db/changelog/baseline/schema.sql   # SHOW CREATE TABLE всех таблиц
db/changelog/baseline/data.sql     # INSERT'ы для таблиц, в которых есть seed-данные
```

Что делает:

1. Создаёт временную схему `<db>_squash_a_*`, накатывает на неё только схлопываемые include'ы.
2. Снимает DDL и данные в `baseline/`. Старые baseline-файлы бэкапятся в `build/baseline-backup/`.
3. Создаёт вторую временную схему `<db>_squash_b_*` только из нового baseline.
4. Сравнивает A и B: DDL побайтно, данные через `CHECKSUM TABLE`. При расхождении откатывает
   `baseline/` из бэкапа, сгенерированное кладёт в `build/baseline-failed/` и ничего не удаляет.
5. Переписывает master (baseline + `--keep`), удаляет схлопнутые файлы, дропает временные схемы.

Правила:

- `--keep <подстрока пути>` — миграции, которые остаются после baseline. Сюда должно попасть всё, что ещё
  **не применено на всех окружениях** (прод, стенды): на существующих БД baseline помечается `MARK_RAN`
  и ничего не выполняет, а kept-миграции выполняются как обычно. Когда они доедут до всех окружений,
  их можно схлопнуть следующим `squash`.
- Baseline ставится на пустую БД по условию `not tableExists(--marker-table)`, по умолчанию `exercise`.
- Все include'ы master должны лежать в `modules/server/src/main/resources` — иначе отказ. Файлы из
  других модулей (как было с `strategies`) работают только через classpath jar'а и ломают локальный запуск.
- **Не перегенерировать baseline под той же датой после того, как он применён хоть на одной БД**:
  checksum `sqlFile` считается по содержимому, Liquibase откажет в `update`. Для локальной БД лечится
  `delete from DATABASECHANGELOG where id = '<date>-baseline'` и повторным `update`; для остальных —
  новый `squash` с новой датой.
- Baseline MySQL-специфичный (как и старые миграции — `BIN_TO_UUID`, JSON-функции, generated columns).
- Таблицы `DATABASECHANGELOG*` и `jobs_jobrunr_*` в baseline не входят (первые ведёт Liquibase,
  вторые создаёт JobRunr сам).

После squash: `git status`, просмотр `baseline/`, затем `dbtool.py liquibase update` на своей БД.
