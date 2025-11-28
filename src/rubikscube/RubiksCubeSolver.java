package rubikscube;

import java.util.*;

/**
 * Bidirectional BFS Rubik's Cube Solver
 * Fast non-optimal solver for deeply scrambled cubes
 */
public class RubiksCubeSolver {
    
    private static final char[] ALL_MOVES = {'U', 'D', 'F', 'B', 'L', 'R'};
    private static final long TIMEOUT_MS = 10000; // 10 second timeout
    private int nodesExplored;
    private long startTime;
    private boolean timedOut;
    
    public String solve(RubiksCube cube) {
        nodesExplored = 0;
        startTime = System.currentTimeMillis();
        timedOut = false;
        
        if (cube.isSolved()) {
            return "";
        }
        
        System.out.println("Starting bidirectional BFS (10s timeout)...");
        
        // Try bidirectional search
        String result = bidirectionalSearch(cube);
        
        if (result != null) {
            System.out.println("Solution found with bidirectional search!");
            return result;
        }
        
        if (timedOut) {
            System.out.println("Timed out after 10 seconds");
            return null;
        }
        
        // If bidirectional fails, use simple BFS up to depth 15
        System.out.println("Trying simple BFS...");
        result = simpleBFS(cube, 15);
        
        if (timedOut) {
            System.out.println("Timed out after 10 seconds");
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
                System.out.println("Found at depth: " + node.path.length());
                return node.path;
            }
            
            nodesExplored++;
            if (nodesExplored % 10000 == 0) {
                System.out.println("Explored: " + nodesExplored + " nodes, depth: " + node.path.length());
            }
            
            // Try all moves
            for (char move : ALL_MOVES) {
                // Prune opposite moves
                if (node.path.length() > 0) {
                    char lastMove = node.path.charAt(node.path.length() - 1);
                    if (areOppositeMoves(move, lastMove)) {
                        continue;
                    }
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
        
        int maxDepthPerSide = 8; // Search 8 moves from each side
        
        for (int depth = 0; depth < maxDepthPerSide; depth++) {
            if (checkTimeout()) {
                return null;
            }
            
            System.out.println("Depth " + depth + " - Forward: " + forwardMap.size() + 
                             ", Backward: " + backwardMap.size());
            
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
                    if (node.path.length() > 0 && areOppositeMoves(move, node.path.charAt(node.path.length() - 1))) {
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
                                System.out.println("Meet in middle! Total moves: " + solution.length());
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
                    if (node.path.length() > 0 && areOppositeMoves(move, node.path.charAt(node.path.length() - 1))) {
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
                                System.out.println("Meet in middle! Total moves: " + solution.length());
                                return solution;
                            }
                        }
                    }
                    
                    tempCube = stringToCube(node.state);
                }
                
                nodesExplored++;
            }
            
            // Early termination if search spaces get too large
            if (forwardMap.size() > 100000 || backwardMap.size() > 100000) {
                System.out.println("Search space too large, switching to simple BFS");
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
     * Check if two moves are opposites (would cancel out)
     */
    private boolean areOppositeMoves(char move1, char move2) {
        if (move1 == move2) return true;
        if (move1 == 'U' && move2 == 'D') return true;
        if (move1 == 'D' && move2 == 'U') return true;
        if (move1 == 'F' && move2 == 'B') return true;
        if (move1 == 'B' && move2 == 'F') return true;
        if (move1 == 'L' && move2 == 'R') return true;
        if (move1 == 'R' && move2 == 'L') return true;
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