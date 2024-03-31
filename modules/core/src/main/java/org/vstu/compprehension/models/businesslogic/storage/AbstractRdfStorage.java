package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.vfs2.FileSystemException;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.Checkpointer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Long.bitCount;

@Log4j2
public class AbstractRdfStorage {
    public static int STAGE_TEMPLATE = 1;
    public static int STAGE_QUESTION = 2;
    public static int STAGE_READY = 3; // in this stage the generated question may be moved to the production bank
    public static int STAGE_EXPORTED = 4;
    public static int GENERATOR_VERSION = 10;

    private final HashMap<String, RemoteFileService> fileServices;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionMetadataManager questionMetadataManager;

    public AbstractRdfStorage(
            Collection<Domain> domains,
            QuestionMetadataRepository questionMetadataRepository,
            QuestionMetadataManager questionMetadataManager) throws URISyntaxException {
        this.fileServices = new HashMap<>();
        for (var domain : domains) {
            var fs = new RemoteFileService(domain.getOptions().getStorageUploadFilesBaseUrl(),
                    Optional.ofNullable(domain.getOptions().getStorageDownloadFilesBaseUrl()).orElse(domain.getOptions().getStorageUploadFilesBaseUrl()));

            this.fileServices.put(domain.getShortName(), fs);
            this.fileServices.put(domain.getDomainId(), fs);
        }

        this.questionMetadataRepository = questionMetadataRepository;
        this.questionMetadataManager = questionMetadataManager;
    }

    public AbstractRdfStorage(
            Domain domain,
            QuestionMetadataRepository questionMetadataRepository,
            QuestionMetadataManager questionMetadataManager) throws URISyntaxException {
        this(List.of(domain), questionMetadataRepository, questionMetadataManager);
    }

    public AbstractRdfStorage(
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
            QuestionMetadataEntity meta = questionEntity.getOptions().getMetadata();
            if (meta != null)
                result.add(meta);
        }
        return result;
    }

    public int countQuestions(QuestionRequest qr) {
        var minComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMin();
        var maxComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMax();

        var bankSearchRequest = qr.toBankSearchRequest(minComplexity, maxComplexity);
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

        var minComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMin();
        var maxComplexity = questionMetadataManager.getComplexityStats(qr.getDomainShortname()).getMax();
        var bankSearchRequest = qr.toBankSearchRequest(minComplexity, maxComplexity);

        QuestionMetadataManager metaMgr = this.questionMetadataManager;
        int nQuestionsInAttempt = Optional.ofNullable(bankSearchRequest.getDeniedQuestionNames()).map(List::size).orElse(0);
        int queryLimit = limit + nQuestionsInAttempt;
        int hardLimit = 25;

        double complexity = bankSearchRequest.getComplexity();

        long targetConceptsBitmask = bankSearchRequest.targetConceptsBitmask();
        long deniedConceptsBitmask = bankSearchRequest.deniedConceptsBitmask();
        long unwantedConceptsBitmask = findLastNQuestionsMeta(bankSearchRequest, attempt, 4).stream()
                .mapToLong(QuestionMetadataEntity::getConceptBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetConceptsBitmask &= ~deniedConceptsBitmask;
        long targetConceptsBitmaskInPlan = bankSearchRequest.targetConceptsBitmask();

        // use laws, for e.g. Expr domain
        long targetLawsBitmask = bankSearchRequest.targetLawsBitmask();
        long deniedLawsBitmask = bankSearchRequest.deniedLawsBitmask();
        long unwantedLawsBitmask = findLastNQuestionsMeta(bankSearchRequest, attempt, 4).stream()
                .mapToLong(QuestionMetadataEntity::getLawBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetLawsBitmask &= ~deniedLawsBitmask;
        long targetViolationsBitmaskInPlan = bankSearchRequest.targetLawsBitmask();

        // use violations from all questions is exercise attempt
        long unwantedViolationsBitmask = attempt.getQuestions().stream()
                .map(qd -> qd.getOptions().getMetadata())
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
            val meta = loadedQuestions.get(0).getQuestionData().getOptions().getMetadata();
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


    private Question loadQuestion(Domain domain, @NotNull QuestionMetadataEntity qMeta) {
        Question q = loadQuestion(domain, qMeta.getQDataGraph());
        if (q != null) {
            if (q.getQuestionData().getOptions() == null) {
                q.getQuestionData().setOptions(new QuestionOptionsEntity());
            }
            q.getQuestionData().getOptions().setTemplateId(qMeta.getTemplateId());
            q.getQuestionData().getOptions().setQuestionMetaId(qMeta.getId());
            q.getQuestionData().getOptions().setMetadata(qMeta);
            q.setMetadata(qMeta);
            // future todo: reflect any set data in Domain.makeQuestionCopy() as well
        }
        return q;
    }

    private Question loadQuestion(Domain domain, String path) {
        Question q = null;
        var fileService = fileServices.get(domain.getDomainId());
        try (InputStream stream = fileService.getFileStream(path)) {
            if (stream != null) {
                q = domain.parseQuestionTemplate(stream);
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
        return q;
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

    public String saveQuestionData(String domainId, String basePath, String questionName, String data) throws IOException {
        var rawQuestionPath = Path.of(basePath, questionName + ".json");
        return saveQuestionDataImpl(domainId, rawQuestionPath.toString(), data);
    }

    public String saveQuestionData(String domainId,  String questionName, String data) throws IOException {
        return saveQuestionDataImpl(domainId, questionName + ".json", data);
    }

    private String saveQuestionDataImpl(String domainId, String rawQuestionPath, String data) throws IOException {
        var fileService = fileServices.get(domainId);
        var questionPath = fileService.prepareNameForFile(rawQuestionPath, false);
        try (OutputStream stream = fileService.openForWrite(questionPath)) {
            assert stream != null;
            stream.write(data.getBytes(StandardCharsets.UTF_8));
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
