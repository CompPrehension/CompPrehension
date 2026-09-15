package org.vstu.compprehension.tools.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.vstu.compprehension.tools.db.DbTool.log;

final class Squash {

    private static final Pattern INCLUDE = Pattern.compile("^\\s*<include\\s+file=\"([^\"]+)\"\\s*/>\\s*$");
    private static final String MASTER_HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <databaseChangeLog
                    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                                  http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.5.xsd">
            """;
    private static final String BASELINE_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                               xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.5.xsd">

                <!--
                    Baseline: schema and seed data squashed from all migrations up to %1$s (inclusive).
                    On databases that already exist the precondition fails and the changeset is marked as ran.
                -->
                <changeSet id="%1$s-baseline" author="%2$s" runInTransaction="false">
                    <preConditions onFail="MARK_RAN">
                        <not><tableExists tableName="%3$s"/></not>
                    </preConditions>
                    <sqlFile path="db/changelog/baseline/schema.sql" stripComments="false"/>
                    <sqlFile path="db/changelog/baseline/data.sql" stripComments="false"/>
                    <rollback/>
                </changeSet>

            </databaseChangeLog>
            """;

    private final DbTool tool;
    private final DbTool.Options options;
    private final DbTool.DbConfig db;
    private final Path resources;
    private final Path master;
    private final Path baselineDir;
    private final Path workDir;

    Squash(DbTool tool, DbTool.Options options) throws IOException {
        this.tool = tool;
        this.options = options;
        this.db = options.db;
        this.resources = tool.resources();
        this.master = resources.resolve(DbTool.MASTER_CHANGELOG);
        this.baselineDir = resources.resolve("db/changelog/baseline");
        this.workDir = tool.workDir();
    }

    int run() throws Exception {
        var today = options.date != null ? options.date : LocalDate.now().toString();
        var includes = masterIncludes();
        var squashed = includes.stream().filter(i -> options.keep.stream().noneMatch(i::contains)).toList();
        var kept = includes.stream().filter(i -> !squashed.contains(i)).toList();
        var missing = squashed.stream().filter(i -> !Files.exists(resources.resolve(i))).toList();
        if (!missing.isEmpty()) {
            return fail("included changelogs not found under " + resources + ":\n  " + String.join("\n  ", missing));
        }
        if (squashed.isEmpty()) {
            return fail("nothing to squash");
        }
        log("squashing " + squashed.size() + " changelog(s), keeping " + kept.size() + ": " + kept);

        var suffix = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        var a = buildSchema(db.name() + "_squash_a_" + suffix, squashed, "squash-source");
        var backup = workDir.resolve("baseline-backup");
        deleteTree(backup);
        if (Files.exists(baselineDir)) {
            copyTree(baselineDir, backup);
        }
        Files.createDirectories(baselineDir);
        try (var connection = tool.connect(a)) {
            var dump = new SchemaDump(connection);
            dump.dumpDdl(baselineDir.resolve("schema.sql"));
            dump.dumpData(baselineDir.resolve("data.sql"));
        }
        var baselineRel = "db/changelog/baseline/" + today + "-baseline-changelog.xml";
        try (var old = Files.list(baselineDir)) {
            for (var file : old.filter(f -> f.getFileName().toString().endsWith("-baseline-changelog.xml")).toList()) {
                Files.delete(file);
            }
        }
        Files.writeString(resources.resolve(baselineRel),
                BASELINE_TEMPLATE.formatted(today, options.author, options.markerTable), StandardCharsets.UTF_8);

        var b = buildSchema(db.name() + "_squash_b_" + suffix, List.of(baselineRel), "squash-check");
        var ddlA = workDir.resolve("ddl-a.sql");
        var ddlB = workDir.resolve("ddl-b.sql");
        Map<String, String> sumsA;
        Map<String, String> sumsB;
        try (var connectionA = tool.connect(a); var connectionB = tool.connect(b)) {
            var dumpA = new SchemaDump(connectionA);
            var dumpB = new SchemaDump(connectionB);
            dumpA.dumpDdl(ddlA);
            dumpB.dumpDdl(ddlB);
            sumsA = dumpA.checksums();
            sumsB = dumpB.checksums();
        }
        var ok = Files.mismatch(ddlA, ddlB) == -1;
        if (!ok) {
            log("DDL mismatch, compare " + ddlA + " and " + ddlB);
        }
        var tables = new TreeSet<>(sumsA.keySet());
        tables.addAll(sumsB.keySet());
        for (var table : tables) {
            if (!Objects.equals(sumsA.get(table), sumsB.get(table))) {
                ok = false;
                log("data mismatch in " + table + ": " + sumsA.get(table) + " vs " + sumsB.get(table));
            }
        }
        if (!options.keepSchemas) {
            for (var schema : List.of(a, b)) {
                execute(db.withName(""), "drop database `" + schema.name() + "`");
            }
        }
        if (!ok) {
            var generated = workDir.resolve("baseline-failed");
            deleteTree(generated);
            copyTree(baselineDir, generated);
            deleteTree(baselineDir);
            if (Files.exists(backup)) {
                copyTree(backup, baselineDir);
            }
            return fail("baseline verification failed; generated files moved to " + generated + ", repository left untouched");
        }

        var body = new StringBuilder(include(baselineRel));
        kept.forEach(i -> body.append(include(i)));
        Files.writeString(master, MASTER_HEADER + body + "</databaseChangeLog>\n", StandardCharsets.UTF_8);
        var removedDirs = new HashSet<Path>();
        for (var i : squashed) {
            var path = resources.resolve(i);
            if (!i.equals(baselineRel) && Files.exists(path)) {
                Files.delete(path);
            }
            removedDirs.add(path.getParent());
        }
        for (var dir : removedDirs) {
            if (!dir.equals(baselineDir) && isEmpty(dir)) {
                Files.delete(dir);
            }
        }
        log("done: baseline " + baselineRel + ", removed " + squashed.size() + " changelog(s)");
        log("next: review `git status`, then run `dbtool liquibase update` against your dev database");
        return 0;
    }

