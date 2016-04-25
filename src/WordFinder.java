import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * Created by trpet15 - Troels Blicher Petersen <troels@newtec.dk> on 4/17/16.
 */
interface WordCount {
    public String word();

    public int amount();
}
class WordCountCompare implements Comparator<WordCount> {

    @Override
    public int compare(WordCount wordCount, WordCount wordCount2) {
        if(wordCount.amount() < wordCount2.amount()) {
            return 1;
        }
        else if(wordCount.amount() > wordCount2.amount()) {
            return -1;
        }
        return 0;
    }
}
public class WordFinder {
    static int cores = Runtime.getRuntime().availableProcessors() + 1;
    private static ThreadPoolExecutor pool;
    private static List<Result> rList = new ArrayList<>();
    private static List<Result> rListLong = new ArrayList<>();
    private static ConcurrentHashMap<Result, String> cMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Result, String> ListMap = new ConcurrentHashMap<>();

    private static Stats searchStats = new Stats() {
        @Override
        public int occurrences(String word) {
            return Collections.frequency(cMap.values(), word);
        }

        @Override
        public List<Result> foundIn(String word) {
            return cMap.entrySet().stream().filter(entry -> Objects.equals(word, entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
        }

        @Override
        public String mostFrequent() {
            WordCount maxWord = new WordCount() {
                @Override
                public String word() {
                    return null;
                }

                @Override
                public int amount() {
                    return 0;
                }
            };
            for (String word : cMap.values()) {
                int wordAmount = Collections.frequency(cMap.values(),word);
                if(wordAmount> maxWord.amount()) {
                    maxWord = new WordCount() {
                        @Override
                        public String word() {
                            return word;
                        }

                        @Override
                        public int amount() {
                            return wordAmount;
                        }
                    };
                }
            }
            return maxWord.word();
        }

        @Override
        public String leastFrequent() {
            WordCount minWord = new WordCount() {
                @Override
                public String word() {
                    return null;
                }

                @Override
                public int amount() {
                    return Collections.frequency(cMap.values(),cMap.values().toArray()[0].toString());
                }
            };
            for (String word : cMap.values()) {
                int wordAmount = Collections.frequency(cMap.values(),word);
                if(wordAmount < minWord.amount()) {
                    minWord = new WordCount() {
                        @Override
                        public String word() {
                            return word;
                        }

                        @Override
                        public int amount() {
                            return wordAmount;
                        }
                    };
                }
            }
            return minWord.word();
        }

        @Override
        public List<String> words() {
            List<String> cMapValues = new ArrayList<>(cMap.values());
            Set<String> hs = new HashSet<>();
            hs.addAll(cMapValues);
            cMapValues.clear();
            cMapValues.addAll(hs);
            return cMapValues;
        }

        @Override
        public List<String> wordsByOccurrences() {
            List<WordCount> words = new ArrayList<>();
            for (String word : cMap.values()) {
                words.add(new WordCount() {
                    @Override
                    public String word() {
                        return word;
                    }

                    @Override
                    public int amount() {
                        return Collections.frequency(cMap.values(),word);
                    }
                });
            }
            words.sort(new WordCountCompare());
            ArrayList<String> orderedWords = new ArrayList<>();
            for(int i = 0; i < words.size();i++) {
                if(!orderedWords.contains(words.get(i).word())) {
                    orderedWords.add(words.get(i).word());
                }
            }
            return orderedWords;
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
    public static List<Result> findAll(String word, Path dir) {
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
        RecursiveFindAll(word, dir);
        while (true) {
            if (pool.getActiveCount() < 1) break;
//            else if (tick) pool.shutdownNow();
        }
//        rList.addAll(rListLong);
//        throw new UnsupportedOperationException();
//        rList.addAll(cMap.keySet());
        pool.shutdown();
        return new ArrayList<>(ListMap.keySet());
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
//                synchronized (rList) {
                ListMap.put(new Result() {
                    @Override
                    public Path path() {
                        return file;
                    }

                    @Override
                    public int line() {
                        return ln;
                    }
                }, word);
//                }
            }
        }
    }

    private static void fileWordCounter(String wordToFind, Path file) throws IOException {
        int ln = 0;
        String line;
        BufferedReader reader = Files.newBufferedReader(file);
        while ((line = reader.readLine()) != null) {
            final String currentLine = line;
            final int thisLine = ln;
            String lineWords[] = currentLine.split("\\s+");
            for (String word : lineWords) {
                if (word.equals(wordToFind)) {
                    ListMap.put(new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return thisLine+1;
                        }
                    }, word);
                }

            }
            ln++;
        }
        reader.close();
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
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
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
                            return ln+1;
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
        pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(cores);
        RecursiveStats(dir);
        while (true) {
            if (pool.getActiveCount() < 1) break;
//            else if (tick) pool.shutdownNow();
        }
//        throw new UnsupportedOperationException();
        pool.shutdown();
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
                    if (path.toString().endsWith(".txt") || path.toString().endsWith(".conf") || path.toString().endsWith(".java")) {
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
        for (String word : lineWords)
            synchronized (cMap) {
                cMap.put(new Result() {
                    @Override
                    public Path path() {
                        return file;
                    }

                    @Override
                    public int line() {
                        return ln+1;
                    }
                }, word);
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
            for (String word : lineWords)
                synchronized (cMap) {
                    cMap.put(new Result() {
                        @Override
                        public Path path() {
                            return file;
                        }

                        @Override
                        public int line() {
                            return thisLine+1;
                        }
                    }, word);
                }
            ln++;
        }
        reader.close();
    }
}