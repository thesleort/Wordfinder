import java.nio.file.*;

/**
 * Created by trpet15 - Troels Blicher Petersen <troels@newtec.dk> on 4/17/16.
 */
public interface Result {
    /**
     * The file ({@link Path}) that the word occurs in.
     * @return the file ({@link Path}) that the word occurrs in
     */
    public Path path();

    /**
     * The line at which the word occurs in the file.
     * @return the line at which the word occurs in the file
     */
    public int line();

}