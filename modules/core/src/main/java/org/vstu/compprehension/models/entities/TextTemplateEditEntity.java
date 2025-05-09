package org.vstu.compprehension.models.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.vstu.compprehension.models.entities.EnumData.TemplateLocation;

import java.io.Serializable;
import java.util.Objects;


@Getter
@Setter
@Entity
@Table(name = "text_template_edit")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TextTemplateEditEntity {

    @EmbeddedId
    private TextTemplateEditKey key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_name")
    @MapsId("domainName")
    private DomainEntity domainEntity;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    public static class TextTemplateEditKey implements Serializable {
        private String domainName;

        @Column(name = "template_location")
        @Enumerated(EnumType.STRING)
        private TemplateLocation templateLocation;

        @Column(name = "sub_location_name")
        private String subLocationName;

        @Column(name = "template_id")
        private int templateId;

        @Column(name = "loc_code")
        private String locCode;

        @Column(name = "property_name")
        private String propertyName;

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            TextTemplateEditKey that = (TextTemplateEditKey) object;
            return templateId == that.templateId
                && Objects.equals(domainName, that.domainName)
                && templateLocation == that.templateLocation
                && Objects.equals(subLocationName, that.subLocationName)
                && Objects.equals(locCode, that.locCode)
                && Objects.equals(propertyName, that.propertyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(domainName, templateLocation, subLocationName, templateId, locCode, propertyName);
        }
    }
}
