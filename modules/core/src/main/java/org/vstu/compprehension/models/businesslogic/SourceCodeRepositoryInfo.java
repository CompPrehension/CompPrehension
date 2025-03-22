package org.vstu.compprehension.models.businesslogic;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SourceCodeRepositoryInfo {
    public final String name;
    public final String license;
    public final String url;
}
