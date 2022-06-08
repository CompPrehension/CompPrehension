package org.vstu.compprehension.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class DomainOptionsEntity {
    @Builder.Default
    private String StorageSPARQLEndpointUrl = null;
    @Builder.Default
    private String StorageUploadFilesBaseUrl = null;
    @Builder.Default
    private String StorageDownloadFilesBaseUrl = null;
    @Builder.Default
    private String QuestionsGraphPath = null;
    @Builder.Default
    private Integer StorageDummyDirsForNewFile = 1;
}
