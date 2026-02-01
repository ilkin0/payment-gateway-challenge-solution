package com.checkout.payment.gateway.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostPaymentRequestValidationTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void validRequest_shouldHaveNoViolations() {
    PostPaymentRequest request = createValidRequest();
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void cardNumber_withLessThan14Digits_shouldFail() {
    PostPaymentRequest request = withCardNumber(createValidRequest(), "1234567890123");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber")));
  }

  @Test
  void cardNumber_withMoreThan19Digits_shouldFail() {
    PostPaymentRequest request = withCardNumber(createValidRequest(), "12345678901234567890");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber")));
  }

  @Test
  void cardNumber_with14Digits_shouldPass() {
    PostPaymentRequest request = withCardNumber(createValidRequest(), "12345678901234");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void cardNumber_with19Digits_shouldPass() {
    PostPaymentRequest request = withCardNumber(createValidRequest(), "1234567890123456789");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void cardNumber_withNonDigits_shouldFail() {
    PostPaymentRequest request = withCardNumber(createValidRequest(), "4111-1111-1111-1111");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void cardNumber_whenNull_shouldFail() {
    PostPaymentRequest request = withCardNumber(createValidRequest(), null);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void expiryMonth_lessThan1_shouldFail() {
    PostPaymentRequest request = withExpiryMonth(createValidRequest(), 0);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("expiryMonth")));
  }

  @Test
  void expiryMonth_greaterThan12_shouldFail() {
    PostPaymentRequest request = withExpiryMonth(createValidRequest(), 13);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("expiryMonth")));
  }

  @Test
  void expiryMonth_valid_shouldPass() {
    PostPaymentRequest baseRequest = createValidRequest();
    for (int month = 1; month <= 12; month++) {
      PostPaymentRequest request = withExpiryMonth(baseRequest, month);
      Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
      boolean hasMonthViolation = violations.stream()
          .anyMatch(v -> v.getPropertyPath().toString().equals("expiryMonth"));
      assertFalse(hasMonthViolation, "Month " + month + " should be valid");
    }
  }

  @Test
  void expiryDate_inPast_shouldFail() {
    YearMonth past = YearMonth.now().minusMonths(1);
    PostPaymentRequest request = new PostPaymentRequest(
        "4111111111111111",
        past.getMonthValue(),
        past.getYear(),
        "USD",
        1000,
        "123"
    );
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void expiryDate_currentMonth_shouldFail() {
    YearMonth now = YearMonth.now();
    PostPaymentRequest request = new PostPaymentRequest(
        "4111111111111111",
        now.getMonthValue(),
        now.getYear(),
        "USD",
        1000,
        "123"
    );
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void expiryDate_inFuture_shouldPass() {
    YearMonth future = YearMonth.now().plusMonths(1);
    PostPaymentRequest request = new PostPaymentRequest(
        "4111111111111111",
        future.getMonthValue(),
        future.getYear(),
        "USD",
        1000,
        "123"
    );
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void cvv_with3Digits_shouldPass() {
    PostPaymentRequest request = withCvv(createValidRequest(), "123");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void cvv_with4Digits_shouldPass() {
    PostPaymentRequest request = withCvv(createValidRequest(), "1234");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void cvv_with2Digits_shouldFail() {
    PostPaymentRequest request = withCvv(createValidRequest(), "12");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cvv")));
  }

  @Test
  void cvv_with5Digits_shouldFail() {
    PostPaymentRequest request = withCvv(createValidRequest(), "12345");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void cvv_withNonDigits_shouldFail() {
    PostPaymentRequest request = withCvv(createValidRequest(), "12a");
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"USD", "EUR", "GBP", "usd"})
  void currency_supported_shouldPass(String currency) {
    PostPaymentRequest request = withCurrency(createValidRequest(), currency);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"JPY", "US", "USDD"})
  void currency_invalid_shouldFail(String currency) {
    PostPaymentRequest request = withCurrency(createValidRequest(), currency);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("currency")));
  }

  @Test
  void amount_whenZero_shouldFail() {
    PostPaymentRequest request = withAmount(createValidRequest(), 0);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
  }

  @Test
  void amount_whenNegative_shouldFail() {
    PostPaymentRequest request = withAmount(createValidRequest(), -1);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertFalse(violations.isEmpty());
  }

  @Test
  void amount_whenPositive_shouldPass() {
    PostPaymentRequest request = withAmount(createValidRequest(), 1);
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertTrue(violations.isEmpty());
  }

  @Test
  void getLastFourDigits_returnsCorrectValue() {
    PostPaymentRequest request = withCardNumber(createValidRequest(), "4111111111111234");
    assertEquals("1234", request.getLastFourDigits());
  }

  @Test
  void getFormattedExpiryDate_returnsCorrectFormat() {
    PostPaymentRequest request = new PostPaymentRequest(
        "4111111111111111",
        3,
        2027,
        "USD",
        1000,
        "123"
    );
    assertEquals("03/2027", request.getFormattedExpiryDate());
  }


  private PostPaymentRequest createValidRequest() {
    YearMonth future = YearMonth.now().plusMonths(6);
    return new PostPaymentRequest(
        "4111111111111111",
        future.getMonthValue(),
        future.getYear(),
        "USD",
        1000,
        "123"
    );
  }

  private PostPaymentRequest withCardNumber(PostPaymentRequest request, String cardNumber) {
    return new PostPaymentRequest(
        cardNumber,
        request.expiryMonth(),
        request.expiryYear(),
        request.currency(),
        request.amount(),
        request.cvv()
    );
  }

  private PostPaymentRequest withExpiryMonth(PostPaymentRequest request, Integer expiryMonth) {
    return new PostPaymentRequest(
        request.cardNumber(),
        expiryMonth,
        request.expiryYear(),
        request.currency(),
        request.amount(),
        request.cvv()
    );
  }

  private PostPaymentRequest withCurrency(PostPaymentRequest request, String currency) {
    return new PostPaymentRequest(
        request.cardNumber(),
        request.expiryMonth(),
        request.expiryYear(),
        currency,
        request.amount(),
        request.cvv()
    );
  }

  private PostPaymentRequest withAmount(PostPaymentRequest request, int amount) {
    return new PostPaymentRequest(
        request.cardNumber(),
        request.expiryMonth(),
        request.expiryYear(),
        request.currency(),
        amount,
        request.cvv()
    );
  }

  private PostPaymentRequest withCvv(PostPaymentRequest request, String cvv) {
    return new PostPaymentRequest(
        request.cardNumber(),
        request.expiryMonth(),
        request.expiryYear(),
        request.currency(),
        request.amount(),
        cvv
    );
  }
}
