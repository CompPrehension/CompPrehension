package org.vstu.compprehension.dto;

import org.vstu.compprehension.models.entities.EnumData.TemplateLocation;

public record TextTemplateDto(
    TemplateLocation templateLocation,
    String subLocationName,
    int id,
    String ownerName,
    String locCode,
    String propertyName,
    String value
) {


}
