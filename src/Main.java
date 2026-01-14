// Multithreaded File Finder

// Imports:
import java.io.File; // File is used to create files/directories and read various properties with its methods
import java.util.ArrayList; // ArrayList is used to handle dynamic lists of objects
import java.util.List; // List is an interface (adds abstract methods/properties to lists)
import java.util.Scanner; // Scanner to read input from console
import java.util.concurrent.atomic.AtomicBoolean; // AtomicBoolean to handle a shared boolean between threads

// The keys to optimizing this algorithm are:
// 1. Parallelism and AtomicBoolean for threads to communicate
// 2. Round-robin distribution among threads to balance consumption
// 3. File methods to efficiently traverse directories/subdirectories
class Main {

  private static final int NUM_THREADS = 4;

  public static void main(String[] args) {
    // Prompt and read the root directory
    System.out.println(
      "Enter the root directory to search (or press Enter to use current directory):"
    );
    Scanner scanner = new Scanner(System.in);
    String rootPath = scanner.nextLine();

    // If left empty, set the base directory with "user.dir"
    // "user.dir" is a system property (in Java) that returns the current execution directory
    if (rootPath.isEmpty()) {
      rootPath = System.getProperty("user.dir");
    }

    System.out.println("Enter the file name to search with its extension:");
    String targetFile = scanner.nextLine();

    // Handle this error...
    if (targetFile.isEmpty()) {
      scanner.close();
      throw new IllegalArgumentException("File name cannot be empty.");
    }

    scanner.close();

    File rootDir = new File(rootPath);

    // Handle invalid directory error...
    if (!rootDir.exists() || !rootDir.isDirectory()) {
      throw new IllegalArgumentException(
        "The specified directory does not exist or is not a directory."
      );
    }

    // Get all subdirectories from the root directory
    File[] allFiles = rootDir.listFiles();
    List<File> subdirectories = new ArrayList<>();

    // If there are files OR directories...
    if (allFiles != null) {
      for (File file : allFiles) {
        // Only filter directories to distribute them among threads
        if (file.isDirectory()) {
          subdirectories.add(file);
        }
      }
    }
    // Add the initial directory to search for files there too
    subdirectories.add(0, rootDir);

    // If there's nothing to search...
    // (And found = true wasn't set in any thread)
    if (subdirectories.isEmpty()) {
      System.out.println("No subdirectories to search in: " + rootPath);
      return;
    }

    // Print the found directories and available threads to traverse them
    System.out.println(
      "Found " +
        subdirectories.size() +
        " directory(ies) to distribute among " +
        NUM_THREADS +
        " threads"
    );

    // Create the thread array with length NUM_THREADS
    Thread[] threads = new Thread[NUM_THREADS];

    // Create the initial AtomicBoolean for all threads as found = false
    AtomicBoolean found = new AtomicBoolean(false);
    // Since we pass by reference, all threads share the same object
    // And it gets updated when a file is found

    // Double loop:
    // For each thread... (up to the maximum, NUM_THREADS)
    for (int i = 0; i < NUM_THREADS; i++) {
      List<File> threadDirectories = new ArrayList<>();

      // ---
      // ROUND-ROBIN DISTRIBUTION
      // ---

      // For each subdirectory, assign in round-robin
      // This is achieved with j += NUM_THREADS, because each thread takes its "turn"
      for (int j = i; j < subdirectories.size(); j += NUM_THREADS) {
        threadDirectories.add(subdirectories.get(j));
        // We add to threadDirectories the directories in a distributed order, thus balancing the load
        // They are added in rounds of maximum NUM_THREADS, so we have MINIMUM 1 directory per thread if directories > NUM_THREADS
        // And the case where one thread has 0 directories (busy waiting) and another has many doesn't occur
      }

      // ---

      // If the directory is not empty, create the thread!
      // This optimizes resources, beyond defining a fixed NUM_THREADS
      if (!threadDirectories.isEmpty()) {
        threads[i] = new Thread(
          new FileFinder(threadDirectories, targetFile, i + 1, found),
          "Thread-" + (i + 1)
        );
        threads[i].start();
      }
    }

    System.out.println("Starting search with " + NUM_THREADS + " threads...");

    try {
      // For each thread, join to wait for them to finish
      for (int i = 0; i < NUM_THREADS; i++) {
        if (threads[i] != null) {
          threads[i].join();
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // After initializing and waiting for all threads, check if the file was found
    // Remember that the algorithm traverses to "leaves", i.e., directories with only files, no subdirectories
    if (!found.get()) {
      System.out.println("File '" + targetFile + "' not found in: " + rootPath);
    }

    System.out.println("Search completed!");
  }
}
