// Imports:
import java.io.File; // File is used to create files/directories and read various properties with its methods
import java.util.List; // List is also for dynamic lists, but it's an interface
import java.util.concurrent.atomic.AtomicBoolean; // AtomicBoolean to handle a shared boolean between threads

/**
 * FileFinder - Class that implements Runnable and searches for a file in the child subdirectories of the entered directory
 * Receives:
 * - List of directories to search
 * - Name of the file to search
 * - Thread ID (for printing logs)
 * - AtomicBoolean shared between threads to indicate if the file was already found
 *
 * The search is performed like a DFS descending through the directory hierarchy
 */
class FileFinder implements Runnable {

  // List of directories present in the directory entered by the user
  private List<File> directories;
  // Name of the file to search
  private String fileName;
  // Thread ID for identification in logs
  private int threadId;
  // "Atomic" (mutable) success boolean to use as a shared resource between threads
  private AtomicBoolean found;

  // Constructor
  public FileFinder(
    List<File> directories,
    String fileName,
    int threadId,
    AtomicBoolean found
  ) {
    this.directories = directories;
    this.fileName = fileName;
    this.threadId = threadId;
    this.found = found;
  }

  // Override the run() method, so when this class is passed as a parameter to a Thread,
  // It executes the following code
  @Override
  public void run() {
    System.out.println(
      "Thread-" +
        threadId +
        " starting search in " +
        directories.size() +
        " directory(ies)"
    );

    for (File directory : directories) {
      // .get() on AtomicBoolean to read the current value
      if (found.get()) {
        // If another thread already found the file, we finish
        System.out.println(
          "Thread-" +
            threadId +
            " stopping because another thread already found the file!"
        );
        // Ideally this doesn't print, because the search is fast and we control threads well
        // Also we return immediately in searchFile if found is true
        return;
      }

      // Print each directory search
      System.out.println(
        "Thread-" + threadId + " searching in: " + directory.getAbsolutePath()
      );
      // If there's no match, use recursion to search in subdirectories!
      searchFile(directory);

      // If another thread found it in this iteration...
      if (found.get()) {
        return;
      }
    }

    // Print once a thread reached a "leaf", i.e., a directory with all files and no subdirectories WITHOUT MATCH
    System.out.println("Thread-" + threadId + " finished its search");
  }

  private void searchFile(File dir) {
    // If another thread already found the file...
    if (found.get()) {
      return;
      // NOTE: This return prevents many lines from executing unnecessarily, because as mentioned, found is a shared AtomicBoolean
    }

    // Create a File array with the files and subdirectories of the current directory
    File[] files = dir.listFiles();

    // If there's something...
    if (files != null) {
      // For each file (file/directory)...
      for (File file : files) {
        if (found.get()) {
          // Another thread already found the file!
          return;
        }

        if (file.isDirectory()) {
          // If the file is a directory, search recursively
          searchFile(file);
          // IMPORTANT: This runs in the same thread, doesn't create new threads
          // And descends through the directory hierarchy with a round-robin distribution configured in its implementation
          // (See in Main class, main method)
        } else if (file.getName().equals(fileName)) {
          // If we found the file, set "found = true"!
          found.set(true);
          // Success!
          System.out.println(
            "FILE FOUND! " +
              file.getAbsolutePath() +
              " by thread " +
              Thread.currentThread().getName()
          );
          return;
        }
      }
    }
  }
}
