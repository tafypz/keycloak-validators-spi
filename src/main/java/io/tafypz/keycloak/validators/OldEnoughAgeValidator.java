package io.tafypz.keycloak.validators;

import com.google.auto.service.AutoService;
import org.keycloak.validate.*;

import java.time.LocalDate;

@AutoService(ValidatorFactory.class)
public class OldEnoughAgeValidator extends AbstractStringValidator {
  public static final String ID = "exclusion-age-validator";
  public static final String ERROR_TOO_YOUNG = "error-age-too-young";
  public static final String ERROR_INVALID_DATE = "error-invalid-date";

  @Override
  protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
    LocalDate dob;
    try {
      dob = LocalDate.parse(value);
    } catch (Exception e) {
      context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_DATE));
      return;
    }
    int minAge = config.getIntOrDefault("min-age", 18);
    LocalDate cutoff = LocalDate.now().minusYears(minAge);
    if (dob.isAfter(cutoff)) {
      context.addError(new ValidationError(ID, inputHint, ERROR_TOO_YOUNG));
    }
  }

  @Override
  public String getId() {
    return ID;
  }
}
