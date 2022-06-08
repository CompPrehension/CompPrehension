package org.vstu.compprehension.models.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;


@Data
@AllArgsConstructor @NoArgsConstructor
@SuperBuilder
@Jacksonized
public class DomainOptionsEntity {
    @JsonProperty("StorageSPARQLEndpointUrl")
    @Builder.Default
    private String StorageSPARQLEndpointUrl = null;

    @JsonProperty("StorageUploadFilesBaseUrl")
    @Builder.Default
    private String StorageUploadFilesBaseUrl = null;

    @JsonProperty("StorageDownloadFilesBaseUrl")
    @Builder.Default
    private String StorageDownloadFilesBaseUrl = null;

    @JsonProperty("QuestionsGraphPath")
    @Builder.Default
    private String QuestionsGraphPath = null;

    @JsonProperty("StorageDummyDirsForNewFile")
    @Builder.Default
    private Integer StorageDummyDirsForNewFile = 1;
}
