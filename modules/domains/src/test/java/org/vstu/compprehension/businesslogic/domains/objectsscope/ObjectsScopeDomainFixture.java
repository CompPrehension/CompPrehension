package org.vstu.compprehension.businesslogic.domains.objectsscope;

import org.vstu.compprehension.businesslogic.domains.DomainFixtures;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.BundleLocalizationService;
import org.vstu.compprehension.businesslogic.domains.DomainFixtures.SeededRandomProvider;
import org.vstu.compprehension.businesslogic.domains.ObjectsScopeDTDomain;
import org.vstu.compprehension.data.question.AnswerObjectData;
import org.vstu.compprehension.data.question.QuestionData;
import org.vstu.compprehension.enums.Language;

import java.util.List;

final class ObjectsScopeDomainFixture {

    /** Время жизни объектов: глобальная, но не статическая переменная b. */
    static final String LIFE_TIME_GLOBAL_VARIABLE = "life_time_global_variable";

    private static final String BANK_LOCATION = "org/vstu/compprehension/businesslogic/domains/objectsscope/";

    private static final class Holder {
        private static final ObjectsScopeDTDomain DOMAIN = new ObjectsScopeDTDomain(
                new BundleLocalizationService("domains/objects-scope"), new SeededRandomProvider(), null);
    }

    private ObjectsScopeDomainFixture() {
    }

    static ObjectsScopeDTDomain domain() {
        return Holder.DOMAIN;
    }

    static QuestionData bankQuestion(String file) {
        return QuestionData.of(domain().makeQuestion(DomainFixtures.bankRecord(BANK_LOCATION + file + ".json"),
                List.of(), Language.RUSSIAN).getContent());
    }

    static AnswerObjectData answer(QuestionData question, String domainInfo) {
        return question.getContent().getAnswerObjects().stream()
                .filter(answer -> domainInfo.equals(answer.getDomainInfo()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Нет ответа " + domainInfo));
    }
}
