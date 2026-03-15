"""
Experimental forward-chaining reasoner over Neo4j for simple Loqi-like constructs.

Current focus:
- keep domain data (imported from domain.loqi / exp_*.ttl) immutable;
- model simple `conclude`-like behaviour via separate inference nodes;
- drive rules from an external Python loop until a fixpoint is reached.

All new nodes are attached to a :ns0__Run node via :ns0__hasRun so they can
be removed without touching domain objects.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, List, Tuple
import sys
import time

from neo4j import GraphDatabase, Session


DATABASE = "loqi"

@dataclass
class Neo4jConfig:
    uri: str = "bolt://localhost:7687"
    user: str = "neo4j"
    password: str = "password"
    # database: str = "loqi"


class SimpleReasoner:
    """
    Minimal external forward-chaining loop over a fixed set of Cypher rules.

    For now we implement a single very simple rule that mimics a `conclude`
    over TraceAct.is_known_correct:
      - is_known_correct = true  -> outcome 'correct'
      - is_known_correct = false -> outcome 'error'
    """

    def __init__(self, config: Neo4jConfig) -> None:
        self._driver = GraphDatabase.driver(config.uri, auth=(config.user, config.password))

    def close(self) -> None:
        self._driver.close()

    # ------------------------------------------------------------------
    # Cypher rules
    # ------------------------------------------------------------------

    @staticmethod
    def _rule_conclude_from_is_known_correct() -> str:
        """
        [DISABLED] Initial toy rule: create :ns0__Inference facts for TraceAct
        based on ns0__is_known_correct.

        This rule is kept here for reference but is not used in the current
        experiment, because TraceAct.is_known_correct is not a direct analogue
        of tree-level `conclude` in Loqi.

        Left here commented-out for possible later reuse/adaptation.
        """
        return """
        WITH $runUri AS runUri
        // Ensure the Run node exists
        MERGE (run:Resource:ns0__Run {uri: runUri})
          ON CREATE SET run.ns0__id = [runUri]
        WITH run
        // Domain data: TraceAct with explicit correctness
        MATCH (ta:Resource:ns0__TraceAct)
        WITH run, ta,
             CASE
               WHEN ta.ns0__is_known_correct = true  THEN 'correct'
               WHEN ta.ns0__is_known_correct = false THEN 'error'
               ELSE 'unknown'
             END AS outcome
        // For now we materialize only definite outcomes
        WHERE outcome <> 'unknown'
        // Create or reuse inference node for this (run, target, rule, outcome)
        MERGE (factToy:Resource:ns0__Inference {
          ns0__ruleId: ['simple_conclude_from_is_known_correct'],
          ns0__targetElement: elementId(ta),
          ns0__runUri: run.uri,
          ns0__outcome: [outcome]
        })
        MERGE (factToy)-[:ns0__hasRun]->(run)
        MERGE (factToy)-[:ns0__aboutTraceAct]->(ta)
        RETURN count(factToy) AS created_or_matched
        """

    @staticmethod
    def _rule_debug_state_no_interruption() -> str:
        """
        Debug rule: detect State objects with interruption_state = no_interruption
        and create simple inference facts for them.

        Assumptions (RDF -> LPG via n10s):
        - State instances are imported as (:Resource:ns0__State { uri: ... });
        - interruption_state is an object property imported as relationship
          :ns0__interruption_state to an enum node :Resource {uri: ...#no_interruption}.
        """
        return """
        WITH $runUri AS runUri
        // Ensure the Run node exists
        MERGE (run:Resource:ns0__Run {uri: runUri})
          ON CREATE SET run.ns0__id = [runUri]
        WITH run
        // Domain data: State with interruption_state = no_interruption
        MATCH (state:Resource:ns0__State)-[:ns0__interruption_state]->(mode:Resource)
        WHERE mode.uri ENDS WITH '#no_interruption'
        // Create or reuse inference node for this (run, target, rule)
        MERGE (factState:Resource:ns0__Inference {
          ns0__ruleId: ['debug_state_interruption_no_interruption'],
          ns0__targetElement: elementId(state),
          ns0__runUri: run.uri,
          ns0__outcome: ['state_no_interruption']
        })
        MERGE (factState)-[:ns0__hasRun]->(run)
        MERGE (factState)-[:ns0__aboutState]->(state)
        RETURN count(factState) AS created_or_matched
        """

    @staticmethod
    def _rule_debug_state_followup() -> str:
        """
        Second debug rule: fire only when the first rule has created a
        'state_no_interruption' inference for a given State in this run.

        This simulates a dependency between rules:
        - rule #1 marks states with no_interruption;
        - rule #2 adds an extra fact that explicitly depends on rule #1.
        """
        return """
        WITH $runUri AS runUri
        MATCH (run:Resource:ns0__Run {uri: runUri})
        // State that already has a 'state_no_interruption' inference in this run
        MATCH (state:Resource:ns0__State)
        MATCH (factBase:Resource:ns0__Inference {
          ns0__ruleId: ['debug_state_interruption_no_interruption'],
          ns0__runUri: $runUri,
          ns0__outcome: ['state_no_interruption']
        })
        WHERE factBase.ns0__targetElement = elementId(state)
        // Create or reuse a follow-up inference that depends on inf1
        MERGE (factFollow:Resource:ns0__Inference {
          ns0__ruleId: ['debug_state_followup_after_no_interruption'],
          ns0__targetElement: elementId(state),
          ns0__runUri: run.uri,
          ns0__outcome: ['state_no_interruption_followup']
        })
        MERGE (factFollow)-[:ns0__hasRun]->(run)
        MERGE (factFollow)-[:ns0__aboutState]->(state)
        MERGE (factFollow)-[:ns0__dependsOn]->(factBase)
        RETURN count(factFollow) AS created_or_matched
        """

    # ------------------------------------------------------------------
    # Rules derived from CtrlFlow tree.loqi
    # ------------------------------------------------------------------

    @staticmethod
    def _rule_ctrlflow_bind_context() -> str:
        """
        Bind CtrlFlow context (L0, A, STATE) for the current run.

        Loqi:
            tree CtrlFlow(L0: TraceAct, A: TraceAct, STATE: State) { ... }

        In RDF export, these parameters are materialized as:
            - main_state_17360 : State      with property `ns0__var...` = ["STATE"]
            - trace_act_5_atom : TraceAct   with `ns0__var...` = ["L0"]
            - trace_act_A      : TraceAct   with `ns0__var...` = ["A"]

        We:
        - locate these nodes by `ns0__var...`;
        - link them to the Run node;
        - create a debugging inference 'ctrlflow_bind_context'.
        """
        return """
        WITH $runUri AS runUri
        // Ensure Run node exists
        MERGE (run:Resource:ns0__Run {uri: runUri})
          ON CREATE SET run.ns0__id = [runUri]
        WITH run
        // Locate CtrlFlow arguments by var-name annotation
        MATCH (state:Resource:ns0__State)
        WHERE 'STATE' IN coalesce(state.`ns0__var...`, [])
        MATCH (l0:Resource:ns0__TraceAct)
        WHERE 'L0' IN coalesce(l0.`ns0__var...`, [])
        MATCH (a:Resource:ns0__TraceAct)
        WHERE 'A' IN coalesce(a.`ns0__var...`, [])
        // Link context to run
        MERGE (run)-[:ns0__hasState]->(state)
        MERGE (run)-[:ns0__hasL0]->(l0)
        MERGE (run)-[:ns0__hasA]->(a)
        // Create or reuse inference describing that context is bound
        MERGE (factCtx:Resource:ns0__Inference {
          ns0__ruleId: ['ctrlflow_bind_context'],
          ns0__targetElement: run.uri,
          ns0__runUri: run.uri,
          ns0__outcome: ['context_bound']
        })
        MERGE (factCtx)-[:ns0__hasRun]->(run)
        MERGE (factCtx)-[:ns0__aboutState]->(state)
        MERGE (factCtx)-[:ns0__aboutTraceActL0]->(l0)
        MERGE (factCtx)-[:ns0__aboutTraceActA]->(a)
        RETURN count(factCtx) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_path_l0_a_exists() -> str:
        """
        CtrlFlow: existence of at least one PathInfo P_l0_a
        from L0.hasCFGNode to A.hasCFGNode.

        Loqi fragment:
            cycle or ($P_l0_a=>from_(L0->hasCFGNode)
                      and $P_l0_a=>to_(A->hasCFGNode)) with PathInfo P_l0_a { ... }

        Here we only materialize the existence of such paths as debug facts.
        """
        return """
        WITH $runUri AS runUri
        MATCH (run:Resource:ns0__Run {uri: runUri})
        // Use bound CtrlFlow context
        MATCH (run)-[:ns0__hasL0]->(l0:Resource:ns0__TraceAct)
        MATCH (run)-[:ns0__hasA]->(a:Resource:ns0__TraceAct)
        MATCH (l0)-[:ns0__hasCFGNode]->(l0Node:Resource)
        MATCH (a)-[:ns0__hasCFGNode]->(aNode:Resource)
        // All PathInfo from L0 CFG node to A CFG node
        MATCH (path:Resource:ns0__PathInfo)-[:ns0__from_]->(l0Node)
        MATCH (path)-[:ns0__to_]->(aNode)
        // Create or reuse inference per candidate PathInfo
        MERGE (factPath:Resource:ns0__Inference {
          ns0__ruleId: ['ctrlflow_path_l0_a_exists'],
          ns0__targetElement: elementId(path),
          ns0__runUri: run.uri,
          ns0__outcome: ['path_l0_a_candidate']
        })
        MERGE (factPath)-[:ns0__hasRun]->(run)
        MERGE (factPath)-[:ns0__aboutPath]->(path)
        MERGE (factPath)-[:ns0__aboutTraceActL0]->(l0)
        MERGE (factPath)-[:ns0__aboutTraceActA]->(a)
        RETURN count(factPath) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_cycle_init() -> str:
        """
        Initialize CtrlFlow cycle over PathInfo P_l0_a:
        create CycleFrame and CycleItem nodes for all candidate paths from
        L0.hasCFGNode to A.hasCFGNode.
        """
        return """
        WITH $runUri AS runUri
        MATCH (run:Resource:ns0__Run {uri: runUri})
        MATCH (run)-[:ns0__hasL0]->(l0:Resource:ns0__TraceAct)
        MATCH (run)-[:ns0__hasA]->(a:Resource:ns0__TraceAct)
        MATCH (l0)-[:ns0__hasCFGNode]->(l0Node:Resource)
        MATCH (a)-[:ns0__hasCFGNode]->(aNode:Resource)
        MATCH (path:Resource:ns0__PathInfo)-[:ns0__from_]->(l0Node)
        MATCH (path)-[:ns0__to_]->(aNode)
        // Frame for this cycle within the run
        MERGE (frame:Resource:ns0__CycleFrame {
          ns0__cycleId: ['CtrlFlow_P_l0_a'],
          ns0__runUri: [$runUri]
        })
        ON CREATE SET frame.ns0__status = ['active']
        WITH run, frame, path, path.ns0__id AS pathIds
        WHERE pathIds IS NOT NULL AND size(pathIds) > 0
        WITH run, frame, path, pathIds[0] AS pathId
        // Ensure CycleItem per PathInfo
        MERGE (item:Resource:ns0__CycleItem {
          ns0__cycleId: ['CtrlFlow_P_l0_a'],
          ns0__runUri: [$runUri],
          ns0__pathId: pathId
        })
        ON CREATE SET item.ns0__status = ['pending']
        MERGE (frame)-[:ns0__hasItem]->(item)
        MERGE (item)-[:ns0__forPath]->(path)
        RETURN count(item) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_cycle_pick_next() -> str:
        """
        Pick next pending CycleItem for CtrlFlow cycle and mark it as current/in_progress.
        """
        return """
        WITH $runUri AS runUri
        MATCH (frame:Resource:ns0__CycleFrame {
          ns0__cycleId: ['CtrlFlow_P_l0_a'],
          ns0__runUri: [$runUri]
        })
        WHERE frame.ns0__status IS NULL OR 'active' IN frame.ns0__status
        // Do not pick new item if there is an in_progress one
        OPTIONAL MATCH (frame)-[:ns0__currentItem]->(current:Resource:ns0__CycleItem)
        WITH frame, current
        WHERE current IS NULL OR NOT 'in_progress' IN coalesce(current.ns0__status, [])
        MATCH (frame)-[:ns0__hasItem]->(item:Resource:ns0__CycleItem)
        WHERE 'pending' IN coalesce(item.ns0__status, [])
        WITH frame, item
        ORDER BY item.ns0__pathId
        LIMIT 1
        // Clear previous currentItem, if any
        OPTIONAL MATCH (frame)-[oldRel:ns0__currentItem]->(:Resource:ns0__CycleItem)
        DELETE oldRel
        WITH frame, item
        SET item.ns0__status = ['in_progress']
        MERGE (frame)-[:ns0__currentItem]->(item)
        RETURN count(item) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_cycle_eval_direct() -> str:
        """
        Simple evaluation of current CtrlFlow cycle item:
        mark outcome based on PathInfo.is_direct (placeholder for body subtree).
        """
        return """
        WITH $runUri AS runUri
        MATCH (frame:Resource:ns0__CycleFrame {
          ns0__cycleId: ['CtrlFlow_P_l0_a'],
          ns0__runUri: [$runUri]
        })-[:ns0__currentItem]->(item:Resource:ns0__CycleItem)
        WHERE 'in_progress' IN coalesce(item.ns0__status, [])
        MATCH (item)-[:ns0__forPath]->(path:Resource:ns0__PathInfo)
        WITH frame, item, path,
             coalesce(path.ns0__is_direct, [false]) AS isDirectList
        WITH frame, item,
             CASE
               WHEN size(isDirectList) > 0 AND isDirectList[0] = true
                 THEN 'true'
               ELSE 'false'
             END AS outcomeStr
        SET item.ns0__outcome = [outcomeStr],
            item.ns0__status  = ['done']
        // Remove currentItem pointer
        WITH frame, item, outcomeStr
        OPTIONAL MATCH (frame)-[curRel:ns0__currentItem]->(item)
        DELETE curRel
        // Create a debug inference for this evaluation
        WITH frame, item, outcomeStr
        MATCH (run:Resource:ns0__Run)
        WHERE [$runUri] = coalesce(frame.ns0__runUri, [])
        OPTIONAL MATCH (item)-[:ns0__forPath]->(path:Resource:ns0__PathInfo)
        MERGE (factEval:Resource:ns0__Inference {
          ns0__ruleId: ['ctrlflow_cycle_eval_direct'],
          ns0__targetElement: elementId(item),
          ns0__runUri: $runUri,
          ns0__outcome: [outcomeStr]
        })
        MERGE (factEval)-[:ns0__hasRun]->(run)
        MERGE (factEval)-[:ns0__aboutPath]->(path)
        RETURN count(item) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_cycle_aggregate_or() -> str:
        """
        Aggregate outcomes of all CycleItems for CtrlFlow cycle (OR semantics).
        """
        return """
        WITH $runUri AS runUri
        MATCH (frame:Resource:ns0__CycleFrame {
          ns0__cycleId: ['CtrlFlow_P_l0_a'],
          ns0__runUri: [$runUri]
        })
        WHERE frame.ns0__status IS NULL OR 'active' IN frame.ns0__status
        MATCH (frame)-[:ns0__hasItem]->(item:Resource:ns0__CycleItem)
        WITH frame,
             collect(item) AS items,
             collect(coalesce(item.ns0__status, [])) AS statuses
        // Aggregate only when there are no pending or in_progress items
        WITH frame, items,
             any(s IN statuses WHERE 'pending' IN s OR 'in_progress' IN s) AS hasOpen
        WHERE hasOpen = false
        UNWIND items AS it
        UNWIND coalesce(it.ns0__outcome, []) AS out
        WITH frame, collect(DISTINCT out) AS outs
        WITH frame,
             any(o IN outs WHERE o = 'true')  AS anyTrue,
             any(o IN outs WHERE o = 'false') AS anyFalse
        WITH frame,
             CASE
               WHEN anyTrue  THEN 'true'
               WHEN anyFalse THEN 'false'
               ELSE 'null'
             END AS finalOutcome
        SET frame.ns0__finalOutcome = [finalOutcome],
            frame.ns0__status       = ['done']
        RETURN count(frame) AS created_or_matched
        """

    # ------------------------------------------------------------------
    # Conclude-like rules from CtrlFlow tree (Variant A)
    # ------------------------------------------------------------------

    @staticmethod
    def _rule_ctrlflow_conclude_interruption_type_matched() -> str:
        """
        Approximate conclude: CORRECT [interruption_type_matched].

        Loqi смысл: входное ограничение на режим прерывания для пути P совместимо
        с текущим STATE.interruption_state (или равно ANY / generic_interruption).
        Здесь мы фиксируем только факт, что хотя бы один такой путь существует
        для текущего прогона CtrlFlow.
        """
        return """
        WITH $runUri AS runUri
        MATCH (run:Resource:ns0__Run {uri: runUri})
        MATCH (run)-[:ns0__hasState]->(state:Resource:ns0__State)
        MATCH (state)-[:ns0__interruption_state]->(stateMode:Resource)
        MATCH (run)-[:ns0__hasL0]->(l0:Resource:ns0__TraceAct)
        MATCH (run)-[:ns0__hasA]->(a:Resource:ns0__TraceAct)
        MATCH (l0)-[:ns0__hasCFGNode]->(l0Node:Resource)
        MATCH (a)-[:ns0__hasCFGNode]->(aNode:Resource)
        MATCH (path:Resource:ns0__PathInfo)-[:ns0__from_]->(l0Node)
        MATCH (path)-[:ns0__to_]->(aNode)
        // Constraint on interruption_mode for this path
        OPTIONAL MATCH (path)-[:ns0__hasConstraints]->(c:Resource)
        OPTIONAL MATCH (c)-[:ns0__interruption_mode]->(modeConstr:Resource)
        WITH run, state, stateMode, path, modeConstr
        WHERE modeConstr IS NOT NULL
          AND (
            modeConstr.uri ENDS WITH '#any'
            OR modeConstr.uri ENDS WITH '#generic_interruption'
            OR modeConstr.uri = stateMode.uri
          )
        WITH DISTINCT run, state, path
        // One fact per (run, state), targetElement = state
        MERGE (fact:Resource:ns0__Inference {
          ns0__ruleId: ['ctrlflow_conclude_interruption_type_matched'],
          ns0__targetElement: elementId(state),
          ns0__runUri: run.uri,
          ns0__outcome: ['interruption_type_matched']
        })
        MERGE (fact)-[:ns0__hasRun]->(run)
        MERGE (fact)-[:ns0__aboutState]->(state)
        RETURN count(fact) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_conclude_applicable_transition_with_condition_found() -> str:
        """
        Approximate conclude: ERROR [applicable_transition_with_condition_found].

        Смысл: выбранный путь P_l0_a не удовлетворяет условию (condition_value),
        но существует другой прямой путь EP из L0 с нужным значением condition_value.
        Здесь мы фиксируем только факт существования такой ситуации.
        """
        return """
        WITH $runUri AS runUri
        MATCH (run:Resource:ns0__Run {uri: runUri})
        MATCH (run)-[:ns0__hasL0]->(l0:Resource:ns0__TraceAct)
        MATCH (l0)-[:ns0__condition_value]->(l0CondVal:Resource)
        MATCH (l0)-[:ns0__hasCFGNode]->(l0Node:Resource)
        MATCH (run)-[:ns0__hasA]->(a:Resource:ns0__TraceAct)
        MATCH (a)-[:ns0__hasCFGNode]->(aNode:Resource)
        // Candidate path P_l0_a that mismatches current condition
        MATCH (pathBad:Resource:ns0__PathInfo)-[:ns0__from_]->(l0Node)
        MATCH (pathBad)-[:ns0__to_]->(aNode)
        MATCH (pathBad)-[:ns0__hasConstraints]->(cBad:Resource)
        MATCH (cBad)-[:ns0__condition_value]->(condBad:Resource)
        WHERE condBad.uri <> l0CondVal.uri
          AND NOT condBad.uri ENDS WITH '#no_value'
        // Exists another direct path with matching condition
        MATCH (pathGood:Resource:ns0__PathInfo)-[:ns0__from_]->(l0Node)
        MATCH (pathGood)-[:ns0__hasConstraints]->(cGood:Resource)
        MATCH (cGood)-[:ns0__condition_value]->(condGood:Resource)
        WHERE pathGood <> pathBad
          AND condGood.uri = l0CondVal.uri
          AND coalesce(pathGood.ns0__is_direct, [false])[0] = true
        WITH DISTINCT run, l0
        MERGE (fact:Resource:ns0__Inference {
          ns0__ruleId: ['ctrlflow_conclude_applicable_transition_with_condition_found'],
          ns0__targetElement: elementId(l0),
          ns0__runUri: run.uri,
          ns0__outcome: ['applicable_transition_with_condition_found']
        })
        MERGE (fact)-[:ns0__hasRun]->(run)
        MERGE (fact)-[:ns0__aboutTraceAct]->(l0)
        RETURN count(fact) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_conclude_action_is_function_call_recognized() -> str:
        """
        Conclude: CORRECT [action_is_function_call_recognized].

        В дереве это срабатывает, когда A НЕ является завершением вызова функции
        (или же в нашем тестовом сценарии всегда так). Здесь мы просто фиксируем,
        что A не имеет ast_node='function_call' и kind=END.
        """
        return """
        WITH $runUri AS runUri
        MATCH (run:Resource:ns0__Run {uri: runUri})
        MATCH (run)-[:ns0__hasA]->(a:Resource:ns0__TraceAct)
        MATCH (a)-[:ns0__hasASTNode]->(astA:Resource)
        MATCH (a)-[:ns0__hasCFGNode]->(cfgA:Resource)
        WHERE NOT (astA.ns0__ast_node CONTAINS 'function_call'
                   AND cfgA.ns0__kind CONTAINS 'END')
        WITH DISTINCT run, a
        MERGE (fact:Resource:ns0__Inference {
          ns0__ruleId: ['ctrlflow_conclude_action_is_function_call_recognized'],
          ns0__targetElement: elementId(a),
          ns0__runUri: run.uri,
          ns0__outcome: ['action_is_function_call_recognized']
        })
        MERGE (fact)-[:ns0__hasRun]->(run)
        MERGE (fact)-[:ns0__aboutTraceAct]->(a)
        RETURN count(fact) AS created_or_matched
        """

    @staticmethod
    def _rule_ctrlflow_conclude_unknown_incorrect() -> str:
        """
        Fallback conclude: ERROR [unknown_incorrect].

        Для варианта A реализуем грубо: если цикл по P_l0_a завершён, но при этом
        не было зафиксировано ни одного \"позитивного\" conclude для CtrlFlow,
        создаём один факт unknown_incorrect на уровне Run.
        """
        return """
        WITH $runUri AS runUri
        MATCH (run:Resource:ns0__Run {uri: runUri})
        // Есть завершённый кадр цикла CtrlFlow_P_l0_a
        MATCH (frame:Resource:ns0__CycleFrame {
          ns0__cycleId: ['CtrlFlow_P_l0_a'],
          ns0__runUri: [$runUri]
        })
        WHERE 'done' IN coalesce(frame.ns0__status, [])
        // Нет уже созданных специфических conclude-фактов CtrlFlow
        OPTIONAL MATCH (run)<-[:ns0__hasRun]-(fPos:Resource:ns0__Inference)
        WHERE any(r IN coalesce(fPos.ns0__ruleId, [])
                  WHERE r STARTS WITH 'ctrlflow_conclude_'
                    AND r <> 'ctrlflow_conclude_unknown_incorrect')
        WITH run, frame, collect(fPos) AS posFacts
        WHERE size(posFacts) = 0
        MERGE (fact:Resource:ns0__Inference {
          ns0__ruleId: ['ctrlflow_conclude_unknown_incorrect'],
          ns0__targetElement: elementId(frame),
          ns0__runUri: run.uri,
          ns0__outcome: ['unknown_incorrect']
        })
        MERGE (fact)-[:ns0__hasRun]->(run)
        MERGE (fact)-[:ns0__aboutCycleFrame]->(frame)
        RETURN count(fact) AS created_or_matched
        """

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def cleanup_all_runs(self) -> None:
        """
        Remove all reasoning runs and all nodes attached to them via :ns0__hasRun.

        This is useful for experiments, to start each script execution from a
        clean slate without touching immutable domain data.
        """
        query = """
        MATCH (run:Resource:ns0__Run)
        OPTIONAL MATCH (run)<-[:ns0__hasRun]-(res:Resource)
        WITH run, collect(DISTINCT res) AS results
        FOREACH (n IN results | DETACH DELETE n)
        DETACH DELETE run
        """
        with self._driver.session(database=DATABASE) as session:  # type: ignore[attr-defined]
            session.run(query)

    def create_run_uri(self, run_id: str) -> str:
        """
        Construct a stable URI for a reasoning run.

        Example: run_id = "202602_debug21" ->
        "http://www.vstu.ru/poas/code#run_202602_debug21"
        """
        return f"http://www.vstu.ru/poas/code#run_{run_id}"

    def apply_rules_once(self, session: Session, run_uri: str) -> Tuple[int, int]:
        """
        Apply all simple rules once and return (nodes_created, relationships_created)
        aggregated over all rule invocations.
        """
        rules: List[str] = [
            # CtrlFlow context and cycle over PathInfo P_l0_a
            self._rule_ctrlflow_bind_context(),
            self._rule_ctrlflow_cycle_init(),
            self._rule_ctrlflow_cycle_pick_next(),
            self._rule_ctrlflow_cycle_eval_direct(),
            self._rule_ctrlflow_cycle_aggregate_or(),

            # Optional debug rule over PathInfo existence
            self._rule_ctrlflow_path_l0_a_exists(),

            # Conclude-like rules from CtrlFlow tree (variant A)
            self._rule_ctrlflow_conclude_interruption_type_matched(),
            self._rule_ctrlflow_conclude_applicable_transition_with_condition_found(),
            self._rule_ctrlflow_conclude_action_is_function_call_recognized(),
            self._rule_ctrlflow_conclude_unknown_incorrect(),

            # # Debug rules for State.interruption_state
            # self._rule_debug_state_no_interruption(),
            # self._rule_debug_state_followup(),
        ]

        total_nodes_created = 0
        total_rels_created = 0

        for query in rules:
            result = session.run(query, runUri=run_uri)
            # Consume the result so that summary is populated
            list(result)
            summary = result.consume()
            counters = summary.counters
            total_nodes_created += counters.nodes_created
            total_rels_created += counters.relationships_created

        return total_nodes_created, total_rels_created

    def run_until_fixpoint(
        self,
        run_id: str,
        max_iterations: int = 100,
    ) -> Iterable[Tuple[int, int, float]]:
        """
        Run all rules in a loop until no new nodes/relationships are created
        or until max_iterations is reached.

        Yields (nodes_created, relationships_created, iteration_ms) for each iteration.
        """
        run_uri = self.create_run_uri(run_id)

        with self._driver.session(database=DATABASE) as session:  # type: ignore[attr-defined]
            for _ in range(max_iterations):
                t0 = time.monotonic_ns()
                nodes_created, rels_created = self.apply_rules_once(session, run_uri)
                t1 = time.monotonic_ns()
                iter_ms = (t1 - t0) / 1_000_000.0
                yield nodes_created, rels_created, iter_ms
                if nodes_created == 0 and rels_created == 0:
                    break

    # ------------------------------------------------------------------
    # Connection / data smoke test
    # ------------------------------------------------------------------

    def test_connection(self) -> None:
        """
        Run a few simple queries to verify that:
        - we can connect to Neo4j;
        - the expected database ('loqi') is selected;
        - key domain nodes (CFG, atom_122) are visible.
        """
        with self._driver.session(database=DATABASE) as session:  # type: ignore[attr-defined]
            # 1) Total node count
            result = session.run("MATCH (n) RETURN count(n) AS totalNodes")
            record = result.single()
            total_nodes = record["totalNodes"] if record is not None else 0
            print(f"[test] totalNodes={total_nodes}")

            # 2) Sample CFG nodes
            cfg_query = """
            MATCH (cfg:Resource:ns0__CFG)
            RETURN cfg.uri AS uri, cfg.ns0__id AS ids
            LIMIT 3
            """
            print("[test] Sample CFG nodes (up to 3):")
            for rec in session.run(cfg_query):
                print(f"  uri={rec['uri']!r}, ids={rec.get('ids')!r}")

            # 3) atom_122 (условие a > b) if present
            atom_query = """
            MATCH (n:Resource)
            WHERE n.uri CONTAINS '#atom_122'
            RETURN n.uri AS uri, n.ns0__id AS ids, n.ns0__RU_localizedName AS ruName
            LIMIT 3
            """
            print("[test] atom_122 nodes (up to 3):")
            for rec in session.run(atom_query):
                print(
                    f"  uri={rec['uri']!r}, ids={rec.get('ids')!r}, "
                    f"ruName={rec.get('ruName')!r}"
                )


def main() -> None:
    """
    Simple CLI entry point for manual experiments.

    Usage (after installing neo4j Python driver):
      - Adjust Neo4jConfig below or pass via environment variables if preferred.
      - Run this module as a script and inspect printed statistics.
    """
    config = Neo4jConfig()
    reasoner = SimpleReasoner(config)

    try:
        # Separate execution branches:
        # - python simple_reasoner.py --test-connection
        # - python simple_reasoner.py  (default reasoning run)
        if len(sys.argv) > 1 and sys.argv[1] == "--test-connection":
            reasoner.test_connection()
        else:
            # Start gross timing (end-to-end for reasoning run)
            gross_t0 = time.monotonic_ns()

            # Start every script execution from a clean reasoning state:
            # remove all :ns0__Run nodes and everything attached to them via :ns0__hasRun.
            reasoner.cleanup_all_runs()

            run_id = "test_simple_conclude"
            for iteration, (nodes, rels, iter_ms) in enumerate(
                reasoner.run_until_fixpoint(run_id), start=1
            ):
                print(
                    f"Iteration {iteration}: "
                    f"nodes_created={nodes}, relationships_created={rels}, "
                    f"iteration_ms={iter_ms:.3f}"
                )
                if nodes == 0 and rels == 0:
                    print("Fixpoint reached.")
                    break

            gross_t1 = time.monotonic_ns()
            gross_ms = (gross_t1 - gross_t0) / 1_000_000.0
            print(f"Total reasoning time (gross_ms) = {gross_ms:.3f}")
    finally:
        reasoner.close()


if __name__ == "__main__":
    main()

