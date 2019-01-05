package com.sonf.core.session;

public class IdleStatus {
    /**
     * Represents the session status that no data is coming from the remote
     * peer.
     */
    public static final IdleStatus READER_IDLE = new IdleStatus("reader idle");

    /**
     * Represents the session status that the session is not writing any data.
     */
    public static final IdleStatus WRITER_IDLE = new IdleStatus("writer idle");

    /**
     * Represents both {@link #READER_IDLE} and {@link #WRITER_IDLE}.
     */
    public static final IdleStatus BOTH_IDLE = new IdleStatus("both idle");

    private final String strValue;

    /**
     * Creates a new instance.
     */
    private IdleStatus(String strValue) {
        this.strValue = strValue;
    }

    /**
     * @return the string representation of this status.
     * <ul>
     *   <li>{@link #READER_IDLE} - <tt>"reader idle"</tt></li>
     *   <li>{@link #WRITER_IDLE} - <tt>"writer idle"</tt></li>
     *   <li>{@link #BOTH_IDLE} - <tt>"both idle"</tt></li>
     * </ul>
     */
    @Override
    public String toString() {
        return strValue;
    }
}
