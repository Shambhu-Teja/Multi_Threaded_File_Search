import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;  

public class MultiThreadedFileSearch {

    static class FileSearchTask implements Callable<List<String>> {
        private final File file;
        private final String searchString;

        public FileSearchTask(File file, String searchString) {
            this.file = file;
            this.searchString = searchString;
        }

        @Override
        public List<String> call() throws Exception {
            List<String> matchingFiles = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(searchString)) {
                        matchingFiles.add(file.getAbsolutePath());
                        break; // Stop reading once a match is found
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + file.getAbsolutePath());
            }
            return matchingFiles;
        }
    }

    public static List<File> getTxtFiles(File folder) {
        List<File> txtFiles = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    txtFiles.add(file);
                } else if (file.isDirectory()) {
                    txtFiles.addAll(getTxtFiles(file));
                }
            }
        }
        return txtFiles;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        if (args.length < 2) {
            System.out.println("Usage: java MultiThreadedFileSearch <folder_path> <search_string>");
            return;
        }

        String folderPath = args[0];
        String searchString = args[1];

        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            System.out.println("Invalid folder path: " + folderPath);
            return;
        }

        long startTime = System.currentTimeMillis();

        List<File> txtFiles = getTxtFiles(folder);
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<String>>> futures = new ArrayList<>();

        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of threads used: " + numThreads);

        // Submit tasks to the executor for each file
        for (File file : txtFiles) {
            futures.add(executor.submit(new FileSearchTask(file, searchString)));
        }

        List<String> result = new ArrayList<>();
        for (Future<List<String>> future : futures) {
            try {
                result.addAll(future.get()); // Gather results from each task
            } catch (ExecutionException e) {
                System.err.println("Error during file search: " + e.getMessage());
            }
        }

        // Shutdown the executor and wait for tasks to complete
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {  // Corrected with TimeUnit
            executor.shutdownNow();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + "ms");

        System.out.println("Files containing the string \"" + searchString + "\":");
        for (String filePath : result) {
            System.out.println(filePath);
        }
    }
}
