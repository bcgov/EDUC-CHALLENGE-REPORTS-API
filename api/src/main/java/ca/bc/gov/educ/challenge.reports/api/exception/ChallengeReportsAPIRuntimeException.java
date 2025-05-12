package ca.bc.gov.educ.challenge.reports.api.exception;

public class ChallengeReportsAPIRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 5241655513745148898L;

  public ChallengeReportsAPIRuntimeException(String message) {
		super(message);
	}

  public ChallengeReportsAPIRuntimeException(Throwable exception) {
    super(exception);
  }

}
