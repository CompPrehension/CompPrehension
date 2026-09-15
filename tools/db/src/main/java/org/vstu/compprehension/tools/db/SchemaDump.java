package org.vstu.compprehension.tools.db;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class SchemaDump {

    private static final Set<String> SKIP_TABLES = Set.of("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

    private final Connection connection;

    SchemaDump(Connection connection) {
        this.connection = connection;
    }

    void execute(String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    void dumpDdl(Path out) throws SQLException, IOException {
        try (var writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8); var statement = connection.createStatement()) {
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
            for (var table : tables()) {
                try (var rows = statement.executeQuery("show create table `" + table + "`")) {
                    rows.next();
                    writer.write(rows.getString(2).replaceAll(" AUTO_INCREMENT=\\d+", "") + ";\n\n");
                }
            }
            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        }
    }

    void dumpData(Path out) throws SQLException, IOException {
        try (var writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8); var statement = connection.createStatement()) {
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
            for (var table : tables()) {
                dumpTable(table, statement, writer);
            }
            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        }
    }

    Map<String, String> checksums() throws SQLException {
        var result = new TreeMap<String, String>();
        try (var statement = connection.createStatement()) {
            for (var table : tables()) {
                try (var rows = statement.executeQuery("checksum table `" + table + "`")) {
                    rows.next();
                    result.put(table, rows.getString(2));
                }
            }
        }
        return result;
    }

    private void dumpTable(String table, Statement statement, Writer writer) throws SQLException, IOException {
        var generated = generatedColumns(table);
        try (var rows = statement.executeQuery("select * from `" + table + "`")) {
            ResultSetMetaData metadata = rows.getMetaData();
            var columns = new ArrayList<Integer>();
            var head = new StringBuilder("INSERT INTO `" + table + "` (");
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                if (generated.contains(metadata.getColumnName(i))) {
                    continue;
                }
                if (!columns.isEmpty()) {
                    head.append(", ");
                }
                head.append('`').append(metadata.getColumnName(i)).append('`');
                columns.add(i);
            }
            head.append(") VALUES (");
            var any = false;
            while (rows.next()) {
                any = true;
                var row = new StringBuilder(head);
                for (int k = 0; k < columns.size(); k++) {
                    if (k > 0) {
                        row.append(", ");
                    }
                    row.append(literal(rows, columns.get(k), metadata.getColumnType(columns.get(k))));
                }
                writer.write(row.append(");\n").toString());
            }
            if (any) {
                writer.write("\n");
            }
        }
    }

    private List<String> tables() throws SQLException {
        var result = new ArrayList<String>();
        try (var statement = connection.prepareStatement(
                "select table_name from information_schema.tables "
                        + "where table_schema = database() and table_type = 'BASE TABLE' order by table_name");
             var rows = statement.executeQuery()) {
            while (rows.next()) {
                var table = rows.getString(1);
                if (!SKIP_TABLES.contains(table)) {
                    result.add(table);
                }
            }
        }
        return result;
    }

    private Set<String> generatedColumns(String table) throws SQLException {
        var result = new HashSet<String>();
        try (var statement = connection.prepareStatement(
                "select column_name from information_schema.columns "
                        + "where table_schema = database() and table_name = ? and extra like '%GENERATED%'")) {
            statement.setString(1, table);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(rows.getString(1));
                }
            }
        }
        return result;
    }

    private static String literal(ResultSet rows, int column, int type) throws SQLException {
        switch (type) {
            case Types.BIT, Types.BOOLEAN -> {
                var value = rows.getBoolean(column);
                return rows.wasNull() ? "NULL" : (value ? "1" : "0");
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> {
                var bytes = rows.getBytes(column);
                if (bytes == null) {
                    return "NULL";
                }
                if (bytes.length == 0) {
                    return "''";
                }
                var result = new StringBuilder("0x");
                for (var b : bytes) {
                    result.append(String.format("%02X", b));
                }
                return result.toString();
            }
            default -> {
                var value = rows.getString(column);
                if (value == null) {
                    return "NULL";
                }
                var result = new StringBuilder("'");
                for (var ch : value.toCharArray()) {
                    switch (ch) {
                        case '\\' -> result.append("\\\\");
                        case '\'' -> result.append("\\'");
                        case '\n' -> result.append("\\n");
                        case '\r' -> result.append("\\r");
                        case '\0' -> result.append("\\0");
                        case '\u001a' -> result.append("\\Z");
                        default -> result.append(ch);
                    }
                }
                return result.append('\'').toString();
            }
        }
    }
}
