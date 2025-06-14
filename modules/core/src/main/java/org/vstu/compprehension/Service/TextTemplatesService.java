package org.vstu.compprehension.Service;

import its.model.DomainSolvingModel;
import its.model.definition.DomainModel;
import its.model.definition.MetaOwner;
import its.model.nodes.DecisionTreeElement;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.vstu.compprehension.dto.TextTemplateDto;
import org.vstu.compprehension.models.businesslogic.domains.DecisionTreeReasoningDomain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.entities.EnumData.TemplateLocation;
import org.vstu.compprehension.models.entities.TextTemplateEditEntity;
import org.vstu.compprehension.models.repository.TextTemplateEditRepository;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


@Log4j2
@Service
public class TextTemplatesService {
    private static final String ID_METADATA_PROPERTY = "TEMPLATING_ID";

    @Lazy
    private final DomainFactory domainFactory;
    private final TextTemplateEditRepository textTemplateEditRepository;

    public TextTemplatesService(DomainFactory domainFactory, TextTemplateEditRepository textTemplateEditRepository) {
        this.domainFactory = domainFactory;
        this.textTemplateEditRepository = textTemplateEditRepository;

        try {
            domainFactory.getDomainIds().stream()
                    .map(domainFactory::getDomain)
                    .filter(domain -> domain instanceof DecisionTreeReasoningDomain)
                    .map(domain -> (DecisionTreeReasoningDomain) domain)
                    .filter(domain -> domain.getDomainSolvingModels() != null && !domain.getDomainSolvingModels().isEmpty())
                    .forEach(this::init);
        } catch (RuntimeException e) {
            log.warn("TextTemplatesService failed to initialize");
        }

    }


