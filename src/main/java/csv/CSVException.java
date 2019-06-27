package csv;

/**
 * @author Omar Muhtaseb
 * @since 2018-12-30
 */
public class CSVException extends RuntimeException {

    public CSVException(Exception exception) {
        super(exception);
    }
}
