package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.Checkpointer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Long.bitCount;

@Log4j2
public class QuestionBank {
    public static int STAGE_TEMPLATE = 1;
    public static int STAGE_QUESTION = 2;
    public static int STAGE_READY = 3; // in this stage the generated question may be moved to the production bank
    public static int STAGE_EXPORTED = 4;
    public static int GENERATOR_VERSION = 10;

    private final HashMap<String, RemoteFileService> fileServices;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionMetadataManager questionMetadataManager;

    public QuestionBank(
            Collection<DomainEntity> domains,
            QuestionMetadataRepository questionMetadataRepository,
            QuestionMetadataManager questionMetadataManager) throws URISyntaxException {
        this.fileServices = new HashMap<>();
        for (var domain : domains) {
            var fs = new RemoteFileService(domain.getOptions().getStorageUploadFilesBaseUrl(),
                    Optional.ofNullable(domain.getOptions().getStorageDownloadFilesBaseUrl()).orElse(domain.getOptions().getStorageUploadFilesBaseUrl()),
                    Optional.ofNullable(domain.getOptions().getStorageDummyDirsForNewFile()).orElse(1));

            this.fileServices.put(domain.getShortName(), fs);
            this.fileServices.put(domain.getName(), fs);
        }

        this.questionMetadataRepository = questionMetadataRepository;
        this.questionMetadataManager = questionMetadataManager;
    }

    public QuestionBank(
            String domainId,
            RemoteFileService fileService,
            QuestionMetadataRepository questionMetadataRepository,
            QuestionMetadataManager questionMetadataManager) {
        this.fileServices = new HashMap<>();
        this.fileServices.put(domainId, fileService);

        this.questionMetadataRepository = questionMetadataRepository;
        this.questionMetadataManager = questionMetadataManager;
    }

    private List<QuestionMetadataEntity> findLastNQuestionsMeta(QuestionBankSearchRequest qr, ExerciseAttemptEntity exerciseAttempt, int n) {
        List<QuestionEntity> list = exerciseAttempt.getQuestions();
        List<QuestionMetadataEntity> result = new ArrayList<>();
        long toSkip = Math.max(0, list.size() - n);
        for (QuestionEntity questionEntity : list) {
            if (toSkip > 0) {
                toSkip--;
                continue;
            }
            QuestionMetadataEntity meta = questionEntity.getMetadata();
            if (meta != null)
                result.add(meta);
        }
        return result;
    }

    private QuestionBankSearchRequest createBankSearchRequest(QuestionRequest qr) {
        var minComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMin();
        var maxComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMax();

        return QuestionBankSearchRequest.fromQuestionRequest(qr, minComplexity, maxComplexity);
    }

    public boolean isMatch(@NotNull QuestionMetadataEntity meta, @NotNull QuestionRequestLogEntity qrLog) {
        var minComplexity = questionMetadataManager.getComplexityStats(qrLog.getDomainShortname()).getMin();
        var maxComplexity = questionMetadataManager.getComplexityStats(qrLog.getDomainShortname()).getMax();

        var bankSearchRequest = QuestionBankSearchRequest.fromQuestionRequestLog(qrLog, minComplexity, maxComplexity);
        return isMatch(meta, bankSearchRequest);
    }

    private boolean isMatch(@NotNull QuestionMetadataEntity meta, @NotNull QuestionBankSearchRequest qr) {
        // Если не совпадает имя домена – мы пытаемся сделать что-то Неправильно!
        if (qr.getDomainShortname() != null && ! qr.getDomainShortname().equalsIgnoreCase(meta.getDomainShortname())) {
            throw new RuntimeException(String.format("Trying matching a question with a QuestionRequest(LogEntity) of different domain ! (%s != %s)", meta.getDomainShortname(), qr.getDomainShortname()));
        }

        // проверка запрещаемых критериев
        if (meta.getSolutionSteps() < qr.getStepsMin()
                || qr.getStepsMax() != 0 && meta.getSolutionSteps() > qr.getStepsMax()
                || qr.getDeniedConceptsBitmask() != 0 && (meta.getConceptBits() & qr.getDeniedConceptsBitmask()) != 0
                || qr.getDeniedLawsBitmask() != 0 && (meta.getViolationBits() & qr.getDeniedLawsBitmask()) != 0
                || qr.getTargetTagsBitmask() != 0 && (meta.getTagBits() & qr.getTargetTagsBitmask()) != meta.getTagBits() // требуем наличия всех тэгов
        ) {
            return false;
        }

        // Если есть запрет по ID шаблона или имени вопроса
        if (qr.getDeniedQuestionTemplateIds() != null && !qr.getDeniedQuestionTemplateIds().isEmpty() && qr.getDeniedQuestionTemplateIds().contains(meta.getTemplateId())
                || qr.getDeniedQuestionNames() != null && !qr.getDeniedQuestionNames().isEmpty() && qr.getDeniedQuestionNames().contains(meta.getName())) {
            return false;
        }

        // сложность должна быть <= от запрашиваемой
        if (qr.getComplexity() != 0 && meta.getIntegralComplexity() > qr.getComplexity()) {
            return false;
        }

        // Присутствует хотя бы один целевой концепт и закон (если они заданы)
        if (qr.getTargetConceptsBitmask() != 0 && (meta.getConceptBits() & qr.getTargetConceptsBitmask()) == 0
                || qr.getTargetLawsBitmask() != 0 && (meta.getLawBits() & qr.getTargetLawsBitmask()) == 0
        ) {
            return false;
        }

        return true;
    }

