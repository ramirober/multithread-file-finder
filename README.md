# `multithread-file-finder`

## Description

Java application that searches for a file (name + extension) within a root directory and all its subdirectories.
The search is performed with multiple threads. Each thread receives a set of directories distributed in round-robin fashion to balance the load.
When a thread finds the file, it notifies the rest via a shared AtomicBoolean and the search stops quickly.

## Technical Keys

- Parallelism with Thread[] (fixed NUM_THREADS).
- Round-robin distribution of initial subdirectories (indices 0,4,8... / 1,5,9... etc. when using 4 threads as currently configured with NUM_THREADS).
- Recursive traversal (DFS) within each thread, without creating new threads in depth.
- AtomicBoolean for lightweight signaling (avoids unnecessary work once the file is found).

## Components

- Main.java: reads user input, builds subdirectory list and creates threads.
- FileFinder.java (Runnable): performs recursive search in assigned directories and reports if it finds the file.

## Flow

1. User enters the root directory (Enter = current directory).
2. Enters the exact name of the file to search (e.g.: data.csv, image.png, Main.java).
3. Subdirectories are listed and distributed among N threads.
4. Each thread traverses its directories and descends into subdirectories.
5. If a thread finds the file: prints absolute path and sets found=true.
6. The rest of the threads stop execution upon detecting found=true.
7. If no thread finds it, a message is displayed at the end.

## Execution

Compile:

```
javac Main.java
```

Run:

```
java Main
```

Execution example:

```
Enter the root directory to search (or press Enter to use current directory):
./
Enter the file name to search with its extension:
README.md
Found X directory(ies) to distribute among 4 threads
Starting search with 4 threads...
FILE FOUND! /absolute/path/README.md by thread Thread-2
Search completed!
```
