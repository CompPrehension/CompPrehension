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

    /** Слой приложения. В core пакет назван с большой буквы, в server и bkt — с маленькой. */
    public static final String[] SERVICES = {"..Service..", "..service.."};

    /** Слой доступа к данным. */
    public static final String REPOSITORIES = "..models.repository..";

    /** JPA-сущности. */
    public static final String ENTITIES = "..models.entities..";

    /** Модели данных, которыми сервисы говорят с бизнес-логикой. */
    public static final String DATA_MODELS = "..models.data..";

    /** Перенос между сущностями и моделями данных. */
    public static final String MAPPERS = "..Service.mapping..";

    /** Типы, которые уезжают наружу по HTTP. */
    public static final String DTO = "..dto..";

    /** Стратегии подбора вопросов: и контракт в core, и реализации в отдельных модулях. */
    public static final String[] STRATEGIES = {"..businesslogic.strategies..", "..compprehension.strategies.."};

    /** Доменная логика и модель — то, что в перспективе должно жить без Spring. */
    public static final String[] BUSINESS_LOGIC = {"..models..", "..Service..", "..strategies.."};
}
