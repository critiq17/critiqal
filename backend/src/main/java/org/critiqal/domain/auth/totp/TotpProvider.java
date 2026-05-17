package org.critiqal.domain.auth.totp;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@ApplicationScoped
public class TotpProvider {

    private final String issuer;
    private final DefaultSecretGenerator secretGen;
    private final CodeVerifier verifier;
    private final ZxingPngQrGenerator qrGen;

    public TotpProvider(
            @ConfigProperty(name = "auth.totp.issuer", defaultValue = "Critiqal") String issuer) {
        this.issuer = issuer;
        this.secretGen = new DefaultSecretGenerator(32);

        var codeGen = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        this.verifier = new DefaultCodeVerifier(codeGen, new SystemTimeProvider());
        this.qrGen = new ZxingPngQrGenerator();
    }

    public String generateSecret() {
        return secretGen.generate();
    }

    /** Verified with window +-1 period */
    public boolean verify(String secret, String code) {
        if (code == null || code.isBlank()) return false;
        ((DefaultCodeVerifier) verifier).setAllowedTimePeriodDiscrepancy(1);
        return verifier.isValidCode(secret, code);
    }

    public String generateQrCodeUri(String secret, String accountName) {
        var data = new QrData.Builder()
                .label(accountName)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            return getDataUriForImage(qrGen.generate(data), qrGen.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }
}
