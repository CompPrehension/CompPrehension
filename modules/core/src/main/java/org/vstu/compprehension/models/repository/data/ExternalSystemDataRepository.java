package org.vstu.compprehension.models.repository.data;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.data.EducationResourceData;
import org.vstu.compprehension.models.data.ExternalAccountData;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceTrustStatus;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.entities.external_system.EducationResourceEntity;
import org.vstu.compprehension.models.repository.EducationResourceRepository;
import org.vstu.compprehension.models.repository.ExternalAccountRepository;

import java.util.List;
import java.util.Optional;

/**
 * Внешние системы: сами LMS и привязки к ним учётных записей.
 * <p>
 * Сгруппировано вместе, потому что и то и другое существует ради одного сценария —
 * входа по LTI, где ресурс заводится и тут же получает привязку пользователя. Связь
 * учётной записи наружу не отдаётся вовсе: из неё читают единственное поле, внешний
 * идентификатор, а остальные её поля — это те же самые user и ресурс, которые
 * вызывающий уже держит в руках.
 */
@Repository
@RequiredArgsConstructor
public class ExternalSystemDataRepository {

    private final EducationResourceRepository educationResourceRepository;
    private final ExternalAccountRepository externalAccountRepository;

    /** Образовательный ресурс по адресу и типу; пусто, если он ещё не заведён. */
    @Transactional(readOnly = true)
    public @NotNull Optional<EducationResourceData> findEducationResource(
            @NotNull String url, @NotNull EducationResourceType type) {
        return educationResourceRepository.findByUrlAndType(url, type)
                .map(ExternalSystemDataRepository::toData);
    }

    /** Образовательные ресурсы заданного типа с заданным статусом доверия. */
    @Transactional(readOnly = true)
    public @NotNull List<EducationResourceData> findEducationResources(
            @NotNull EducationResourceType type, @NotNull EducationResourceTrustStatus trustStatus) {
        return educationResourceRepository.findByTypeAndTrustStatus(type, trustStatus).stream()
                .map(ExternalSystemDataRepository::toData)
                .toList();
    }

    /** Все привязки учётных записей к внешней системе. */
    @Transactional(readOnly = true)
    public @NotNull List<ExternalAccountData> findExternalAccounts(long educationResourceId) {
        return externalAccountRepository.findAccountsByEducationResourceId(educationResourceId).stream()
                .map(view -> new ExternalAccountData(
                        Strict.required(view.getUserId(), "userId",
                                "external account of education resource " + educationResourceId),
                        educationResourceId,
                        Strict.required(view.getExternalId(), "externalId",
                                "external account of education resource " + educationResourceId)))
                .toList();
    }

    /**
     * Образовательный ресурс по адресу и типу, заводя его при отсутствии.
     * <p>
     * Вставка идемпотентная ({@code insert ignore}) и после неё идёт чтение: при гонке
     * двух LTI-запусков победит тот, кто вставил, а второй прочитает уже существующую
     * строку. Новый ресурс появляется недоверенным — доверие проставляется вручную.
     *
     * @throws IllegalStateException если строки нет и после вставки
     */
    @Transactional
    public @NotNull EducationResourceData createEducationResourceIfAbsent(
            @NotNull String url, @NotNull EducationResourceType type) {
        educationResourceRepository.createIfAbsent(url, type.name());
        return findEducationResource(url, type)
                .orElseThrow(() -> new IllegalStateException(
                        "Education resource " + type + " " + url + " not found after insert"));
    }

    /** Идентификатор пользователя во внешней системе; пусто, если привязки нет. */
    @Transactional(readOnly = true)
    public @NotNull Optional<String> findExternalAccountId(long userId, long educationResourceId) {
        return externalAccountRepository.findExternalId(userId, educationResourceId);
    }

    /** Завести привязку учётной записи к внешней системе, если её ещё нет. */
    @Transactional
    public void createExternalAccountIfAbsent(long userId, long educationResourceId,
                                              @NotNull String externalId) {
        externalAccountRepository.createIfAbsent(userId, educationResourceId, externalId);
    }

    // ---------------------------------------------------------------- маппинг

    private static @NotNull EducationResourceData toData(@NotNull EducationResourceEntity entity) {
        long id = Strict.required(entity.getId(), "id", "education resource " + entity.getUrl());
        return new EducationResourceData(
                id,
                Strict.required(entity.getUrl(), "url", "education resource " + id),
                Strict.required(entity.getType(), "type", "education resource " + id),
                Strict.required(entity.getTrustStatus(), "trustStatus", "education resource " + id));
    }
}
