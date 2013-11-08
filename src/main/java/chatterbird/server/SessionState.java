package chatterbird.server;


public enum SessionState {
  CONNECTING(1),
  OPEN(2),
  CLOSING(3),
  CLOSED(4);
  private final int value;

  private SessionState(final int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
