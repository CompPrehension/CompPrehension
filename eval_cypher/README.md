## eval_cypher

Этот каталог содержит экспериментальный код для имитации рассуждающей модели (дерева `CtrlFlow`) поверх графа Neo4j с помощью Cypher‑правил и внешнего Python‑цикла.

### Общие принципы

- **Доменные данные неизменяемы**: узлы и связи, соответствующие моделям из `domain.loqi` (`ns0__Node`, `ns0__Edge`, `ns0__PathInfo`, `ns0__TraceAct`, `ns0__State`, и т.п.), не модифицируются правилами рассуждения.
- **Результаты reasoning-а отделены**: все новые сущности создаются как узлы с меткой `:Resource` и дополнительными метками вида `:ns0__Run`, `:ns0__Inference` и т.п.
- **Очистка по Run**: каждый прогон помечается отдельным узлом `(:Resource:ns0__Run)`; все результаты этого прогона имеют связь `:ns0__hasRun` на соответствующий `Run`. Очистка выполняется удалением `Run` и всех узлов, связанных с ним через `:ns0__hasRun`.

### Базовые сущности reasoning-а

- `(:Resource:ns0__Run {uri, ns0__id})`
  - представляет один прогон reasoning-а;
  - `uri` — IRI вида `http://www.vstu.ru/poas/code#run_<ID>`;
  - `ns0__id` — массив с тем же идентификатором, что и в `uri`.

- `(:Resource:ns0__Inference {ns0__ruleId, ns0__runUri, ns0__targetElement, ns0__outcome})`
  - один выведенный факт (аналог узла `conclude` в дереве);
  - `ns0__ruleId` — строковый идентификатор правила;
  - `ns0__runUri` — `uri` соответствующего `Run`;
  - `ns0__targetElement` — `elementId` доменного узла, к которому относится вывод (например, `TraceAct`);
  - `ns0__outcome` — `'correct'`, `'error'` или `'unknown'` (массив строк для согласованности с импортированными свойствами).

- Связи:
  - `(inf:ns0__Inference)-[:ns0__hasRun]->(run:ns0__Run)` — принадлежность факта конкретному прогону;
  - `(inf:ns0__Inference)-[:ns0__aboutTraceAct]->(ta:ns0__TraceAct)` — связь факта с доменным узлом трассы (по мере необходимости могут добавляться другие типы связей, например к `PathInfo`).

### Очистка результатов по Run

Для удаления результатов reasoning-а конкретного прогона, не затрагивая исходные данные, используется следующая команда (вариант по `uri` Run):

```cypher
// Очистка всех результатов reasoning-а для одного прогона
// Параметр:
//   $runUri — IRI узла :ns0__Run, например "http://www.vstu.ru/poas/code#run_202602_debug21"

MATCH (run:Resource:ns0__Run {uri: $runUri})
OPTIONAL MATCH (run)<-[:ns0__hasRun]-(res:Resource)
WITH run, collect(DISTINCT res) AS results
FOREACH (n IN results | DETACH DELETE n)
DETACH DELETE run;
```

Если удобнее работать по логическому идентификатору (`ns0__id`), можно использовать вариант:

```cypher
// Очистка по ns0__id Run
// Параметр:
//   $runId — логический идентификатор (строка), например "run_202602_debug21"

MATCH (run:Resource:ns0__Run)
WHERE $runId IN run.ns0__id
OPTIONAL MATCH (run)<-[:ns0__hasRun]-(res:Resource)
WITH run, collect(DISTINCT res) AS results
FOREACH (n IN results | DETACH DELETE n)
DETACH DELETE run;
```

Обе команды предполагают, что:

- все новые факты (и другие технические узлы reasoning-а) всегда имеют связь `:ns0__hasRun` на свой `Run`;
- доменные узлы (трасса, CFG, PathInfo, AST и т.д.) сами по себе не удаляются и не изменяются.

### Дальнейшее развитие

- реализация простых конструкций дерева Loqi (`conclude`, `ask`, `out`) как набора Cypher‑правил;
- внешний Python‑цикл, который запускает набор правил до тех пор, пока `summary.counters.*_created` остаются нулевыми (forward‑chaining до насыщения);
- возможная альтернатива — реализация того же цикла через `apoc.do.until`.

