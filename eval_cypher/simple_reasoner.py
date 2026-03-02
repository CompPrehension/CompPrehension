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
        Create :ns0__Inference facts for TraceAct based on ns0__is_known_correct.

        Assumptions:
        - TraceAct nodes are imported as (:Resource:ns0__TraceAct { ns0__is_known_correct: <bool> }).
        - We do NOT modify these nodes; we only create inference nodes.
        - All new inference nodes are linked to a single :ns0__Run node by :ns0__hasRun.

        This rule is idempotent due to MERGE on (ruleId, targetElement, runUri, outcome).
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
        MERGE (inf:Resource:ns0__Inference {
          ns0__ruleId: ['simple_conclude_from_is_known_correct'],
          ns0__targetElement: elementId(ta),
          ns0__runUri: run.uri,
          ns0__outcome: [outcome]
        })
        MERGE (inf)-[:ns0__hasRun]->(run)
        MERGE (inf)-[:ns0__aboutTraceAct]->(ta)
        RETURN count(inf) AS created_or_matched
        """

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

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
            self._rule_conclude_from_is_known_correct(),
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
    ) -> Iterable[Tuple[int, int]]:
        """
        Run all rules in a loop until no new nodes/relationships are created
        or until max_iterations is reached.

        Yields (nodes_created, relationships_created) for each iteration.
        """
        run_uri = self.create_run_uri(run_id)

        with self._driver.session(database=DATABASE) as session:  # type: ignore[attr-defined]
            for _ in range(max_iterations):
                nodes_created, rels_created = self.apply_rules_once(session, run_uri)
                yield nodes_created, rels_created
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
        if False or len(sys.argv) > 1 and sys.argv[1] == "--test-connection":
            reasoner.test_connection()
        else:
            run_id = "test_simple_conclude"
            for iteration, (nodes, rels) in enumerate(
                reasoner.run_until_fixpoint(run_id), start=1
            ):
                print(
                    f"Iteration {iteration}: "
                    f"nodes_created={nodes}, relationships_created={rels}"
                )
                if nodes == 0 and rels == 0:
                    print("Fixpoint reached.")
                    break
    finally:
        reasoner.close()


if __name__ == "__main__":
    main()

