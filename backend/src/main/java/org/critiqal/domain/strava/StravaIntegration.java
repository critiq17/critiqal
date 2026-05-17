package org.critiqal.domain.strava;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.critiqal.domain.shared.uuid.UuidGeneration;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "strava_integrations")
public class StravaIntegration extends PanacheEntityBase {

    @Id
    @Column(columnDefinition = "uuid")
    public UUID id = UuidGeneration.generate();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "athlete_id", nullable = false, unique = true)
    public Long athleteId;

    @Column(name = "athlete_username")
    public String athleteUsername;

    @Column(name = "athlete_firstname")
    public String athleteFirstname;

    @Column(name = "athlete_lastname")
    public String athleteLastname;

    @Column(name = "athlete_city")
    public String athleteCity;

    @Column(name = "athlete_avatar_url")
    public String athleteAvatarUrl;

    @Column(name = "access_token", nullable = false, length = 512)
    public String accessToken;

    @Column(name = "refresh_token", nullable = false, length = 512)
    public String refreshToken;

    @Column(name = "expires_at", nullable = false, length = 512)
    public Long expiresAt;

    @Column(name = "connected_at", updatable = false)
    public Instant connectedAt = Instant.now();

    public boolean isTokenExpired() {
        return Instant.now().getEpochSecond() >= expiresAt - 60;
    }
}
