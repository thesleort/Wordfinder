import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by trpet15 - Troels Blicher Petersen <troels@newtec.dk> on 4/17/16.
 */
public class WordFinder {
    static int cores = Runtime.getRuntime().availableProcessors() + 1;
    private static ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
    private static List<Result> rList = new ArrayList<>();
    private static List<Result> rListLong = new ArrayList<>();
    private static ConcurrentHashMap<Result,String> cMap = new ConcurrentHashMap<>();
    private static Stats searchStats = new Stats() {
        @Override
        public int occurrences(String word) {

            int occ = Collections.frequency(cMap.values(), word);
//            for(Result s : cMap.values()) {
//                System.out.println(s.path());
//            }
            return occ;
        }

        @Override
        public List<Result> foundIn(String word) {
            return null;
        }

        @Override
        public String mostFrequent() {
            return null;
        }

        @Override
        public String leastFrequent() {
            return null;
        }

        @Override
        public List<String> words() {
            return null;
        }

        @Override
        public List<String> wordsByOccurrences() {
            return null;
        }
    };

    private static Result anyResult;
    private static boolean tick = false;

    /**
     * Finds all the (case-sensitive) occurrences of a word in a directory.
     * Only text files should be considered (files ending with the .txt suffix).
     * <p>
     * The word must be an exact match: it is case-sensitive and may contain punctuation.
     * See https://github.com/fmontesi/cp2016/tree/master/exam for more details.
     * <p>
     * The search is recursive: if the directory contains subdirectories,
     * these are also searched and so on so forth (until there are no more
     * subdirectories).
     *
     * @param word the word to find (does not contain whitespaces or punctuation)
     * @param dir  the directory to search
     * @return a list of results ({@link Result}), which tell where the word was found
     */
    public static ArrayList<Result> findAll(String word, Path dir) {
        RecursiveFindAll(word, dir);
        while (true) {
            if (pool.getActiveCount() < 1) break;
//            else if (tick) pool.shutdownNow();
        }
        rList.addAll(rListLong);
//        throw new UnsupportedOperationException();
        return (ArrayList<Result>) rList;
    }

