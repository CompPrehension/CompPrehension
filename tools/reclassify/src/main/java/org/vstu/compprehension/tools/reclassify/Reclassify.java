package org.vstu.compprehension.tools.reclassify;

import its.model.nodes.DecisionTreeElement;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.businesslogic.Concept;
import org.vstu.compprehension.businesslogic.Law;
import org.vstu.compprehension.businesslogic.Skill;
import org.vstu.compprehension.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.MeaningTreeOrderQuestionBuilder;
import org.vstu.compprehension.businesslogic.domains.helpers.meaningtree.MeaningTreeUtils;
import org.vstu.compprehension.businesslogic.storage.SerializableQuestion;
import org.vstu.compprehension.common.MathHelper;
import org.vstu.compprehension.data.domain.DomainData;
import org.vstu.compprehension.data.domain.DomainOptionsData;
import org.vstu.compprehension.data.question.AnswerData;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.data.question.QuestionMetadataData;
import org.vstu.compprehension.data.question.QuestionMetadataWithData;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.services.LocalizationService;
import org.vstu.compprehension.services.RandomProvider;
import org.vstu.meaningtree.SupportedLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public final class Reclassify {

    private static final String DOMAIN_SHORTNAME = "expression_dt";
    private static final int VERSION = 12;
    private static final String DEFAULT_ENV_FILE = ".env/compprehension-env.env";

    private static final String SELECT = """
            SELECT qm.id, qm.name, qm.domain_shortname, qm.template_id, qm.tag_bits, qm.concept_bits, qm.law_bits,
                   qm.skill_bits, qm.violation_bits, qm.trace_concept_bits, qm.solution_structural_complexity,
                   qm.integral_complexity, qm.solution_steps, qm.distinct_errors_count, qm._version, qm.origin,
                   qm.origin_license, qm.structure_hash, qd.data
            FROM questions_meta qm JOIN questions_data qd ON qd.id = qm.question_data_id
            WHERE qm.domain_shortname = ? AND qm._version = ? AND qm.id > ?
            ORDER BY qm.id LIMIT ?
            """;
    private static final String UPDATE = """
            UPDATE questions_meta SET concept_bits = ?, trace_concept_bits = ?, skill_bits = ?, violation_bits = ?,
                   integral_complexity = ?, distinct_errors_count = ?, solution_structural_complexity = ?, solution_steps = ?
            WHERE id = ?
            """;
    private static final String DELETE = "DELETE FROM questions_meta WHERE id = ?";

    private final Options options;
    private final ProgrammingLanguageExpressionDTDomain domain;
    private final Set<String> lawsKnownToTrees;
    private final Map<String, Long> lawBits;
    private int scanned, changed, unchanged, invalid, failed;

    private Reclassify(Options options) {
        this.options = options;
        this.domain = newDomain();
        this.lawsKnownToTrees = lawsKnownToTrees(domain);
        this.lawBits = domain.getNegativeLaws().stream().collect(Collectors.toMap(Law::getName, Law::getBitmask));
    }

    public static void main(String[] args) throws Exception {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(Options.USAGE);
            System.exit(1);
            return;
        }
        var tool = new Reclassify(options);
        try (var connection = DriverManager.getConnection(options.url, options.user, options.password)) {
            connection.setAutoCommit(false);
            tool.run(connection);
        }
        tool.summary();
        System.exit(tool.failed > 0 && !options.deleteInvalid ? 2 : 0);
    }

    private void run(Connection connection) throws SQLException {
        int lastId = 0;
        while (true) {
            var batch = load(connection, lastId);
            if (batch.isEmpty()) {
                break;
            }
            for (var record : batch) {
                lastId = record.getId();
                if (!options.ids.isEmpty() && !options.ids.contains(record.getId())) {
                    continue;
                }
                if (options.shards > 1 && record.getId() % options.shards != options.shard) {
                    continue;
                }
                process(connection, record);
            }
            if (!options.dryRun) {
                connection.commit();
            }
        }
    }

    private List<QuestionMetadataWithData> load(Connection connection, int afterId) throws SQLException {
        var result = new ArrayList<QuestionMetadataWithData>();
        try (var statement = connection.prepareStatement(SELECT)) {
            statement.setString(1, DOMAIN_SHORTNAME);
            statement.setInt(2, VERSION);
            statement.setInt(3, afterId);
            statement.setInt(4, options.batchSize);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(toRecord(rows));
                }
            }
        }
        return result;
    }

    private static QuestionMetadataWithData toRecord(ResultSet row) throws SQLException {
        var record = new QuestionMetadataWithData();
        record.setId(row.getInt("id"));
        record.setName(row.getString("name"));
        record.setDomainShortname(row.getString("domain_shortname"));
        record.setTemplateId(row.getString("template_id"));
        record.setTagBits(row.getLong("tag_bits"));
        record.setConceptBits(row.getLong("concept_bits"));
        record.setLawBits(row.getLong("law_bits"));
        record.setSkillBits(row.getLong("skill_bits"));
        record.setViolationBits(row.getLong("violation_bits"));
        record.setTraceConceptBits(row.getLong("trace_concept_bits"));
        record.setSolutionStructuralComplexity(row.getDouble("solution_structural_complexity"));
        record.setIntegralComplexity(row.getDouble("integral_complexity"));
        record.setSolutionSteps(row.getInt("solution_steps"));
        record.setDistinctErrorsCount(row.getInt("distinct_errors_count"));
        record.setVersion(row.getInt("_version"));
        record.setOrigin(row.getString("origin"));
        record.setOriginLicense(row.getString("origin_license"));
        record.setStructureHash(row.getString("structure_hash"));
        record.setData(SerializableQuestion.deserializeFromString(row.getString("data")));
        return record;
    }

    private void process(Connection connection, QuestionMetadataWithData record) throws SQLException {
        scanned++;
        QuestionMetadataData fresh;
        try {
            fresh = MeaningTreeOrderQuestionBuilder.metadataRecalculate(domain, record);
            if (fresh != null && options.solver) {
                applySolver(record, fresh);
            }
        } catch (Exception e) {
            failed++;
            System.out.printf("FAILED   %d %s: %s%s%n", record.getId(), record.getName(), e, options.deleteInvalid ? " -> delete" : "");
            if (options.verbose) {
                e.printStackTrace(System.out);
            }
            deleteIfRequested(connection, record);
            return;
        }
        if (fresh == null) {
            invalid++;
            System.out.printf("INVALID  %d %s%s%n", record.getId(), record.getName(), options.deleteInvalid ? " -> delete" : "");
            deleteIfRequested(connection, record);
            return;
        }
        var diff = describeDiff(record, fresh);
        if (diff.isEmpty()) {
            unchanged++;
            if (options.verbose) {
                System.out.printf("SAME     %d %s%n", record.getId(), record.getName());
            }
            return;
        }
        changed++;
        System.out.printf("CHANGED  %d %s%n%s", record.getId(), record.getName(), diff);
        if (!options.dryRun) {
            try (var statement = connection.prepareStatement(UPDATE)) {
                statement.setLong(1, fresh.getConceptBits());
                statement.setLong(2, fresh.getTraceConceptBits());
                statement.setLong(3, fresh.getSkillBits());
                statement.setLong(4, fresh.getViolationBits());
                statement.setDouble(5, fresh.getIntegralComplexity());
                statement.setInt(6, fresh.getDistinctErrorsCount());
                statement.setDouble(7, fresh.getSolutionStructuralComplexity());
                statement.setInt(8, fresh.getSolutionSteps());
                statement.setInt(9, record.getId());
                statement.executeUpdate();
            }
        }
    }

    private void deleteIfRequested(Connection connection, QuestionMetadataWithData record) throws SQLException {
        if (options.deleteInvalid && !options.dryRun) {
            try (var statement = connection.prepareStatement(DELETE)) {
                statement.setInt(1, record.getId());
                statement.executeUpdate();
            }
        }
    }

    private void applySolver(QuestionMetadataWithData record, QuestionMetadataData fresh) {
        var language = MeaningTreeUtils.detectLanguageFromTags(record.getTagBits(), domain);
        var tags = List.of(domain.getTag(tagOf(language)));
        var question = QuestionData.of(domain.makeQuestion(record, tags, Language.ENGLISH).getContent());
        var laws = new HashSet<String>();
        var skills = new HashSet<String>();
        var remaining = new ArrayList<>(question.getContent().getAnswerObjects());
        var prefix = new ArrayList<AnswerObjectData>();
        while (true) {
            AnswerObjectData next = null;
            var finished = false;
            for (var candidate : remaining) {
                var attempt = new ArrayList<>(prefix);
                attempt.add(candidate);
                var result = domain.judgeQuestion(question, responses(attempt), tags, Language.ENGLISH);
                laws.addAll(result.domainNegativeLaws);
                skills.addAll(result.domainSkills);
                if (result.isAnswerCorrect && next == null) {
                    next = candidate;
                    finished = result.IterationsLeft == 0;
                }
            }
            if (next == null) {
                throw new IllegalStateException("решатель не нашёл верного продолжения после " + prefix.size() + " шагов");
            }
            prefix.add(next);
            remaining.remove(next);
            if (finished || remaining.isEmpty()) {
                break;
            }
        }
        long solverLaws = laws.stream().map(lawBits::get).filter(b -> b != null).reduce(0L, (a, b) -> a | b);
        long treeLaws = lawsKnownToTrees.stream().map(lawBits::get).filter(b -> b != null).reduce(0L, (a, b) -> a | b);
        fresh.setViolationBits((fresh.getViolationBits() & ~treeLaws) | solverLaws);
        long solverSkills = 0;
        for (var name : skills) {
            var skill = domain.getSkill(name);
            if (skill == null) {
                continue;
            }
            var bases = skill.getBaseSkills() == null || skill.getBaseSkills().isEmpty() ? List.of(skill) : skill.getBaseSkills();
            for (var base : bases) {
                solverSkills |= base.getBitmask();
            }
        }
        fresh.setSkillBits(solverSkills);
        int steps = prefix.size() + 1;
        int errors = Long.bitCount(fresh.getViolationBits());
        fresh.setSolutionSteps(steps);
        fresh.setSolutionStructuralComplexity((double) steps + 1);
        fresh.setDistinctErrorsCount(errors);
        fresh.setIntegralComplexity(MathHelper.sigmoid((0.18549906 * steps - 0.01883239 * errors) * 4 - 2));
    }

    private String describeDiff(QuestionMetadataData old, QuestionMetadataData fresh) {
        var out = new StringBuilder();
        appendBitsDiff(out, "concepts  ", old.getConceptBits(), fresh.getConceptBits(), bits -> domain.conceptsFromBitmask(bits).stream().map(Concept::getName).collect(Collectors.toSet()));
        appendBitsDiff(out, "violations", old.getViolationBits(), fresh.getViolationBits(), bits -> domain.negativeLawFromBitmask(bits).stream().map(Law::getName).collect(Collectors.toSet()));
        appendBitsDiff(out, "skills    ", old.getSkillBits(), fresh.getSkillBits(), bits -> domain.skillsFromBitmask(bits).stream().map(Skill::getName).collect(Collectors.toSet()));
        if (!old.getSolutionSteps().equals(fresh.getSolutionSteps())) {
            out.append(String.format("    steps      %d -> %d%n", old.getSolutionSteps(), fresh.getSolutionSteps()));
        }
        if (Math.abs(old.getIntegralComplexity() - fresh.getIntegralComplexity()) > 1e-6) {
            out.append(String.format("    complexity %.4f -> %.4f%n", old.getIntegralComplexity(), fresh.getIntegralComplexity()));
        }
        return out.toString();
    }

    private static void appendBitsDiff(StringBuilder out, String label, long oldBits, long newBits, Function<Long, Set<String>> names) {
        if (oldBits == newBits) {
            return;
        }
        var removed = new LinkedHashSet<>(names.apply(oldBits & ~newBits));
        var added = new LinkedHashSet<>(names.apply(newBits & ~oldBits));
        out.append("    ").append(label);
        if (!removed.isEmpty()) {
            out.append(" -").append(removed);
        }
        if (!added.isEmpty()) {
            out.append(" +").append(added);
        }
        out.append(System.lineSeparator());
    }

    private void summary() {
        System.out.printf("%nscanned=%d changed=%d unchanged=%d invalid=%d failed=%d%s%n",
                scanned, changed, unchanged, invalid, failed, options.dryRun ? " (dry run, nothing written)" : "");
    }

    private static List<AnswerData> responses(List<AnswerObjectData> answers) {
        return answers.stream().map(a -> AnswerData.of(a, a)).collect(Collectors.toList());
    }

    private static String tagOf(SupportedLanguage language) {
        return switch (language) {
            case JAVA -> "Java";
            case PYTHON -> "Python";
            default -> "C++";
        };
    }

    private static Set<String> lawsKnownToTrees(ProgrammingLanguageExpressionDTDomain domain) {
        var laws = new HashSet<String>();
        for (var model : domain.getDomainSolvingModels()) {
            for (var tree : model.getDecisionTrees().values()) {
                collectLaws(tree, laws, new HashSet<>());
            }
        }
        return laws;
    }

    private static void collectLaws(DecisionTreeElement element, Set<String> into, Set<DecisionTreeElement> seen) {
        if (!seen.add(element)) {
            return;
        }
        var law = element.getMetadata().getString("law");
        if (law != null) {
            into.addAll(Arrays.asList(law.split(";")));
        }
        element.getLinkedElements().forEach(linked -> collectLaws(linked, into, seen));
    }

    private static ProgrammingLanguageExpressionDTDomain newDomain() {
        return new ProgrammingLanguageExpressionDTDomain(
                new DomainData("ProgrammingLanguageExpressionDTDomain", DOMAIN_SHORTNAME, "1", new DomainOptionsData()),
                new ProgrammingLanguageExpressionDomain(
                        new DomainData("ProgrammingLanguageExpressionDomain", "expression", "1", new DomainOptionsData()),
                        new KeyLocalizationService(),
                        new SeededRandomProvider(),
                        null));
    }

    private static final class KeyLocalizationService implements LocalizationService {
        @Override
        public @NotNull String getMessage(@NotNull String messageId, @NotNull Locale locale) {
            return messageId;
        }

        @Override
        public @NotNull String getMessage(@NotNull String messageId, @NotNull Language language) {
            return messageId;
        }
    }

    private static final class SeededRandomProvider implements RandomProvider {
        private RandomGenerator random = new SplittableRandom(0);

        @Override
        public RandomGenerator getRandom() {
            return random;
        }

        @Override
        public void reset(int seed) {
            random = new SplittableRandom(seed);
        }
    }

    private static final class Options {
        static final String USAGE = """
                Usage: java -jar reclassify.jar [--url jdbc:mysql://host:3306/db] [--user U] [--password P] [--env FILE]
                                                [--dry-run] [--solver] [--delete-invalid] [--ids 1,2,3] [--shard K/N]
                                                [--batch N] [--verbose]
                Without --url the connection is read from --env (default .env/compprehension-env.env in the current
                directory): MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD.""";

        String url;
        String user;
        String password;
        String envFile = DEFAULT_ENV_FILE;
        boolean dryRun;
        boolean deleteInvalid;
        boolean solver;
        boolean verbose;
        int batchSize = 1000;
        int shard = 0;
        int shards = 1;
        Set<Integer> ids = new HashSet<>();

        static Options parse(String[] args) {
            var options = new Options();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--url" -> options.url = args[++i];
                    case "--user" -> options.user = args[++i];
                    case "--password" -> options.password = args[++i];
                    case "--env" -> options.envFile = args[++i];
                    case "--batch" -> options.batchSize = Integer.parseInt(args[++i]);
                    case "--ids" -> Arrays.stream(args[++i].split(",")).map(String::trim).map(Integer::parseInt).forEach(options.ids::add);
                    case "--shard" -> {
                        var parts = args[++i].split("/");
                        options.shard = Integer.parseInt(parts[0]);
                        options.shards = Integer.parseInt(parts[1]);
                    }
                    case "--dry-run" -> options.dryRun = true;
                    case "--delete-invalid" -> options.deleteInvalid = true;
                    case "--solver" -> options.solver = true;
                    case "--verbose" -> options.verbose = true;
                    default -> throw new IllegalArgumentException("Неизвестный аргумент: " + args[i]);
                }
            }
            if (options.url == null) {
                options.fromEnv();
            }
            return options;
        }

        private void fromEnv() {
            var path = Path.of(envFile);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Не задан --url, а файл " + path.toAbsolutePath() + " не найден");
            }
            var env = new HashMap<String, String>();
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
            var database = env.get("MYSQL_DATABASE");
            if (database == null) {
                throw new IllegalArgumentException("В " + path + " нет MYSQL_DATABASE");
            }
            url = "jdbc:mysql://" + env.getOrDefault("MYSQL_HOST", "localhost") + ":" + env.getOrDefault("MYSQL_PORT", "3306")
                    + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
            if (user == null) {
                user = env.getOrDefault("MYSQL_USER", "root");
            }
            if (password == null) {
                password = env.get("MYSQL_PASSWORD");
            }
        }
    }
}