    private DbTool.DbConfig buildSchema(String name, List<String> includes, String tempName) throws Exception {
        execute(db.withName(""), "drop database if exists `" + name + "`");
        execute(db.withName(""), "create database `" + name + "` character set utf8mb4 collate utf8mb4_0900_ai_ci");
        var root = workDir.resolve(tempName);
        deleteTree(root);
        var changelog = "db/changelog/" + tempName + ".xml";
        var target = root.resolve(changelog);
        Files.createDirectories(target.getParent());
        var body = new StringBuilder();
        includes.forEach(i -> body.append(include(i)));
        Files.writeString(target, MASTER_HEADER + body + "</databaseChangeLog>\n", StandardCharsets.UTF_8);
        var schema = db.withName(name);
        int code = tool.liquibase(schema, List.of("update"), root, changelog);
        if (code != 0) {
            throw new IllegalStateException("liquibase update failed for " + name + " with exit code " + code);
        }
        return schema;
    }

    private void execute(DbTool.DbConfig target, String sql) throws SQLException {
        try (var connection = tool.connect(target)) {
            new SchemaDump(connection).execute(sql);
        }
    }

    private List<String> masterIncludes() throws IOException {
        var includes = new ArrayList<String>();
        for (var line : Files.readAllLines(master, StandardCharsets.UTF_8)) {
            var matcher = INCLUDE.matcher(line);
            if (matcher.matches()) {
                includes.add(matcher.group(1));
            }
        }
        return includes;
    }

    private static String include(String file) {
        return "    <include file=\"" + file + "\"/>\n";
    }

    private static int fail(String message) {
        System.err.println("[dbtool] ERROR: " + message);
        return 1;
    }

    private static boolean isEmpty(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (var path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> walk = Files.walk(from)) {
            for (var path : walk.toList()) {
                var target = to.resolve(from.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target);
                }
            }
        }
    }
}
