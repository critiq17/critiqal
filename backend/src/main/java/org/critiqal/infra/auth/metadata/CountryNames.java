package org.critiqal.infra.auth.metadata;

import java.util.Locale;
import java.util.Set;

/** Maps 2-letter ISO country codes to display-friendly English country names. */
public final class CountryNames {

    private static final Set<String> VALID_CODES =
            Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2);

    private CountryNames() {
    }

    public static String resolve(String iso2) {
        if (iso2 == null) return null;
        var code = iso2.trim().toUpperCase(Locale.ROOT);
        if (code.length() != 2 || !VALID_CODES.contains(code)) return null;
        try {
            var name = new Locale("", code).getDisplayCountry(Locale.ENGLISH);
            return name.isBlank() || name.equals(code) ? null : name;
        } catch (Exception e) {
            return null;
        }
    }
}
