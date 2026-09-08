package org.vstu.compprehension.architecture;

/** Пакеты, которыми выражены слои приложения. */
public final class ArchitecturePackages {
    private ArchitecturePackages() {
    }

    /** Корень всего кода проекта. */
    public static final String ROOT = "org.vstu.compprehension";

    /** Web-слой: Spring MVC контроллеры. */
    public static final String CONTROLLERS = "..controllers..";

    /** Слой приложения. */
    public static final String[] SERVICES = {ROOT + ".services..", ROOT + ".service.."};

    /** Слой доступа к данным. */
    public static final String REPOSITORIES = ROOT + ".repositories..";

    /** Где сущности превращаются в модели данных. */
    public static final String DATA_ACCESS = ROOT + ".repositories.data..";

    /** JPA-сущности. */
    public static final String ENTITIES = ROOT + ".entities..";

    /** Модели данных, которыми сервисы говорят с бизнес-логикой. */
    public static final String DATA_MODELS = ROOT + ".data..";

    /** Интерфейсы маппинга: {@code Mapper} и {@code UpdateMapper}. */
    public static final String MAPPER_API = ROOT + ".mappers..";

    /** Мапперы между сущностями и моделями данных. */
    public static final String DATA_MAPPERS = ROOT + ".repositories.mappers..";

    /** Мапперы между моделями данных и тем, что уходит наружу. */
    public static final String FRONTEND_MAPPERS = ROOT + ".frontend.mappers..";

    /** Все места, где живёт маппинг. */
    public static final String[] MAPPERS = {MAPPER_API, DATA_MAPPERS, FRONTEND_MAPPERS};

    /** Типы, которые уезжают наружу по HTTP. */
    public static final String DTO = "..dto..";

    /** Стратегии подбора вопросов. */
    public static final String[] STRATEGIES = {"..businesslogic.strategies..", ROOT + ".strategies.."};

    /** Доменная логика и модель. */
    public static final String[] BUSINESS_LOGIC = {
            ROOT + ".services..", ROOT + ".mappers..", ROOT + ".businesslogic..",
            ROOT + ".data..", ROOT + ".entities..", ROOT + ".repositories..", ROOT + ".strategies.."};
}
