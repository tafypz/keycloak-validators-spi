package io.tafypz.keycloak.validators;

import com.google.auto.service.AutoService;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AutoService(ValidatorFactory.class)
public class AdultDateOfBirthValidator extends AbstractStringValidator implements ConfiguredProvider {
  public static final String ID = "adult-dob-validator";
  public static final String CONFIG_MIN_AGE = "minAge";
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
    int minAge = config.getIntOrDefault(CONFIG_MIN_AGE, 18);
    LocalDate cutoff = LocalDate.now().minusYears(minAge);
    if (dob.isAfter(cutoff)) {
      context.addError(new ValidationError(ID, inputHint, ERROR_TOO_YOUNG, minAge));
    }
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getHelpText() {
    return "Validates that date of birth indicates the user is at least N years old.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    List<ProviderConfigProperty> props = new ArrayList<>();

    ProviderConfigProperty minAge = new ProviderConfigProperty();
    minAge.setName(CONFIG_MIN_AGE);
    minAge.setLabel("Minimum age");
    minAge.setHelpText("User must be at least this many years old.");
    minAge.setType(ProviderConfigProperty.STRING_TYPE);
    minAge.setDefaultValue("18");
    props.add(minAge);
    return props;
  }
}