    private static void RecursiveFindAll(String word, Path dir) {
        try (
                DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)
        ) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path)) {
                    RecursiveFindAll(word, path);
                } else {
                    if (path.toString().endsWith(".js") || path.toString().endsWith(".conf") || path.toString().endsWith(".java")) {
//                        System.out.println(path.toString());
                        fileOperation(word, path);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //    19060 ms
//    1215191 results
//    1215191 results
    private static void fileOperation(String word, Path file) throws IOException {
        if ((file.getFileName().toFile().length() / 1048576) > 1) {
            int ln = 0;
            String line;
            BufferedReader reader = Files.newBufferedReader(file);
            while ((line = reader.readLine()) != null) {
                final String currentLine = line;
                final int thisLine = ln;
                pool.submit(() -> wordCounter(thisLine, word, currentLine, file));
                ln++;
            }
            reader.close();
        } else {
            pool.submit(() -> {
                try {
                    fileWordCounter(word, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void wordCounter(int ln, String wordToFind, String line, Path file) {
        String lineWords[] = line.split("\\s+");
        for (String word : lineWords) {
            if (word.equals(wordToFind)) {
                synchronized (rList) {
                    rList.add(new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return ln;
                        }
                    });
                }
            }
        }
    }

    private static void fileWordCounter(String wordToFind, Path file) throws IOException {
        int ln = 0;
        String line;
        ArrayList<Result> localAL = new ArrayList<>();
        BufferedReader reader = Files.newBufferedReader(file);
        while ((line = reader.readLine()) != null) {
            final String currentLine = line;
            final int thisLine = ln;
            String lineWords[] = currentLine.split("\\s+");
            for (String word : lineWords) {
                if (word.equals(wordToFind)) {
                    localAL.add(new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return thisLine;
                        }
                    });
                }

            }
            ln++;
        }
        reader.close();
        synchronized (rList) {
            rListLong.addAll(localAL);
        }
    }

    /**
     * Finds an occurrence of a word in a directory and returns.
     * <p>
     * This method searches only for one (any) occurrence of the word in the
     * directory. As soon as one such occurrence is found, the search can be
     * stopped and the method can return immediately.
     * <p>
     * As for method {@code findAll}, the search is recursive.
     *
     * @param word
     * @param dir
     * @return
     */
    public static Result findAny(String word, Path dir) {
        try (
                DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)
        ) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path) && !tick) {
                    findAny(word, path);
                } else if (!tick && (path.toString().endsWith(".js") || path.toString().endsWith(".conf") || path.toString().endsWith(".java"))) {
//                        System.out.println(path.toString());
                    fileOperationAny(word, path);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            if (pool.getActiveCount() < 1) break;
            else if (tick) pool.shutdownNow();
        }
        return anyResult;
    }

    private static void fileOperationAny(String word, Path file) throws IOException {
        if ((file.getFileName().toFile().length() / 1048576) > 1 && !tick) {
            int ln = 0;
            String line;
            BufferedReader reader = Files.newBufferedReader(file);
            while ((line = reader.readLine()) != null && !tick) {
                final String currentLine = line;
                final int thisLine = ln;
                pool.submit(() -> wordCounterAny(thisLine, word, currentLine, file));
                ln++;
            }
            reader.close();
        } else if (!tick) {
            pool.submit(() -> {
                try {
                    fileWordCounterAny(word, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void wordCounterAny(int ln, String wordToFind, String line, Path file) {
        String lineWords[] = line.split("\\s+");
        for (String word : lineWords) {
            if (word.equals(wordToFind) && !tick) {
                synchronized (anyResult) {
                    anyResult = new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return ln;
                        }
                    };
                    tick = true;
                    break;
                }
            }
        }
    }

    private static void fileWordCounterAny(String wordToFind, Path file) throws IOException {
        int ln = 0;
        String line;
        BufferedReader reader = Files.newBufferedReader(file);
        while ((line = reader.readLine()) != null && !tick) {
            final String currentLine = line;
            final int thisLine = ln;
            String lineWords[] = currentLine.split("\\s+");
            for (String word : lineWords) {
                if (word.equals(wordToFind) && !tick) {
                    anyResult = new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return thisLine;
                        }
                    };
                    tick = true;
                    break;
                }

            }
            ln++;
        }
        reader.close();
    }

    /**
     * Computes overall statistics about the occurrences of words in a directory.
     * <p>
     * This method recursively searches the directory for all words and returns
     * a {@link Stats} object containing the statistics of interest. See the
     * documentation of {@link Stats}.
     *
     * @param dir the directory to search
     * @return the statistics of occurring words in the directory
     */
    public static Stats stats(Path dir) {
        RecursiveStats(dir);
        while (true) {
            if (pool.getActiveCount() < 1) break;
//            else if (tick) pool.shutdownNow();
        }
//        throw new UnsupportedOperationException();
        System.out.println("done");
        return searchStats;
    }

    private static void RecursiveStats(Path dir) {
        try (
                DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)
        ) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path)) {
                    RecursiveStats(path);
                } else {
                    if (path.toString().endsWith(".js") || path.toString().endsWith(".conf") || path.toString().endsWith(".java")) {
//                        System.out.println(path.toString());
                        fileOperationStats(path);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fileOperationStats(Path file) throws IOException {
        if ((file.getFileName().toFile().length() / 1048576) > 1) {
            int ln = 0;
            String line;
            BufferedReader reader = Files.newBufferedReader(file);
            while ((line = reader.readLine()) != null) {
                final String currentLine = line;
                final int thisLine = ln;
                pool.submit(() -> wordStatsCounter(thisLine, currentLine, file));
                ln++;
            }
            reader.close();
        } else {
            pool.submit(() -> {
                try {
                    fileWordStatsCounter(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void wordStatsCounter(int ln, String line, Path file) {
        String lineWords[] = line.split("\\s+");
        for (String word : lineWords) synchronized (cMap){
            cMap.put(new Result() {
                @Override
                public Path path() {
                    return file;
                }

                @Override
                public int line() {
                    return ln;
                }
            },word);
        }
    }

    private static void fileWordStatsCounter(Path file) throws IOException {
        int ln = 0;
        String line;
        BufferedReader reader = Files.newBufferedReader(file);
        while ((line = reader.readLine()) != null) {
            final String currentLine = line;
            final int thisLine = ln;
            String lineWords[] = currentLine.split("\\s+");
            for (String word : lineWords) synchronized (cMap){
                cMap.put(new Result() {
                    @Override
                    public Path path() {
                        return file;
                    }

                    @Override
                    public int line() {
                        return thisLine;
                    }
                },word);
            }
            ln++;
        }
        reader.close();
    }
}