    private Optional<Integer> parseInt(Object value) {
        if (value instanceof Integer integer) {
            return Optional.of(integer);
        }
        if (value instanceof String string) {
            try {
                return Optional.of(Integer.parseInt(string));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private void collectTreeElementsById(
        DecisionTreeElement currentElement,
        Map<Integer, MetaOwner> resultingMap,
        List<MetaOwner> unmarkedElements
    ) {
        collectMetaOwnerById(currentElement, resultingMap, unmarkedElements);
        currentElement.getLinkedElements().forEach(linkedElement ->
            collectTreeElementsById(linkedElement, resultingMap, unmarkedElements)
        );
    }

    private void collectDomainModelElementsById(
        DomainModel domainModel,
        Map<Integer, MetaOwner> resultingMap,
        List<MetaOwner> unmarkedElements
    ) {
        domainModel.getEnums().forEach(enumDef -> {
            collectMetaOwnerById(enumDef, resultingMap, unmarkedElements);
            enumDef.getValues().forEach(enumValueDef ->
                collectMetaOwnerById(enumValueDef, resultingMap, unmarkedElements)
            );
        });
        domainModel.getClasses().forEach(classDef -> {
            collectMetaOwnerById(classDef, resultingMap, unmarkedElements);
            classDef.getDeclaredProperties().forEach(propertyDef ->
                collectMetaOwnerById(propertyDef, resultingMap, unmarkedElements)
            );
            classDef.getDeclaredRelationships().forEach(relationshipDef ->
                collectMetaOwnerById(relationshipDef, resultingMap, unmarkedElements)
            );
        });
        domainModel.getObjects().forEach(objectDef ->
            collectMetaOwnerById(objectDef, resultingMap, unmarkedElements)
        );
    }

    private void collectMetaOwnerById(
        MetaOwner currentElement,
        Map<Integer, MetaOwner> resultingMap,
        List<MetaOwner> unmarkedElements
    ) {
        if (currentElement.getMetadata().isEmpty()) {
            return;
        }
        parseInt(currentElement.getMetadata().get(ID_METADATA_PROPERTY))
            .filter(id -> !resultingMap.containsKey(id))
            .ifPresentOrElse(
                id -> resultingMap.put(id, currentElement),
                () -> unmarkedElements.add(currentElement)
            );
    }

    private record MetaOwnerKey(
        TemplateLocation location,
        String subLocationName,
        Integer id
    ) {
    }

    private Map<MetaOwnerKey, MetaOwner> createMetaOwnerMap(
        TemplateLocation location,
        String subLocationName,
        BiConsumer<Map<Integer, MetaOwner>, List<MetaOwner>> filler
    ) {
        Map<Integer, MetaOwner> resultingMap = new HashMap<>();
        List<MetaOwner> unmarkedElements = new ArrayList<>();
        filler.accept(resultingMap, unmarkedElements);

        if (!unmarkedElements.isEmpty()) {
            log.warn("Not all templating IDs (set with '" + ID_METADATA_PROPERTY + "' metadata property) " +
                "were provided for " + location + " '" + subLocationName + "'. " +
                "The model was probably edited manually. You should probably save it with the new IDs present.");
        }

        int newId = resultingMap.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
        for (MetaOwner element : unmarkedElements) {
            newId++;
            element.getMetadata().add(ID_METADATA_PROPERTY, newId);
            resultingMap.put(newId, element);
        }

        return resultingMap.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> new MetaOwnerKey(location, subLocationName, entry.getKey()),
                Map.Entry::getValue
            ));
    }

    private Map<MetaOwnerKey, MetaOwner> createTreeElementMap(DomainSolvingModel domainSolvingModel) {
        Map<MetaOwnerKey, MetaOwner> resultingMap = new HashMap<>();
        domainSolvingModel.getDecisionTrees().forEach((name, decisionTree) -> {
            resultingMap.putAll(createMetaOwnerMap(
                TemplateLocation.DECISION_TREE,
                name,
                (idMap, unmarkedElements) -> collectTreeElementsById(decisionTree, idMap, unmarkedElements)
            ));
        });
        return resultingMap;
    }

    private Map<MetaOwnerKey, MetaOwner> createDomainModelElementMap(DomainSolvingModel domainSolvingModel) {
        Map<MetaOwnerKey, MetaOwner> resultingMap = new HashMap<>();
        resultingMap.putAll(createMetaOwnerMap(
            TemplateLocation.DOMAIN_MODEL,
            null,
            (idMap, unmarkedElements) -> collectDomainModelElementsById(
                domainSolvingModel.getDomainModel(), idMap, unmarkedElements)
        ));
        domainSolvingModel.getTagsData().forEach((tagName, tagModel) ->
            resultingMap.putAll(createMetaOwnerMap(
                TemplateLocation.DOMAIN_MODEL,
                tagName,
                (idMap, unmarkedElements) -> collectDomainModelElementsById(tagModel, idMap, unmarkedElements)
            ))
        );
        return resultingMap;
    }

    private Map<MetaOwnerKey, MetaOwner> createMetaOwnerMap(DomainSolvingModel domainSolvingModel) {
        Map<MetaOwnerKey, MetaOwner> resultingMap = new HashMap<>();
        resultingMap.putAll(createDomainModelElementMap(domainSolvingModel));
        resultingMap.putAll(createTreeElementMap(domainSolvingModel));

        return resultingMap.entrySet().stream()
            //костыль
            .filter(entry ->
                !(entry.getKey().location == TemplateLocation.DECISION_TREE
                    && "no_strict".equals(entry.getKey().subLocationName))
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public List<TextTemplateDto> search(String searchString, String domainId) {
        DecisionTreeReasoningDomain domain = (DecisionTreeReasoningDomain) domainFactory.getDomain(domainId);        
        String searchStringLowerCase = searchString.toLowerCase();
        List<TextTemplateDto> results = new ArrayList<>();

        for(var domainSolvingModel : domain.getDomainSolvingModels()) {
            Map<MetaOwnerKey, MetaOwner> metaOwnerMap = createMetaOwnerMap(domainSolvingModel);            
            metaOwnerMap.forEach((key, metaOwner) -> {
                metaOwner.getMetadata().getEntries().forEach(metadataPropertyValue -> {
                    if (!(metadataPropertyValue.getValue() instanceof String)) {
                        return;
                    }
                    if (ID_METADATA_PROPERTY.equals(metadataPropertyValue.getPropertyName())) {
                        return;
                    }
                    String valueString = metadataPropertyValue.getValue().toString();
                    if (valueString.toLowerCase().contains(searchStringLowerCase)) {
                        results.add(new TextTemplateDto(
                                key.location,
                                key.subLocationName,
                                key.id,
                                metaOwner.toString(),
                                metadataPropertyValue.getLocCode(),
                                metadataPropertyValue.getPropertyName(),
                                valueString
                        ));
                    }
                });
            });
        }

        return results;
    }

    public void updateAndSave(List<TextTemplateDto> updatedTemplates, String domainId) {
        DecisionTreeReasoningDomain domain = (DecisionTreeReasoningDomain) domainFactory.getDomain(domainId);
        List<TextTemplateDto> updated = update(updatedTemplates, domain);

        textTemplateEditRepository.saveAll(
            updated.stream()
                .map(dto -> new TextTemplateEditEntity(
                    new TextTemplateEditEntity.TextTemplateEditKey(
                        domainId,
                        dto.templateLocation(),
                        dto.subLocationName(),
                        dto.id(),
                        dto.locCode(),
                        dto.propertyName()
                    ),
                    dto.value(),
                    domain.getDomainEntity()
                ))
                .collect(Collectors.toList())
        );
    }

    private List<TextTemplateDto> update(
        List<TextTemplateDto> updatedTemplates,
        DecisionTreeReasoningDomain domain
    ) {
        List<TextTemplateDto> updated = new ArrayList<>();
        for (var domainSolvingModel : domain.getDomainSolvingModels() ) {
            Map<MetaOwnerKey, MetaOwner> metaOwnerMap = createMetaOwnerMap(domainSolvingModel);            
            for (TextTemplateDto textTemplateDto : updatedTemplates) {
                MetaOwner metaOwner = metaOwnerMap.get(new MetaOwnerKey(
                        textTemplateDto.templateLocation(),
                        textTemplateDto.subLocationName(),
                        textTemplateDto.id()
                ));
                String locCode = textTemplateDto.locCode();
                String property = textTemplateDto.propertyName();
                String value = textTemplateDto.value();

                if (!Objects.equals(metaOwner.getMetadata().get(locCode, property), value)) {
                    metaOwner.getMetadata().add(locCode, property, value);
                    updated.add(textTemplateDto);
                }
            }
        }

        return updated;
    }

    public void init(DecisionTreeReasoningDomain domain) {
        String domainId = domain.getDomainId();
        update(
            textTemplateEditRepository.findAllByKey_DomainName(domainId).stream()
                .map(entity -> new TextTemplateDto(
                    entity.getKey().getTemplateLocation(),
                    entity.getKey().getSubLocationName(),
                    entity.getKey().getTemplateId(),
                    "",
                    entity.getKey().getLocCode(),
                    entity.getKey().getPropertyName(),
                    entity.getValue()
                ))
                .collect(Collectors.toList()),
            domain
        );
        //uncomment to update the domainSolvingModel
//        domain.getDomainSolvingModelSourceDirectory().ifPresent(directory ->
//            DomainSolvingModel.writeModelToDirectory(
//                domain.getDomainSolvingModel(),
//                directory.getAbsolutePath()
//            )
//        );
    }

}
