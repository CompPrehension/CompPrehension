package org.vstu.compprehension.architecture;

import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.LocationProvider;

import java.util.Set;

/** Отдаёт ArchUnit классы только модуля {@code core-api}. */
public class CoreApiClassesLocationProvider implements LocationProvider {

    @Override
    public Set<Location> get(Class<?> testClass) {
        return Set.of(ProjectClassesLocationProvider.moduleClasses("core-api"));
    }
}
