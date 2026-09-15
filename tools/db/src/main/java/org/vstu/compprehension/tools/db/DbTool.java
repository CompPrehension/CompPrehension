package org.vstu.compprehension.tools.db;

import liquibase.integration.commandline.LiquibaseCommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class DbTool {

    static final String MASTER_CHANGELOG = "db/changelog/db.changelog-master.xml";
    private static final String LIQUIBASE_MAIN_ARGS = "--log-level=warning";
    private static final String HIBERNATE_REFERENCE_URL = "hibernate:spring:org.vstu.compprehension.entities"
            + "?dialect=" + DiffMySQLDialect.class.getName()
            + "&hibernate.physical_naming_strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
            + "&hibernate.implicit_naming_strategy=org.springframework.boot.hibernate.SpringImplicitNamingStrategy";

    private final Options options;
    private final Path root;
    private final Path resources;
    private final Path workDir;

    private DbTool(Options options) {
        this.options = options;
        this.root = options.root != null ? options.root : findRoot();
        this.resources = root.resolve("modules/server/src/main/resources");
        this.workDir = root.resolve("tools/db/target/work");
        if (!Files.exists(resources.resolve(MASTER_CHANGELOG))) {
            throw new IllegalArgumentException("Не найден " + resources.resolve(MASTER_CHANGELOG) + "; задайте --root");
        }
        options.resolveDb(root);
    }

    public static void main(String[] args) throws Exception {
        Options options;
        DbTool tool;
        try {
            options = Options.parse(args);
            tool = new DbTool(options);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(Options.USAGE);
            System.exit(1);
            return;
        }
        int code = switch (options.command) {
            case "liquibase" -> tool.liquibase(options.db, options.commandArgs, null, MASTER_CHANGELOG);
            case "diff-entities" -> tool.diffEntities();
            case "squash" -> new Squash(tool, options).run();
            default -> throw new IllegalStateException(options.command);
        };
        System.exit(code);
    }

    Path root() {
        return root;
    }

    Path resources() {
        return resources;
    }

    Path workDir() throws IOException {
        Files.createDirectories(workDir);
        return workDir;
    }

    int liquibase(DbConfig db, List<String> args, Path extraSearchPath, String changelog) {
        var searchPath = resources.toString() + (extraSearchPath != null ? "," + extraSearchPath : "");
        var all = new ArrayList<String>();
        all.add("--url=" + db.url());
        all.add("--username=" + db.user());
        all.add("--password=" + db.password());
        all.add("--search-path=" + searchPath);
        all.add("--changelog-file=" + changelog);
        all.add(LIQUIBASE_MAIN_ARGS);
        all.addAll(args);
        return new LiquibaseCommandLine().execute(all.toArray(String[]::new));
    }

    private int diffEntities() throws IOException {
        var report = workDir().resolve("diff-entities.txt");
        int code = new LiquibaseCommandLine().execute(new String[]{
                "--url=" + options.db.url(), "--username=" + options.db.user(), "--password=" + options.db.password(),
                "--reference-url=" + HIBERNATE_REFERENCE_URL, "--output-file=" + report, LIQUIBASE_MAIN_ARGS, "diff"});
        if (code != 0) {
            return code;
        }
        var text = Files.readString(report, StandardCharsets.UTF_8);
        System.out.println(options.full ? text : DiffSummary.summarize(text));
        log("full report: " + report);
        return 0;
    }

    Connection connect(DbConfig db) throws SQLException {
        return DriverManager.getConnection(db.url(), db.user(), db.password());
    }

    static void log(String message) {
        System.out.println("[dbtool] " + message);
    }

    private static Path findRoot() {
        var dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.exists(dir.resolve("modules/server/src/main/resources").resolve(MASTER_CHANGELOG))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalArgumentException("Корень репозитория не найден относительно текущего каталога; задайте --root");
    }

    record DbConfig(String host, int port, String name, String user, String password) {
        String url() {
            return "jdbc:mysql://" + host + ":" + port + "/" + name + "?useSSL=false&allowPublicKeyRetrieval=true";
        }

        DbConfig withName(String otherName) {
            return new DbConfig(host, port, otherName, user, password);
        }
    }

    static final class Options {
        static final String USAGE = """
                Usage: java -jar dbtool.jar <command> [options]
                Commands:
                  liquibase <args...>   run Liquibase CLI against the configured database
                  diff-entities         diff Hibernate entity model against the database
                  squash                squash migrations into a baseline
                Connection: --host H --port P --db NAME --user U --password PW, or --env FILE
                            (default .env/compprehension-env.env under the repository root: MYSQL_HOST, MYSQL_PORT,
                            MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD).
                Common:     --root DIR (repository root, found automatically by default)
                diff-entities: --full
                squash:     --keep SUBSTRING (repeatable) --date YYYY-MM-DD --author NAME --marker-table TABLE --keep-schemas""";

        String command;
        List<String> commandArgs = new ArrayList<>();
        Path root;
        String host;
        Integer port;
        String name;
        String user;
        String password;
        String envFile;
        boolean full;
        List<String> keep = new ArrayList<>();
        String date;
        String author = "Artem Prokudin";
        String markerTable = "exercise";
        boolean keepSchemas;
        DbConfig db;

        static Options parse(String[] args) {
            if (args.length == 0) {
                throw new IllegalArgumentException("Не указана команда");
            }
            var options = new Options();
            options.command = args[0];
            if (!List.of("liquibase", "diff-entities", "squash").contains(options.command)) {
                throw new IllegalArgumentException("Неизвестная команда: " + options.command);
            }
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--root" -> options.root = Path.of(args[++i]).toAbsolutePath();
                    case "--host" -> options.host = args[++i];
                    case "--port" -> options.port = Integer.parseInt(args[++i]);
                    case "--db" -> options.name = args[++i];
                    case "--user" -> options.user = args[++i];
                    case "--password" -> options.password = args[++i];
                    case "--env" -> options.envFile = args[++i];
                    case "--full" -> options.full = true;
                    case "--keep" -> options.keep.add(args[++i]);
                    case "--date" -> options.date = args[++i];
                    case "--author" -> options.author = args[++i];
                    case "--marker-table" -> options.markerTable = args[++i];
                    case "--keep-schemas" -> options.keepSchemas = true;
                    default -> {
                        if (options.command.equals("liquibase")) {
                            options.commandArgs.add(args[i]);
                        } else {
                            throw new IllegalArgumentException("Неизвестный аргумент: " + args[i]);
                        }
                    }
                }
            }
            return options;
        }

        void resolveDb(Path root) {
            var path = envFile != null ? Path.of(envFile) : root.resolve(".env/compprehension-env.env");
            var env = new HashMap<String, String>();
            if (Files.exists(path)) {
                try {
                    for (var line : Files.readAllLines(path)) {
                        var trimmed = line.trim();
                        int eq = trimmed.indexOf('=');
                        if (!trimmed.startsWith("#") && eq > 0) {
                            env.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException("Не удалось прочитать " + path + ": " + e.getMessage());
                }
            } else if (envFile != null) {
                throw new IllegalArgumentException("Файл " + path.toAbsolutePath() + " не найден");
            }
            var dbName = name != null ? name : env.get("MYSQL_DATABASE");
            var dbPassword = password != null ? password : env.get("MYSQL_PASSWORD");
            if (dbName == null || dbPassword == null) {
                throw new IllegalArgumentException("Не заданы --db/--password, и в " + path + " нет MYSQL_DATABASE/MYSQL_PASSWORD");
            }
            db = new DbConfig(
                    host != null ? host : env.getOrDefault("MYSQL_HOST", "localhost"),
                    port != null ? port : Integer.parseInt(env.getOrDefault("MYSQL_PORT", "3306")),
                    dbName,
                    user != null ? user : env.getOrDefault("MYSQL_USER", "root"),
                    dbPassword);
        }
    }
}
