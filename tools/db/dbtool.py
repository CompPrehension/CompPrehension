#!/usr/bin/env python3
import argparse
import ctypes
import os
import re
import shutil
import subprocess
import sys
import time
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOL_DIR = Path(__file__).resolve().parent
BUILD = TOOL_DIR / "build"
SERVER = ROOT / "modules" / "server"
RESOURCES = SERVER / "src" / "main" / "resources"
CHANGELOG_DIR = RESOURCES / "db" / "changelog"
MASTER = CHANGELOG_DIR / "db.changelog-master.xml"
MASTER_REL = "db/changelog/db.changelog-master.xml"
BASELINE_DIR = CHANGELOG_DIR / "baseline"
ENV_FILE = ROOT / ".env" / "compprehension-env.env"

LIQUIBASE_MAIN = "liquibase.integration.commandline.LiquibaseCommandLine"
HIBERNATE_REF_URL = (
    "hibernate:spring:org.vstu.compprehension.entities"
    "?dialect=dbtools.DiffMySQLDialect"
    "&hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
    "&hibernate.implicit_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
)
MASTER_HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.5.xsd">
"""
BASELINE_TEMPLATE = """<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                       http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.5.xsd">

    <!--
        Baseline: schema and seed data squashed from all migrations up to {date} (inclusive).
        On databases that already exist the precondition fails and the changeset is marked as ran.
    -->
    <changeSet id="{date}-baseline" author="{author}" runInTransaction="false">
        <preConditions onFail="MARK_RAN">
            <not><tableExists tableName="{marker}"/></not>
        </preConditions>
        <sqlFile path="db/changelog/baseline/schema.sql" stripComments="false"/>
        <sqlFile path="db/changelog/baseline/data.sql" stripComments="false"/>
        <rollback/>
    </changeSet>

