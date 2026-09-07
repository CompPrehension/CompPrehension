package org.vstu.compprehension.architecture;

/**
 * Единое место, где записано, какими пакетами выражены слои приложения.
 * <p>
 * Пакеты, а не модули, потому что все maven-модули лежат в одном корневом пакете
 * {@code org.vstu.compprehension} и по имени класса модуль не восстанавливается.
 * Когда слои разъедутся по отдельным модулям, эти константы станут именами модулей.
 */
public final class ArchitecturePackages {
    private ArchitecturePackages() {
    }

    /** Корень всего кода проекта. */
    public static final String ROOT = "org.vstu.compprehension";

    /** Web-слой: Spring MVC контроллеры. */
    public static final String CONTROLLERS = "..controllers..";

    /** Слой приложения. В core пакет назван во множественном числе, в server и bkt — в единственном. */
    public static final String[] SERVICES = {ROOT + ".services..", ROOT + ".service.."};

    /**
     * Слой доступа к данным (сущностные и data-репозитории вместе).
     * <p>
     * Привязано к {@link #ROOT}, а не свободный {@code ..repositories..}: иначе строка
     * "repositories" как непривязанный сегмент рискует зацепить что-то за пределами
     * этого дерева пакетов. Здесь риска нет, но остальные константы того же семейства
     * ({@link #DATA_MODELS}) от этого реально страдали — единообразие ради него самого.
     */
    public static final String REPOSITORIES = ROOT + ".repositories..";

    /**
     * Единственное место, где JPA-сущности превращаются в модели данных.
     * <p>
     * Запрос и маппинг лежат здесь вместе, поэтому маппинг может быть тотальным:
     * форма выборки известна ровно там, где по ней собираются данные.
     */
    public static final String DATA_ACCESS = ROOT + ".repositories.data..";

    /** JPA-сущности. */
    public static final String ENTITIES = ROOT + ".entities..";

    /**
     * Модели данных, которыми сервисы говорят с бизнес-логикой.
     * <p>
     * Обязательно привязано к {@link #ROOT}: свободный {@code ..data..} совпадает не
     * только с {@code data}, но и с сегментом {@code data} внутри {@link #DATA_ACCESS}
     * ({@code repositories.data}) — тогда каждый data-репозиторий ошибочно считается
     * моделью данных и падает на первой же ссылке на сущность.
     */
    public static final String DATA_MODELS = ROOT + ".data..";

    /** Перенос между сущностями и моделями данных. */
    public static final String MAPPERS = ROOT + ".mappers..";

    /** Типы, которые уезжают наружу по HTTP. */
    public static final String DTO = "..dto..";

    /** Стратегии подбора вопросов: и контракт в core, и реализации в отдельных модулях. */
    public static final String[] STRATEGIES = {"..businesslogic.strategies..", ROOT + ".strategies.."};

    /**
     * Доменная логика и модель — то, что в перспективе должно жить без Spring.
     * <p>
     * Только пакеты core: {@code service}/{@code gradepassback} и подобные из server
     * сюда не входят — это интеграционный код, ему и положено знать про RestTemplate.
     */
    public static final String[] BUSINESS_LOGIC = {
            ROOT + ".services..", ROOT + ".mappers..", ROOT + ".businesslogic..",
            ROOT + ".data..", ROOT + ".entities..", ROOT + ".repositories..", ROOT + ".strategies.."};
}
