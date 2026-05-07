package org.critiqal.domain.shared.uuid;

import com.fasterxml.uuid.Generators;
import java.util.UUID;

public final class UuidGeneration {
    private UuidGeneration() {}

    // UUID v7 - time-ordered
    public static UUID generate() {
        return Generators.timeBasedEpochGenerator().generate();
    }
}
