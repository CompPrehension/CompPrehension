package org.vstu.compprehension.Service;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.models.data.EducationResourceData;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceTrustStatus;
import org.vstu.compprehension.models.entities.EnumData.EducationResourceType;
import org.vstu.compprehension.models.repository.data.ExternalSystemDataRepository;

import java.util.Optional;

/**
 * Внешние образовательные системы, из которых к нам приходят по LTI.
 * <p>
 * Наружу отдаются идентификаторы: всё, что вызывающие делают с ресурсом, — привязывают
 * к нему учётную запись, курс и роли. Сам ресурс остаётся внутри сервиса, потому что
 * читается у него ровно одно поле сверх идентификатора — статус доверия.
 */
@Service
@RequiredArgsConstructor
public class EducationResourceService {

    private final ExternalSystemDataRepository externalSystems;

    /** Идентификатор ресурса по адресу и типу, если он уже заведён. */
    @Transactional(readOnly = true)
    public @NotNull Optional<Long> findIdByUrlAndType(@NotNull String url, @NotNull EducationResourceType type) {
        return externalSystems.findEducationResource(url, type).map(EducationResourceData::id);
    }

    /**
     * Идентификатор доверенного образовательного ресурса; заводит ресурс при отсутствии.
     * <p>
     * Новый ресурс появляется недоверенным, и до перевода его в
     * {@link EducationResourceTrustStatus#TRUSTED} вручную LTI-привязка и работа с курсами
     * запрещены (approval-gate).
     *
     * @throws SecurityException если ресурс ещё не переведён в доверенные
     */
    @Transactional
    public long getOrCreateTrustedId(@NotNull String url, @NotNull EducationResourceType type) {
        var resource = externalSystems.findEducationResource(url, type)
                .orElseGet(() -> externalSystems.createEducationResourceIfAbsent(url, type));
        if (resource.trustStatus() != EducationResourceTrustStatus.TRUSTED) {
            throw new SecurityException(String.format("EducationResource %s is not trusted", resource.url()));
        }
        return resource.id();
    }
}
