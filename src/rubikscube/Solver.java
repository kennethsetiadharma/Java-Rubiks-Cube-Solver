package rubikscube;

import java.io.*;
import java.util.*;

/**
 * Bidirectional BFS Rubik's Cube Solver
 * Fast non-optimal solver for deeply scrambled cubes
 */
public class Solver {
    
    private static final char[] ALL_MOVES = {'U', 'D', 'F', 'B', 'L', 'R'};
    private static final long TIMEOUT_MS = 10000; // 10 second timeout
    private int nodesExplored;
    private long startTime;
    private boolean timedOut;
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(1);
        }
        
        String inputFile = args[0];
        String outputFile = args[1];
        
        try {
            RubiksCube cube = new RubiksCube(inputFile);
            Solver solver = new Solver();
            String solution = solver.solve(cube);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                if (solution != null) {
                    writer.println(solution);
                } else {
                    writer.println("No solution found - timed out");
                }
            }
        } catch (Exception e) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.println("Error: Could not solve cube");
            } catch (IOException ex) {
                System.exit(1);
            }
        }
    }
    
    public String solve(RubiksCube cube) {
        nodesExplored = 0;
        startTime = System.currentTimeMillis();
        timedOut = false;
        
        if (cube.isSolved()) {
            return "";
        }
        
        // Try bidirectional search
        String result = bidirectionalSearch(cube);
        
        if (result != null) {
            return result;
        }
        
        if (timedOut) {
            return null;
        }
        
        // If bidirectional fails, try IDA* (better than simple BFS for deep scrambles)
        result = idaStar(cube);
        
        if (timedOut) {
            return null;
        }
        
        if (result != null) {
            return result;
        }
        
        // Last resort: simple BFS with lower depth
        result = simpleBFS(cube, 12);
        
        if (timedOut) {
            return null;
        }
        
        return result;
    }
    
    private boolean checkTimeout() {
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            timedOut = true;
            return true;
        }
        return false;
    }
    
    /**
     * IDA* search - memory efficient for deep searches
     */
    private String idaStar(RubiksCube cube) {
        int threshold = getHeuristic(cube);
        
        while (threshold <= 20) {
            if (checkTimeout()) {
                return null;
            }
            
            String result = idaSearch(cube, 0, threshold, ' ', "");
            
            if (result != null) {
                return result;
            }
            
            threshold++;
        }
        
        return null;
    }
    
    /**
     * Recursive IDA* search
     */
    private String idaSearch(RubiksCube cube, int g, int threshold, char lastMove, String path) {
        if (checkTimeout()) {
            return null;
        }
        
        if (cube.isSolved()) {
            return path;
        }
        
        int h = getHeuristic(cube);
        int f = g + h;
        
        if (f > threshold) {
            return null;
        }
        
        nodesExplored++;
        
        String originalState = cube.toString();
        
        for (char move : ALL_MOVES) {
            if (shouldPruneMove(move, path)) {
                continue;
            }
            
            cube.applyMoves(String.valueOf(move));
            String result = idaSearch(cube, g + 1, threshold, move, path + move);
            
            if (result != null) {
                return result;
            }
            
            // Restore
            restoreCube(cube, originalState);
        }
        
        return null;
    }
    
    /**
     * Heuristic function - estimates moves to solve
     * Improved version that considers corners and edges separately
     */
    private int getHeuristic(RubiksCube cube) {
        String state = cube.toString();
        String[] lines = state.split("\n");
        
        int wrongCorners = 0;
        int wrongEdges = 0;
        
        // Check corners (8 corners, 3 stickers each)
        // Top corners
        if (lines[0].charAt(3) != 'O') wrongCorners++;
        if (lines[0].charAt(5) != 'O') wrongCorners++;
        if (lines[2].charAt(3) != 'O') wrongCorners++;
        if (lines[2].charAt(5) != 'O') wrongCorners++;
        
        // Bottom corners
        if (lines[6].charAt(3) != 'R') wrongCorners++;
        if (lines[6].charAt(5) != 'R') wrongCorners++;
        if (lines[8].charAt(3) != 'R') wrongCorners++;
        if (lines[8].charAt(5) != 'R') wrongCorners++;
        
        // Check edges (12 edges, 2 stickers each)
        // Top edges
        if (lines[0].charAt(4) != 'O') wrongEdges++;
        if (lines[1].charAt(3) != 'O') wrongEdges++;
        if (lines[1].charAt(5) != 'O') wrongEdges++;
        if (lines[2].charAt(4) != 'O') wrongEdges++;
        
        // Middle edges
        if (lines[3].charAt(1) != 'G') wrongEdges++;
        if (lines[3].charAt(4) != 'W') wrongEdges++;
        if (lines[3].charAt(7) != 'B') wrongEdges++;
        if (lines[3].charAt(10) != 'Y') wrongEdges++;
        
        // Bottom edges
        if (lines[6].charAt(4) != 'R') wrongEdges++;
        if (lines[7].charAt(3) != 'R') wrongEdges++;
        if (lines[7].charAt(5) != 'R') wrongEdges++;
        if (lines[8].charAt(4) != 'R') wrongEdges++;
        
        // Corners need at least wrongCorners/4 moves (4 corners per move)
        // Edges need at least wrongEdges/4 moves (4 edges per move)
        int cornerEstimate = (wrongCorners + 3) / 4;
        int edgeEstimate = (wrongEdges + 3) / 4;
        
        return Math.max(cornerEstimate, edgeEstimate);
    }
    
    /**
     * Restore cube state
     */
    private void restoreCube(RubiksCube cube, String state) {
        try {
            String[] lines = state.split("\n");
            java.lang.reflect.Field field = RubiksCube.class.getDeclaredField("cube");
            field.setAccessible(true);
            field.set(cube, lines);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore cube", e);
        }
    }
    
    /**
     * Simple BFS - guaranteed to find solution
     */
    private String simpleBFS(RubiksCube cube, int maxDepth) {
        Queue<SearchNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        
        String startState = cube.toString();
        visited.add(startState);
        queue.offer(new SearchNode(startState, ""));
        
        while (!queue.isEmpty()) {
            if (checkTimeout()) {
                return null;
            }
            
            SearchNode node = queue.poll();
            
            if (node.path.length() > maxDepth) {
                continue;
            }
            
            RubiksCube tempCube = stringToCube(node.state);
            
            if (tempCube.isSolved()) {
                return node.path;
            }
            
            nodesExplored++;
            
            // Try all moves
            for (char move : ALL_MOVES) {
                if (shouldPruneMove(move, node.path)) {
                    continue;
                }
                
                tempCube.applyMoves(String.valueOf(move));
                String newState = tempCube.toString();
                
                if (!visited.contains(newState)) {
                    visited.add(newState);
                    queue.offer(new SearchNode(newState, node.path + move));
                }
                
                // Restore
                tempCube = stringToCube(node.state);
            }
        }
        
        return null;
    }
    
    /**
     * Bidirectional search - meet in the middle
     */
    private String bidirectionalSearch(RubiksCube cube) {
        // Forward from scrambled
        Map<String, String> forwardMap = new HashMap<>();
        Queue<SearchNode> forwardQueue = new LinkedList<>();
        
        String startState = cube.toString();
        forwardMap.put(startState, "");
        forwardQueue.offer(new SearchNode(startState, ""));
        
        // Backward from solved
        Map<String, String> backwardMap = new HashMap<>();
        Queue<SearchNode> backwardQueue = new LinkedList<>();
        
        RubiksCube solvedCube = new RubiksCube();
        String solvedState = solvedCube.toString();
        backwardMap.put(solvedState, "");
        backwardQueue.offer(new SearchNode(solvedState, ""));
        
        int maxDepthPerSide = 10; // Search 10 moves from each side (20 total)
        
        for (int depth = 0; depth < maxDepthPerSide; depth++) {
            if (checkTimeout()) {
                return null;
            }
            
            // Expand forward one level
            int forwardSize = forwardQueue.size();
            for (int i = 0; i < forwardSize; i++) {
                if (checkTimeout()) {
                    return null;
                }
                
                SearchNode node = forwardQueue.poll();
                if (node == null) continue;
                
                RubiksCube tempCube = stringToCube(node.state);
                
                for (char move : ALL_MOVES) {
                    if (shouldPruneMove(move, node.path)) {
                        continue;
                    }
                    
                    tempCube.applyMoves(String.valueOf(move));
                    String newState = tempCube.toString();
                    String newPath = node.path + move;
                    
                    if (!forwardMap.containsKey(newState)) {
                        forwardMap.put(newState, newPath);
                        forwardQueue.offer(new SearchNode(newState, newPath));
                        
                        // Check if backward has this state
                        if (backwardMap.containsKey(newState)) {
                            String backPath = backwardMap.get(newState);
                            String solution = newPath + invertMoveSequence(backPath);
                            
                            // VERIFY the solution before returning
                            if (verifySolution(cube, solution)) {
                                return solution;
                            }
                        }
                    }
                    
                    tempCube = stringToCube(node.state);
                }
                
                nodesExplored++;
            }
            
            // Expand backward one level
            int backwardSize = backwardQueue.size();
            for (int i = 0; i < backwardSize; i++) {
                if (checkTimeout()) {
                    return null;
                }
                
                SearchNode node = backwardQueue.poll();
                if (node == null) continue;
                
                RubiksCube tempCube = stringToCube(node.state);
                
                for (char move : ALL_MOVES) {
                    if (shouldPruneMove(move, node.path)) {
                        continue;
                    }
                    
                    tempCube.applyMoves(String.valueOf(move));
                    String newState = tempCube.toString();
                    String newPath = node.path + move;
                    
                    if (!backwardMap.containsKey(newState)) {
                        backwardMap.put(newState, newPath);
                        backwardQueue.offer(new SearchNode(newState, newPath));
                        
                        // Check if forward has this state
                        if (forwardMap.containsKey(newState)) {
                            String forwardPath = forwardMap.get(newState);
                            String solution = forwardPath + invertMoveSequence(newPath);
                            
                            // VERIFY the solution before returning
                            if (verifySolution(cube, solution)) {
                                return solution;
                            }
                        }
                    }
                    
                    tempCube = stringToCube(node.state);
                }
                
                nodesExplored++;
            }
            
            // Early termination if search spaces get too large
            if (forwardMap.size() > 300000 || backwardMap.size() > 300000) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Verify that a solution actually solves the cube
     */
    private boolean verifySolution(RubiksCube originalCube, String solution) {
        RubiksCube testCube = stringToCube(originalCube.toString());
        testCube.applyMoves(solution);
        return testCube.isSolved();
    }
    
    /**
     * Invert a move sequence
     * For a Rubik's cube, each move is its own inverse when done 3 times
     * But we need to reverse the order and invert each move
     */
    private String invertMoveSequence(String moves) {
        StringBuilder inverted = new StringBuilder();
        
        // Go through moves in reverse order
        for (int i = moves.length() - 1; i >= 0; i--) {
            char move = moves.charAt(i);
            // Each move inverted is doing it 3 times (or equivalently, doing it backwards)
            // For our purposes: F' = FFF, but since we only have clockwise moves
            // We need to do the move 3 times to invert it
            inverted.append(move).append(move).append(move);
        }
        
        return inverted.toString();
    }
    
    /**
     * Advanced move pruning - avoid redundant sequences
     * MINIMAL PRUNING - only prune moves that are clearly redundant
     */
    private boolean shouldPruneMove(char move, String path) {
        if (path.length() == 0) return false;
        
        char lastMove = path.charAt(path.length() - 1);
        
        // Only prune if same move appears 4+ times in a row
        // (since UUUU = identity, doing it 4 times does nothing)
        if (move == lastMove) {
            int count = 1;
            for (int i = path.length() - 1; i >= 0 && path.charAt(i) == move; i--) {
                count++;
            }
            // Allow up to 3 repetitions, prune the 4th
            if (count >= 3) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Convert string to cube
     */
    private RubiksCube stringToCube(String state) {
        try {
            RubiksCube cube = new RubiksCube();
            String[] lines = state.split("\n");
            java.lang.reflect.Field field = RubiksCube.class.getDeclaredField("cube");
            field.setAccessible(true);
            field.set(cube, lines);
            return cube;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create cube", e);
        }
    }
    
    public int getNodesExplored() {
        return nodesExplored;
    }
    
    /**
     * Search node for BFS
     */
    private static class SearchNode {
        String state;
        String path;
        
        SearchNode(String state, String path) {
            this.state = state;
            this.path = path;
        }
    }
}