package org.critiqal.infra.auth.metadata;

import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.auth.session.repository.AuthSessionRepository;
import org.critiqal.domain.shared.exception.ConflictException;

@ApplicationScoped
public class DeviceGuard {

    private final AuthSessionRepository authSessionRepository;

    public DeviceGuard(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
    }

    public void assertCanRegister(String deviceIdHash) {
        if (deviceIdHash == null) return;
        if (authSessionRepository.existsByDeviceIdHash(deviceIdHash)) {
            throw new ConflictException("device_already_used");
        }
    }
}
