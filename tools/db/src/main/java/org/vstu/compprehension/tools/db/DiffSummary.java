package org.vstu.compprehension.tools.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class DiffSummary {

    private static final List<String> NOISE_TABLES = List.of("DATABASECHANGELOG", "jobs_jobrunr");
    private static final List<Map.Entry<String, String>> TYPE_ALIASES = List.of(
            Map.entry("bigint(19)", "bigint"), Map.entry("int(10)", "int"), Map.entry("integer", "int"),
            Map.entry("bit(1)", "bit"), Map.entry("text(65535)", "text"), Map.entry("float(12)", "float"),
            Map.entry("float(23)", "float"), Map.entry("double(22)", "double"), Map.entry("float(53)", "double"),
            Map.entry("tinyint(3)", "tinyint"), Map.entry("tinyint", "int"));

    private static final Pattern SECTION = Pattern.compile("^(Missing|Unexpected|Changed) (\\w[\\w ]*)\\(s\\): ?(NONE)?$");
    private static final Pattern CHANGED_FIELD = Pattern.compile("\\s+(\\w+) changed from '(.*)' to '(.*)'");
    private static final Pattern CHANGED_PK_COLUMNS = Pattern.compile("\\s+columns changed from '\\[(.*)]' to '\\[(.*)]'");
    private static final Pattern PK_OR_FK_INDEX = Pattern.compile("(?i)(PRIMARY |fk_).*");
    private static final Pattern IMPLICIT_PK_INDEX = Pattern.compile("IX_\\w+PK .*");

    private DiffSummary() {
    }

    static String summarize(String report) {
        String section = null;
        String current = null;
        var out = new ArrayList<String>();
        for (var line : report.split("\\R")) {
            var header = SECTION.matcher(line);
            if (header.matches()) {
                section = header.group(3) != null ? null : header.group(1) + " " + header.group(2);
                continue;
            }
            if (section == null || section.endsWith("Catalog") || !line.startsWith("     ")
                    || NOISE_TABLES.stream().anyMatch(line::contains)) {
                continue;
            }
            var nested = line.startsWith("          ");
            switch (section) {
                case "Changed Column" -> {
                    if (!nested) {
                        current = line.strip();
                        continue;
                    }
                    var change = CHANGED_FIELD.matcher(line);
                    if (!change.lookingAt() || change.group(1).equals("order") || change.group(1).equals("certainDataType")) {
                        continue;
                    }
                    if (change.group(1).equals("type") && sameType(change.group(2), change.group(3))) {
                        continue;
                    }
                    if (change.group(1).equals("defaultValue") && stripQuotes(change.group(2)).equals(stripQuotes(change.group(3)))) {
                        continue;
                    }
                    out.add(section + " | " + current + ": " + change.group(1) + " '" + change.group(2) + "' -> '" + change.group(3) + "'");
                }
                case "Changed Primary Key" -> {
                    if (!nested) {
                        current = line.strip();
                        continue;
                    }
                    var change = CHANGED_PK_COLUMNS.matcher(line);
                    if (change.lookingAt() && columnNames(change.group(1)).equals(columnNames(change.group(2)))) {
                        continue;
                    }
                    out.add(section + " | " + current + ": " + line.strip());
                }
                case "Changed Foreign Key" -> {
                }
                default -> {
                    if (nested
                            || section.equals("Unexpected Index") && PK_OR_FK_INDEX.matcher(line.strip()).matches()
                            || section.equals("Missing Index") && IMPLICIT_PK_INDEX.matcher(line.strip()).matches()) {
                        continue;
                    }
                    out.add(section + " | " + line.strip());
                }
            }
        }
        return out.isEmpty() ? "(no substantive differences)" : String.join("\n", out);
    }

    private static boolean sameType(String a, String b) {
        var left = normalizeType(a);
        var right = normalizeType(b);
        return left.equals(right)
                || left.startsWith("enum") && right.startsWith("varchar")
                || left.startsWith("varchar") && right.startsWith("enum");
    }

    private static String normalizeType(String type) {
        var result = type.toLowerCase().strip().replaceAll("\\s*byte\\)", ")");
        for (var alias : TYPE_ALIASES) {
            result = result.replace(alias.getKey(), alias.getValue());
        }
        return result;
    }

    private static String stripQuotes(String value) {
        return value.replaceAll("^'+|'+$", "");
    }

    private static HashSet<String> columnNames(String list) {
        return Arrays.stream(list.split(", "))
                .map(column -> column.substring(column.lastIndexOf('.') + 1))
                .collect(Collectors.toCollection(HashSet::new));
    }
}