    public int countQuestions(QuestionRequest qr) {
        var bankSearchRequest = createBankSearchRequest(qr);
        return questionMetadataRepository.countQuestions(bankSearchRequest);
    }

    /**
     * Find question templates in the questions bank. If no questions satisfy the requirements exactly, finds ones of similar complexity. Denied concepts and laws (laws map to violations on DB) cannot present in result questions anyway.
     * @param qr QuestionRequest
     * @param limit maximum questions to return (must be > 0)
     * @return questions found or empty list if the requirements cannot be satisfied
     */
    public List<Question> searchQuestions(Domain domain, ExerciseAttemptEntity attempt, QuestionRequest qr, int limit) {

        Checkpointer ch = new Checkpointer(log);

        var bankSearchRequest = createBankSearchRequest(qr);

        QuestionMetadataManager metaMgr = this.questionMetadataManager;
        int nQuestionsInAttempt = Optional.ofNullable(bankSearchRequest.getDeniedQuestionNames()).map(List::size).orElse(0);
        int queryLimit = limit + nQuestionsInAttempt;
        int hardLimit = 25;

        double complexity = bankSearchRequest.getComplexity();

        long targetConceptsBitmask = bankSearchRequest.getTargetConceptsBitmask();
        long deniedConceptsBitmask = bankSearchRequest.getDeniedConceptsBitmask();
        long unwantedConceptsBitmask = findLastNQuestionsMeta(bankSearchRequest, attempt, 4).stream()
                .mapToLong(QuestionMetadataEntity::getConceptBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetConceptsBitmask &= ~deniedConceptsBitmask;
        long targetConceptsBitmaskInPlan = bankSearchRequest.getTargetConceptsBitmask();

        // use laws, for e.g. Expr domain
        long targetLawsBitmask = bankSearchRequest.getTargetLawsBitmask();
        long deniedLawsBitmask = bankSearchRequest.getDeniedLawsBitmask();
        long unwantedLawsBitmask = findLastNQuestionsMeta(bankSearchRequest, attempt, 4).stream()
                .mapToLong(QuestionMetadataEntity::getLawBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetLawsBitmask &= ~deniedLawsBitmask;
        long targetViolationsBitmaskInPlan = bankSearchRequest.getTargetLawsBitmask();

        // use violations from all questions is exercise attempt
        long unwantedViolationsBitmask = attempt.getQuestions().stream()
                .map(QuestionEntity::getMetadata)
                .filter(Objects::nonNull)
                .mapToLong(QuestionMetadataEntity::getViolationBits)
                .reduce((t, t2) -> t | t2).orElse(0);

        ch.hit("searchQuestionsAdvanced - bitmasks prepared");

        List<QuestionMetadataEntity> foundQuestionMetas = questionMetadataRepository.findSampleAroundComplexityWithoutQIds(bankSearchRequest, 0.1, queryLimit, 12);

        ch.hit("searchQuestionsAdvanced - query executed with " + foundQuestionMetas.size() + " candidates");

        foundQuestionMetas = filterQuestionMetas(foundQuestionMetas,
                complexity,
                targetConceptsBitmask,
                unwantedConceptsBitmask,
                unwantedLawsBitmask,
                unwantedViolationsBitmask,
                Math.min(limit, hardLimit)  // Note: queryLimit >= limit
            );

        ch.hit("searchQuestionsAdvanced - filtered up to " + foundQuestionMetas.size() + " candidates");
        log.info("searchQuestionsAdvanced - candidates: {}", foundQuestionMetas.stream().map(QuestionMetadataEntity::getName).toList());

        // set concepts from request (for future reference via questions' saved metadata)
        for (QuestionMetadataEntity m : foundQuestionMetas) {
            m.setConceptBitsInPlan(targetConceptsBitmaskInPlan);
            m.setViolationBitsInPlan(targetViolationsBitmaskInPlan);
            // Save actual requested bits as well
            m.setConceptBitsInRequest(targetConceptsBitmask);
            m.setViolationBitsInRequest(targetLawsBitmask);

            /// debug check law bits
            if (targetLawsBitmask != 0 && (targetLawsBitmask & m.getViolationBits()) == 0) {
                log.warn("No LAW bits matched: {} {}", targetLawsBitmask, m.getName());
            }
        }

        List<Question> loadedQuestions = loadQuestions(domain, foundQuestionMetas);
        ch.hit("searchQuestionsAdvanced - files loaded");

        if (loadedQuestions.size() == 1) {
            // increment the question's usage counter
            val meta = loadedQuestions.get(0).getQuestionData().getMetadata();
            if (meta == null) {
                throw new RuntimeException("No metadata for question " + loadedQuestions.get(0).getQuestionData().getId());
            }
            
            meta.setUsedCount(Optional.ofNullable(meta.getUsedCount()).orElse(0L) + 1);
            meta.setLastAttemptId(attempt.getId());
            meta.setDateLastUsed(new Date());
            questionMetadataRepository.save(meta);
        }

        ch.since_start("searchQuestionsAdvanced - completed with " + loadedQuestions.size() + " questions");

        return loadedQuestions;
    }

    private List<QuestionMetadataEntity> filterQuestionMetas(
            List<QuestionMetadataEntity> given,
            double scaledComplexity,
            long targetConceptsBitmask,
            long unwantedConceptsBitmask,
            long unwantedLawsBitmask,
            long unwantedViolationsBitmask,
            int limit) {
        if (given.size() <= limit)
            return given;

        List<QuestionMetadataEntity> ranking1 = given.stream()
                .sorted(Comparator.comparingDouble(q -> q.complexityAbsDiff(scaledComplexity)))
                .toList();

        List<QuestionMetadataEntity> ranking3 = given.stream()
                .sorted(Comparator.comparingInt(
                        q -> bitCount(q.getConceptBits() & unwantedConceptsBitmask)
                        + bitCount(q.getLawBits() & unwantedLawsBitmask)
                        + bitCount(q.getViolationBits() & unwantedViolationsBitmask)
                        - bitCount(q.getConceptBits() & targetConceptsBitmask) * 3
                ))
                .toList();


        // want more diversity in question ?? count control values -> take more with '1' not with '0'
//        List<QuestionMetadataEntity> ranking6 = given.stream()
//                .sorted(Comparator.comparingInt(q -> -q.getDistinctErrorsCount()))
//                .collect(Collectors.toList());

        List<QuestionMetadataEntity> finalRanking = given.stream()
                .sorted(Comparator.comparingInt(q -> (
                        ranking1.indexOf(q) +
                        /*ranking2.indexOf(q) +*/
                        ranking3.indexOf(q)
                        /*ranking6.indexOf(q)*/
                        )))
                .collect(Collectors.toList());

        return finalRanking.subList(0, limit);
    }

    private List<Question> loadQuestions(Domain domain, Collection<QuestionMetadataEntity> metas) {
        List<Question> list = new ArrayList<>();
        for (QuestionMetadataEntity meta : metas) {
            Question question = loadQuestion(domain, meta);
            if (question != null) {
                list.add(question);
            }
        }
        return list;
    }

    private @Nullable Question loadQuestion(Domain domain, @NotNull QuestionMetadataEntity qMeta) {
        var path = qMeta.getQDataGraph();
        var fileService = fileServices.get(domain.getDomainId());
        try (InputStream stream = fileService.getFileStream(path)) {
            if (stream != null) {
                var deserialized = SerializableQuestion.deserialize(stream);
                return createQuestion(domain, qMeta, deserialized);
            } else {
                log.warn("File NOT found by storage: {}", path);
            }
        } catch (IOException | NullPointerException | IllegalStateException e) {
            log.error("Error loading question with path [{}] - {}", path, e.getMessage(), e);
        } finally {
            try {
                fileService.closeConnections();
            } catch (FileSystemException e) {
                log.error("Error closing connection - {}", e.getMessage(), e);
            }
        }
        return null;
    }
    
    private Question createQuestion(Domain domain, @NotNull QuestionMetadataEntity qMeta, SerializableQuestion question) {
        if (question == null) {
            return null;
        }
        if (!qMeta.getDomainShortname().equals(domain.getShortName())) {
            throw new RuntimeException("Domain mismatch: " + qMeta.getDomainShortname() + " vs " + domain.getShortName());
        }

        var questionData = question.getQuestionData();
        var questionEntity = new QuestionEntity();
        questionEntity.setQuestionType(questionData.getQuestionType());
        questionEntity.setQuestionText(questionData.getQuestionText());
        questionEntity.setQuestionName(questionData.getQuestionName());
        questionEntity.setQuestionDomainType(questionData.getQuestionDomainType());
        questionEntity.setMetadata(qMeta);
        questionEntity.setOptions(questionData.getOptions());
        questionEntity.setAnswerObjects(questionData.getAnswerObjects()
                .stream()
                .map(a -> AnswerObjectEntity.builder()
                        .answerId(a.getAnswerId())
                        .hyperText(a.getHyperText())
                        .domainInfo(a.getDomainInfo())
                        .isRightCol(a.isRightCol())
                        .concept(a.getConcept())
                        .build())
                .toList());
        questionEntity.setInteractions(new ArrayList<>());
        questionEntity.setDomainEntity(domain.getDomainEntity());
        questionEntity.setStatementFacts(questionData.getStatementFacts()
                .stream()
                .map(s -> new BackendFactEntity(
                        s.getSubjectType(),
                        s.getSubject(), 
                        s.getVerb(), 
                        s.getObjectType(), 
                        s.getObject()))
                .toList());
        questionEntity.setSolutionFacts(new ArrayList<>());

        var result = new Question(questionEntity, domain);
        result.setConcepts(new ArrayList<>(question.getConcepts()));
        result.setTags(new HashSet<>(question.getTags()));
        result.setNegativeLaws(new ArrayList<>(question.getNegativeLaws()));        
        return result;
    }

    /**
     * Find and return row of question or question template with given name from 'question_meta_draft' table
     */
    public QuestionMetadataEntity findQuestionByName(String questionName) {
        val repo = this.questionMetadataRepository;
        if (repo == null)
            return null;

        val found = repo.findByName(questionName);
        if (found.isEmpty())
            return null;

        return found.get(0);
    }

    @NotNull
    public QuestionMetadataEntity saveMetadataDraftEntity(QuestionMetadataEntity meta) {
        if (!meta.isDraft())
            meta.setDraft(true);
        return questionMetadataRepository.save(meta);
    }

    @NotNull
    public QuestionMetadataEntity saveMetadataEntity(QuestionMetadataEntity meta) {
        if (meta.isDraft())
            meta.setDraft(false);
        return questionMetadataRepository.save(meta);
    }

    /**
     * Create metadata representing empty Question, but not overwrite existing data if recreate == false.
     *
     * @param questionName unique identifier-like name of question
     * @return true on success
     */
    public QuestionMetadataEntity createQuestion(Domain domain, String questionName, String questionTemplateName, boolean recreate) {

        // find template metadata
        QuestionMetadataEntity templateMeta = findQuestionByName(questionTemplateName);


        QuestionMetadataEntity meta;
        if (!recreate) {
            meta = findQuestionByName(questionName);
            if (meta != null) {
                if (templateMeta != null) {
                    meta.setTemplateId(templateMeta.getId());
                }
                return meta;
            }
        }
        QuestionMetadataEntity.QuestionMetadataEntityBuilder builder;
        int templateId;
        if (templateMeta != null) {
            // get builder to copy data from template
            builder = templateMeta.toBuilder()
                    .id(null); // reset id
            templateId = templateMeta.getId();
        } else {
            builder = QuestionMetadataEntity.builder()
                    .domainShortname(Optional.ofNullable(domain).map(Domain::getShortName).orElse(""));
            templateId = -1;
        }

        // проинициализировать метаданные вопроса, далее сохранить в БД
        meta = builder.name(questionName)
                    .templateId(templateId)
                    .isDraft(true)
                    .stage(STAGE_QUESTION)
                    .version(GENERATOR_VERSION)
                    .build();
        meta = saveMetadataDraftEntity(meta);

        return meta;
    }

    public String saveQuestionData(String domainId, String basePath, String questionName, SerializableQuestion question) throws IOException {
        var rawQuestionPath = Path.of(basePath, questionName + ".json");
        return saveQuestionDataImpl(domainId, rawQuestionPath.toString(), question);
    }

    public String saveQuestionData(String domainId, String questionName, SerializableQuestion question) throws IOException {
        return saveQuestionDataImpl(domainId, questionName + ".json", question);
    }

    private String saveQuestionDataImpl(String domainId, String rawQuestionPath, SerializableQuestion question) throws IOException {
        var fileService = fileServices.get(domainId);
        var questionPath = fileService.prepareNameForFile(rawQuestionPath, false);
        try (OutputStream stream = fileService.openForWrite(questionPath)) {
            assert stream != null;
            question.serializeToStream(stream);
            return questionPath;
        } finally {
            try {
                fileService.closeConnections();
            } catch (FileSystemException e) {
                log.error("Error closing file service connection: {}", e.getMessage(), e);
            }
        }
    }

    public static int getTooFewQuestionsForQR(int qrLogId) {
        if (qrLogId != 0) {
            // TODO: get the exercise the QR made from and fetch its expected number of students
        }
        return 500;
    }

    public static int getQrEnoughQuestions(int qrLogId) {
        if (qrLogId != 0) {
            // TODO: get the exercise the QR made from and fetch its expected number of students
        }
        return 500;
    }
}
