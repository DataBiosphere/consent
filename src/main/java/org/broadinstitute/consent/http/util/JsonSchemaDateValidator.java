package org.broadinstitute.consent.http.util;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;

/**
 * Custom parser for "date" types.
 * See <a href="https://github.com/everit-org/json-schema">https://github.com/everit-org/json-schema</a>
 * for more information on library validation. The docs
 * suggest that "date" is supported in draft 2020-12, but in practice, any
 * string validates as date format. See <a href="https://json-schema.org/understanding-json-schema/reference/string.html#dates-and-times">Json Schema</a>
 * documentation for supported formats.
 */
public class JsonSchemaDateValidator implements FormatValidator {

    @Override
    public String formatName() {
        return "date";
    }

    @Override
    public Optional<String> validate(String subject) {
        if (subject.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return Optional.empty();
        } else {
            return Optional.of(String.format("The provided value [%s] is not in date format: [YYYY-MM-DD]", subject));
        }
    }
}
