package dbtools;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SchemaDump {
    private static final Set<String> SKIP_TABLES = Set.of("DATABASECHANGELOG", "DATABASECHANGELOGLOCK");

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: SchemaDump <ddl|data|checksum|exec> <url> <user> <password> [outFile|sql]");
            System.exit(2);
        }
        String cmd = args[0], url = args[1], user = args[2], password = args[3];
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            switch (cmd) {
                case "ddl" -> dumpDdl(c, Path.of(args[4]));
                case "data" -> dumpData(c, Path.of(args[4]));
                case "checksum" -> checksums(c);
                case "exec" -> {
                    try (Statement st = c.createStatement()) {
                        st.execute(args[4]);
                    }
                }
                default -> throw new IllegalArgumentException("unknown command " + cmd);
            }
        }
    }

    private static List<String> tables(Connection c) throws Exception {
        List<String> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "select table_name from information_schema.tables " +
                "where table_schema = database() and table_type = 'BASE TABLE' order by table_name")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String t = rs.getString(1);
                if (!SKIP_TABLES.contains(t)) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    private static void dumpDdl(Connection c, Path out) throws Exception {
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8); Statement st = c.createStatement()) {
            w.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
            for (String t : tables(c)) {
                ResultSet rs = st.executeQuery("show create table `" + t + "`");
                rs.next();
                String ddl = rs.getString(2).replaceAll(" AUTO_INCREMENT=\\d+", "");
                w.write(ddl + ";\n\n");
            }
            w.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        }
    }

    private static void dumpData(Connection c, Path out) throws Exception {
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8); Statement st = c.createStatement()) {
            w.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
            for (String t : tables(c)) {
                Set<String> generated = generatedColumns(c, t);
                ResultSet rs = st.executeQuery("select * from `" + t + "`");
                ResultSetMetaData md = rs.getMetaData();
                List<Integer> cols = new ArrayList<>();
                StringBuilder head = new StringBuilder("INSERT INTO `" + t + "` (");
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    if (generated.contains(md.getColumnName(i))) continue;
                    if (!cols.isEmpty()) head.append(", ");
                    head.append('`').append(md.getColumnName(i)).append('`');
                    cols.add(i);
                }
                head.append(") VALUES (");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    StringBuilder row = new StringBuilder(head);
                    for (int k = 0; k < cols.size(); k++) {
                        if (k > 0) row.append(", ");
                        row.append(literal(rs, cols.get(k), md.getColumnType(cols.get(k))));
                    }
                    w.write(row.append(");\n").toString());
                }
                if (any) w.write("\n");
            }
            w.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        }
    }

    private static Set<String> generatedColumns(Connection c, String table) throws Exception {
        Set<String> result = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement(
                "select column_name from information_schema.columns " +
                "where table_schema = database() and table_name = ? and extra like '%GENERATED%'")) {
            ps.setString(1, table);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(rs.getString(1));
        }
        return result;
    }

    private static String literal(ResultSet rs, int col, int type) throws Exception {
        switch (type) {
            case Types.BIT, Types.BOOLEAN -> {
                boolean b = rs.getBoolean(col);
                return rs.wasNull() ? "NULL" : (b ? "1" : "0");
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> {
                byte[] bytes = rs.getBytes(col);
                if (bytes == null) return "NULL";
                if (bytes.length == 0) return "''";
                StringBuilder sb = new StringBuilder("0x");
                for (byte b : bytes) sb.append(String.format("%02X", b));
                return sb.toString();
            }
            default -> {
                String s = rs.getString(col);
                if (s == null) return "NULL";
                StringBuilder sb = new StringBuilder("'");
                for (char ch : s.toCharArray()) {
                    switch (ch) {
                        case '\\' -> sb.append("\\\\");
                        case '\'' -> sb.append("\\'");
                        case '\n' -> sb.append("\\n");
                        case '\r' -> sb.append("\\r");
                        case '\0' -> sb.append("\\0");
                        case '\u001a' -> sb.append("\\Z");
                        default -> sb.append(ch);
                    }
                }
                return sb.append('\'').toString();
            }
        }
    }

    private static void checksums(Connection c) throws Exception {
        try (Statement st = c.createStatement()) {
            for (String t : tables(c)) {
                ResultSet rs = st.executeQuery("checksum table `" + t + "`");
                rs.next();
                System.out.println(t + "\t" + rs.getString(2));
            }
        }
    }
}
