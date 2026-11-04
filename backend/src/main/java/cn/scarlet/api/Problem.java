package cn.scarlet.api;

public class Problem extends RuntimeException {

  public final int status;
  public final String code;

  public Problem(int status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static Problem missing() {
    return new Problem(404, "NOT_FOUND", "这份藏书暂未开放，或已经移往别处");
  }
}
