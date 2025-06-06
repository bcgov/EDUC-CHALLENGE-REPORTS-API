package ca.bc.gov.educ.challenge.reports.api.exception;

/**
 * InvalidParameterException to provide error details when unexpected parameters are passed to endpoint
 *
 */
public class InvalidParameterException extends RuntimeException {

  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = -2325104800954988680L;

  /**
   * Instantiates a new Invalid parameter exception.
   *
   * @param searchParamsMap the search params map
   */
  public InvalidParameterException(String... searchParamsMap) {
    super(InvalidParameterException.generateMessage(searchParamsMap));
  }

  /**
   * Generate message string.
   *
   * @param searchParams the search params
   * @return the string
   */
  private static String generateMessage(String... searchParams) {
    StringBuilder message = new StringBuilder("Unexpected request parameters provided: ");
    String prefix = "";
    for (String parameter : searchParams) {
      message.append(prefix);
      prefix = ",";
      message.append(parameter);
    }
    return message.toString();
  }
}
