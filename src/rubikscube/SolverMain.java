package rubikscube;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class SolverMain {
    
    public static void main(String[] args) {
        String testcasesDir = "testcases";
        File dir = new File(testcasesDir);
        
        // Get all scramble files
        File[] files = dir.listFiles((d, name) -> name.startsWith("scramble") && name.endsWith(".txt"));
        
        if (files == null || files.length == 0) {
            System.out.println("No test files found in " + testcasesDir + " directory.");
            System.out.println("Please create files like: scramble01.txt, scramble02.txt, etc.");
            return;
        }
        
        // Sort files by name
        Arrays.sort(files);
        
        System.out.println("=== Rubik's Cube Solver ===");
        System.out.println("Found " + files.length + " test case(s)\n");
        
        int solvedCount = 0;
        int failedCount = 0;
        long totalTime = 0;
        
        for (File file : files) {
            System.out.println("Processing: " + file.getName());
            System.out.println("-".repeat(50));
            
            try {
                // Load the scrambled cube
                RubiksCube cube = new RubiksCube(file.getPath());
                
                System.out.println("Initial state:");
                System.out.print(cube);
                
                // Solve the cube
                RubiksCubeSolver solver = new RubiksCubeSolver();
                long startTime = System.currentTimeMillis();
                String solution = solver.solve(cube);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                totalTime += duration;
                
                if (solution != null) {
                    System.out.println("✓ SOLVED!");
                    System.out.println("Solution: " + solution);
                    System.out.println("Moves: " + solution.length());
                    System.out.println("Time: " + duration + " ms");
                    System.out.println("Nodes explored: " + solver.getNodesExplored());
                    
                    // Verify solution
                    cube.applyMoves(solution);
                    if (cube.isSolved()) {
                        System.out.println("✓ Verification: Solution is correct!");
                        solvedCount++;
                    } else {
                        System.out.println("✗ Verification FAILED: Solution is incorrect!");
                        failedCount++;
                    }
                } else {
                    System.out.println("✗ FAILED: No solution found within depth limit");
                    System.out.println("Time: " + duration + " ms");
                    failedCount++;
                }
                
            } catch (IOException e) {
                System.out.println("✗ ERROR: Could not read file - " + e.getMessage());
                failedCount++;
            } catch (IncorrectFormatException e) {
                System.out.println("✗ ERROR: Incorrect file format - " + e.getMessage());
                failedCount++;
            } catch (Exception e) {
                System.out.println("✗ ERROR: " + e.getMessage());
                e.printStackTrace();
                failedCount++;
            }
            
            System.out.println();
        }
        
        // Summary
        System.out.println("=".repeat(50));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(50));
        System.out.println("Total test cases: " + files.length);
        System.out.println("Solved: " + solvedCount);
        System.out.println("Failed: " + failedCount);
        System.out.println("Total time: " + totalTime + " ms");
        if (solvedCount > 0) {
            System.out.println("Average time per solved cube: " + (totalTime / solvedCount) + " ms");
        }
    }
}