</databaseChangeLog>
"""


SECRETS = []


def log(msg):
    for secret in SECRETS:
        msg = msg.replace(secret, "***")
    print(f"[dbtool] {msg}", flush=True)


def die(msg):
    print(f"[dbtool] ERROR: {msg}", file=sys.stderr, flush=True)
    sys.exit(1)


def run(cmd, **kw):
    log("$ " + " ".join(str(c) for c in cmd))
    return subprocess.run([str(c) for c in cmd], check=True, **kw)


def mvn():
    return "mvn.cmd" if os.name == "nt" else "mvn"


# ---------------------------------------------------------------- db config

class Db:
    def __init__(self, host, port, name, user, password):
        self.host, self.port, self.name, self.user, self.password = host, port, name, user, password

    def url(self, name=None):
        return f"jdbc:mysql://{self.host}:{self.port}/{name if name is not None else self.name}?useSSL=false&allowPublicKeyRetrieval=true"

    def with_name(self, name):
        return Db(self.host, self.port, name, self.user, self.password)


def read_env():
    env = {}
    if ENV_FILE.exists():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip()
    return env


def db_from_args(args):
    env = read_env()
    host = args.host or env.get("MYSQL_HOST", "localhost")
    name = args.db or env.get("MYSQL_DATABASE")
    user = args.user or env.get("MYSQL_USER", "root")
    password = args.password if args.password is not None else env.get("MYSQL_PASSWORD")
    if not name or password is None:
        die(f"database name/password not given and not found in {ENV_FILE}")
    if password:
        SECRETS.append(password)
    return Db(host, args.port, name, user, password)


# ---------------------------------------------------------------- classpath

def short_path(p):
    if os.name != "nt":
        return p
    buf = ctypes.create_unicode_buffer(1024)
    if ctypes.windll.kernel32.GetShortPathNameW(str(p), buf, 1024):
        return buf.value
    return p


def write_argfile(path, classpath):
    if not classpath.isascii():
        classpath = os.pathsep.join(short_path(e) for e in classpath.split(os.pathsep))
    classpath = classpath.replace("\\", "/")
    with open(path, "w", encoding="ascii", newline="\n") as f:
        f.write(f'-cp "{classpath}"\n')


def local_repo():
    out = run([mvn(), "-q", "help:evaluate", "-Dexpression=settings.localRepository", "-DforceStdout"],
              cwd=ROOT, capture_output=True, text=True).stdout.strip()
    return Path(out.splitlines()[-1])


def plugin_version(artifact):
    pom = (SERVER / "pom.xml").read_text(encoding="utf-8")
    m = re.search(rf"<artifactId>{re.escape(artifact)}</artifactId>\s*<version>([^<]+)</version>", pom)
    if not m:
        die(f"cannot find version of {artifact} in {SERVER / 'pom.xml'}")
    return m.group(1)


def resolve(repo, group, artifact, version):
    jar = repo / Path(*group.split(".")) / artifact / version / f"{artifact}-{version}.jar"
    if not jar.exists():
        run([mvn(), "-q", "dependency:get", f"-Dartifact={group}:{artifact}:{version}"], cwd=ROOT)
    if not jar.exists():
        die(f"cannot resolve {jar}")
    return jar


def prepare(skip_build):
    BUILD.mkdir(exist_ok=True)
    if not skip_build:
        run([mvn(), "-q", "compile", "-pl", "modules/server", "-am"], cwd=ROOT)
    cp_file = BUILD / "mvn-classpath.txt"
    run([mvn(), "-q", "dependency:build-classpath", f"-Dmdep.outputFile={cp_file}", "-Dmdep.includeScope=runtime"],
        cwd=SERVER)
    entries = []
    for e in cp_file.read_text(encoding="utf-8").strip().split(os.pathsep):
        m = re.search(r"org[\\/]vstu[\\/]compprehension[\\/]([^\\/]+)[\\/]", e)
        if m and (ROOT / "modules" / m.group(1) / "target" / "classes").is_dir():
            entries.append(str(ROOT / "modules" / m.group(1) / "target" / "classes"))
        else:
            entries.append(e)
    repo = local_repo()
    lb_version = plugin_version("liquibase-core")
    extras = [
        resolve(repo, "org.liquibase.ext", "liquibase-hibernate6", lb_version),
        resolve(repo, "info.picocli", "picocli", "4.7.5"),
    ]
    classes = BUILD / "classes"
    shutil.rmtree(classes, ignore_errors=True)
    classes.mkdir()
    deps_argfile = BUILD / "deps.argfile"
    write_argfile(deps_argfile, os.pathsep.join(entries + [str(e) for e in extras]))
    sources = [str(p) for p in (TOOL_DIR / "src").rglob("*.java")]
    run(["javac", "-proc:none", "-nowarn", f"@{deps_argfile}", "-d", str(classes)] + sources)
    write_argfile(BUILD / "classpath.argfile", os.pathsep.join([str(classes)] + entries + [str(e) for e in extras]))
    log(f"classpath ready: {BUILD / 'classpath.argfile'}")


def ensure_prepared(args):
    if args.rebuild or not (BUILD / "classpath.argfile").exists():
        prepare(skip_build=False)


def java(main, *args, **kw):
    return run(["java", f"@{BUILD / 'classpath.argfile'}", main, *args], **kw)


def liquibase(db, *args, search_path=None, changelog=MASTER_REL, capture=False):
    paths = [str(RESOURCES)] + ([str(search_path)] if search_path else [])
    return java(LIQUIBASE_MAIN,
                f"--url={db.url()}", f"--username={db.user}", f"--password={db.password}",
                f"--search-path={','.join(paths)}", f"--changelog-file={changelog}",
                "--log-level=warning", *args,
                capture_output=capture, text=capture)


def sql(db, statement, name=None):
    java("dbtools.SchemaDump", "exec", db.url(name), db.user, db.password, statement)


def dump(db, kind, out):
    java("dbtools.SchemaDump", kind, db.url(), db.user, db.password, str(out))


def checksums(db):
    out = java("dbtools.SchemaDump", "checksum", db.url(), db.user, db.password,
               capture_output=True, text=True).stdout
    return dict(line.split("\t") for line in out.strip().splitlines() if line)


# ---------------------------------------------------------------- diff

NOISE_TABLES = ("DATABASECHANGELOG", "jobs_jobrunr")
TYPE_ALIASES = [("bigint(19)", "bigint"), ("int(10)", "int"), ("integer", "int"), ("bit(1)", "bit"),
                ("text(65535)", "text"), ("float(12)", "float"), ("float(23)", "float"),
                ("double(22)", "double"), ("float(53)", "double"), ("tinyint(3)", "tinyint")]


def norm_type(t):
    t = re.sub(r"\s*byte\)", ")", t.lower().strip())
    for a, b in TYPE_ALIASES:
        t = t.replace(a, b)
    return t


def summarize_diff(text):
    lines = text.splitlines()
    section, current, out = None, None, []
    for line in lines:
        m = re.match(r"^(Missing|Unexpected|Changed) (\w[\w ]*)\(s\): ?(NONE)?$", line)
        if m:
            section = None if m.group(3) else f"{m.group(1)} {m.group(2)}"
            continue
        if not section or section.endswith("Catalog") or not line.startswith("     ")                 or any(n in line for n in NOISE_TABLES):
            continue
        if section == "Changed Column":
            if not line.startswith("          "):
                current = line.strip()
                continue
            m = re.match(r"\s+(\w+) changed from '(.*)' to '(.*)'", line)
            if not m or m.group(1) in ("order", "certainDataType"):
                continue
            if m.group(1) == "type" and norm_type(m.group(2)) == norm_type(m.group(3)):
                continue
            if m.group(1) == "defaultValue" and m.group(2).strip("'") == m.group(3).strip("'"):
                continue
            out.append(f"{section} | {current}: {m.group(1)} {m.group(2)!r} -> {m.group(3)!r}")
        elif section == "Changed Primary Key":
            if not line.startswith("          "):
                current = line.strip()
                continue
            m = re.match(r"\s+columns changed from '\[(.*)]' to '\[(.*)]'", line)
            if m:
                cols = [set(c.rsplit(".", 1)[-1] for c in g.split(", ")) for g in m.groups()]
                if cols[0] == cols[1]:
                    continue
            out.append(f"{section} | {current}: {line.strip()}")
        elif section == "Changed Foreign Key" or line.startswith("          "):
            continue
        elif section == "Unexpected Index" and re.match(r"(PRIMARY |fk_)", line.strip(), re.I):
            continue
        elif section == "Missing Index" and re.match(r"IX_\w+PK ", line.strip()):
            continue
        else:
            out.append(f"{section} | {line.strip()}")
    return "\n".join(out) if out else "(no substantive differences)"


def cmd_liquibase(args):
    ensure_prepared(args)
    liquibase(db_from_args(args), *args.args)


def cmd_diff_entities(args):
    db = db_from_args(args)
    ensure_prepared(args)
    res = java(LIQUIBASE_MAIN,
               f"--url={db.url()}", f"--username={db.user}", f"--password={db.password}",
               f"--reference-url={HIBERNATE_REF_URL}", "--log-level=warning", "diff",
               capture_output=True, text=True)
    (BUILD / "diff-entities.txt").write_text(res.stdout, encoding="utf-8")
    print(res.stdout if args.full else summarize_diff(res.stdout))
    log(f"full report: {BUILD / 'diff-entities.txt'}")


# ---------------------------------------------------------------- squash

INCLUDE_RE = re.compile(r'^\s*<include\s+file="([^"]+)"\s*/>\s*$')


def master_includes():
    includes = []
    for line in MASTER.read_text(encoding="utf-8").splitlines():
        m = INCLUDE_RE.match(line)
        if m:
            includes.append(m.group(1))
    return includes


def write_temp_master(name, includes):
    root = BUILD / name
    shutil.rmtree(root, ignore_errors=True)
    target = root / "db" / "changelog" / f"{name}.xml"
    target.parent.mkdir(parents=True)
    body = "".join(f'    <include file="{i}"/>\n' for i in includes)
    target.write_text(MASTER_HEADER + body + "</databaseChangeLog>\n", encoding="utf-8")
    return root, f"db/changelog/{name}.xml"


def build_schema(db, name, includes, temp_name):
    sql(db, f"drop database if exists `{name}`", "")
    sql(db, f"create database `{name}` character set utf8mb4 collate utf8mb4_0900_ai_ci", "")
    root, changelog = write_temp_master(temp_name, includes)
    liquibase(db.with_name(name), "update", search_path=root, changelog=changelog)
    return db.with_name(name)


def cmd_squash(args):
    db = db_from_args(args)
    ensure_prepared(args)
    today = args.date or date.today().isoformat()
    includes = master_includes()
    squashed = [i for i in includes if not any(k in i for k in args.keep)]
    kept = [i for i in includes if i not in squashed]
    missing = [i for i in squashed if not (RESOURCES / i).exists()]
    if missing:
        die("included changelogs not found under " + str(RESOURCES) + ":\n  " + "\n  ".join(missing))
    if not squashed:
        die("nothing to squash")
    log(f"squashing {len(squashed)} changelog(s), keeping {len(kept)}: {kept}")

    suffix = time.strftime("%H%M%S")
    a = build_schema(db, f"{db.name}_squash_a_{suffix}", squashed, "squash-source")
    backup = BUILD / "baseline-backup"
    shutil.rmtree(backup, ignore_errors=True)
    if BASELINE_DIR.exists():
        shutil.copytree(BASELINE_DIR, backup)
    BASELINE_DIR.mkdir(exist_ok=True)
    dump(a, "ddl", BASELINE_DIR / "schema.sql")
    dump(a, "data", BASELINE_DIR / "data.sql")
    baseline_rel = f"db/changelog/baseline/{today}-baseline-changelog.xml"
    for old in BASELINE_DIR.glob("*-baseline-changelog.xml"):
        old.unlink()
    (RESOURCES / baseline_rel).write_text(
        BASELINE_TEMPLATE.format(date=today, author=args.author, marker=args.marker_table), encoding="utf-8")

    b = build_schema(db, f"{db.name}_squash_b_{suffix}", [baseline_rel], "squash-check")
    ddl_a, ddl_b = BUILD / "ddl-a.sql", BUILD / "ddl-b.sql"
    dump(a, "ddl", ddl_a)
    dump(b, "ddl", ddl_b)
    ok = ddl_a.read_bytes() == ddl_b.read_bytes()
    if not ok:
        log(f"DDL mismatch, compare {ddl_a} and {ddl_b}")
    sums_a, sums_b = checksums(a), checksums(b)
    for t in sorted(set(sums_a) | set(sums_b)):
        if sums_a.get(t) != sums_b.get(t):
            ok = False
            log(f"data mismatch in {t}: {sums_a.get(t)} vs {sums_b.get(t)}")
    if not args.keep_schemas:
        for s in (a, b):
            sql(db, f"drop database `{s.name}`", "")
    if not ok:
        generated = BUILD / "baseline-failed"
        shutil.rmtree(generated, ignore_errors=True)
        shutil.copytree(BASELINE_DIR, generated)
        shutil.rmtree(BASELINE_DIR)
        if backup.exists():
            shutil.copytree(backup, BASELINE_DIR)
        die(f"baseline verification failed; generated files moved to {generated}, repository left untouched")

    body = f'    <include file="{baseline_rel}"/>\n' + "".join(f'    <include file="{i}"/>\n' for i in kept)
    MASTER.write_text(MASTER_HEADER + body + "</databaseChangeLog>\n", encoding="utf-8")
    removed_dirs = set()
    for i in squashed:
        p = RESOURCES / i
        if i != baseline_rel and p.exists():
            p.unlink()
        removed_dirs.add(p.parent)
    for d in removed_dirs:
        if d != BASELINE_DIR and not any(d.iterdir()):
            d.rmdir()
    log(f"done: baseline {baseline_rel}, removed {len(squashed)} changelog(s)")
    log("next: review `git status`, then run `dbtool.py liquibase update` against your dev database")


# ---------------------------------------------------------------- cli

def add_db_args(p):
    p.add_argument("--host")
    p.add_argument("--port", type=int, default=3306)
    p.add_argument("--db")
    p.add_argument("--user")
    p.add_argument("--password")
    p.add_argument("--rebuild", action="store_true", help="recompile modules and rebuild the classpath first")


def main():
    parser = argparse.ArgumentParser(description="Liquibase / schema tooling for CompPrehension")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p = sub.add_parser("prepare", help="compile modules, build flat classpath and helper classes")
    p.add_argument("--skip-build", action="store_true", help="do not run mvn compile")
    p.set_defaults(fn=lambda a: prepare(a.skip_build))

    p = sub.add_parser("liquibase", help="run Liquibase CLI against the configured database")
    add_db_args(p)
    p.add_argument("args", nargs=argparse.REMAINDER)
    p.set_defaults(fn=cmd_liquibase)

    p = sub.add_parser("diff-entities", help="diff Hibernate entity model against the database")
    add_db_args(p)
    p.add_argument("--full", action="store_true", help="print raw Liquibase report instead of the summary")
    p.set_defaults(fn=cmd_diff_entities)

    p = sub.add_parser("squash", help="squash migrations into a baseline")
    add_db_args(p)
    p.add_argument("--keep", action="append", default=[],
                   help="substring of include paths to keep after the baseline (repeatable), e.g. 2026-09-11")
    p.add_argument("--date", help="baseline date, default today")
    p.add_argument("--author", default="Artem Prokudin")
    p.add_argument("--marker-table", default="exercise",
                   help="table whose absence means the database is empty")
    p.add_argument("--keep-schemas", action="store_true", help="do not drop temporary schemas")
    p.set_defaults(fn=cmd_squash)

    args = parser.parse_args()
    try:
        args.fn(args)
    except subprocess.CalledProcessError as e:
        die(f"command failed with exit code {e.returncode}: {e.cmd[0]} ... {e.cmd[-1]}")


if __name__ == "__main__":
    main()
