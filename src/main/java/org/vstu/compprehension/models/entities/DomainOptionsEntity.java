package org.vstu.compprehension.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
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
    private String StorageSPARQLEndpointUrl;
    private String StorageUploadFilesBaseUrl;
    private String StorageDownloadFilesBaseUrl = null;
    private Integer StorageDummyDirsForNewFile = 1;
}
