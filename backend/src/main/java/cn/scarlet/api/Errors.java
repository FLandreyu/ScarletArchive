package cn.scarlet.api;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class Errors {

  private static final Logger log = LoggerFactory.getLogger(Errors.class);

  private ResponseEntity<Api.Result<Void>> reply(int s, String c, String m) {
    return ResponseEntity.status(s).body(new Api.Result<>(c, m, null));
  }

  @ExceptionHandler(Problem.class)
  public ResponseEntity<?> business(Problem e) {
    return reply(e.status, e.code, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return reply(
      400,
      "VALIDATION",
      e
        .getBindingResult()
        .getFieldErrors()
        .stream()
        .findFirst()
        .map(x -> x.getField() + "：" + x.getDefaultMessage())
        .orElse("请检查输入")
    );
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    MissingRequestHeaderException.class,
    MissingServletRequestParameterException.class,
  })
  public ResponseEntity<?> invalid(Exception e) {
    return reply(400, "VALIDATION", "参数格式不正确");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> constraint(Exception e) {
    return reply(409, "DATA_CONFLICT", "名称或短链接已存在，或该分类仍被文章使用");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<?> size(Exception e) {
    return reply(413, "UPLOAD_SIZE", "图片不得超过5MB");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<?> missing(Exception e) {
    return reply(404, "NOT_FOUND", "页面或资源不存在");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> unknown(Exception e) {
    log.error("Request failed", e);
    return reply(500, "INTERNAL", "服务暂时不可用，请稍后重试");
  }
}
