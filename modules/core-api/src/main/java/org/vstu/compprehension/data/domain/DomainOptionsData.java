package org.vstu.compprehension.data.domain;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@AllArgsConstructor
@SuperBuilder
@Jacksonized
public class DomainOptionsData implements Serializable {
}
