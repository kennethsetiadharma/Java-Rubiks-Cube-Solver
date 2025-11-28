package rubikscube;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class RubiksCube {

    private String[] cube;

    /**
     * default constructor
     * Creates a Rubik's Cube in an initial state:
     *    OOO
     *    OOO
     *    OOO
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     *    RRR
     *    RRR
     *    RRR
     */
    public RubiksCube() {
        cube = new String[9];
        cube[0] = "   OOO";
        cube[1] = "   OOO";
        cube[2] = "   OOO";
        cube[3] = "GGGWWWBBBYYY";
        cube[4] = "GGGWWWBBBYYY";
        cube[5] = "GGGWWWBBBYYY";
        cube[6] = "   RRR";
        cube[7] = "   RRR";
        cube[8] = "   RRR";
    }

    /**
     * @param fileName
     * @throws IOException
     * @throws IncorrectFormatException
     * Creates a Rubik's Cube from the description in fileName
     */
    public RubiksCube(String fileName) throws IOException, IncorrectFormatException {
        cube = new String[9];

        // System.out.println("Looking for file: " + new java.io.File(fileName).getAbsolutePath());

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))){
            for (int i = 0; i < 9; i++) {
                String line = br.readLine(); // a line from the file

                if (line == null) throw new IncorrectFormatException("File ended before 9 lines");

                // Validating textfile format
                if ((i >= 0 && i <= 2) || (i >= 6 && i <= 8)) {
                    if (line.length() != 6) {
                        throw new IncorrectFormatException("Line " + (i + 1) + "should have 6 lines.");
                    }
                }

                if (i >= 3 && i <= 5 && line.length() != 12) {
                    throw new IncorrectFormatException("Line " + (i + 1) + "should have 12 lines.");
                }
                
                cube[i] = line;
            }   
            if (br.readLine() != null) throw new IncorrectFormatException("File has more than 9 lines");
        }
    }

    /**
     * @param moves
     * Applies the sequence of moves on the Rubik's Cube
     */
    public void applyMoves(String moves) {

        for (int i = 0; i < moves.length(); i++) {
            char move = moves.charAt(i);

            switch (move) {
                case 'F': {
                    // Rotate front face
                    rotateFaceClockwise(3, 3);
                    // Rotate surrounding edges
                    rotateEdges(
                        new int[][]{{2, 3}, {2, 4}, {2, 5}},   // top
                        new int[][]{{3, 6}, {4, 6}, {5, 6}},   // right
                        new int[][]{{6, 5}, {6, 4}, {6, 3}},   // bottom
                        new int[][]{{5, 2}, {4, 2}, {3, 2}}    // left
                    );
                    break;
                }

                case 'B': {
                    rotateFaceClockwise(3, 9);
                    rotateEdges(
                        new int[][]{{0, 5}, {0, 4}, {0, 3}},  // top
                        new int[][]{{3, 0}, {4, 0}, {5, 0}},    // right
                        new int[][]{{8, 3}, {8, 4}, {8, 5}},  // bottom
                        new int[][]{{5, 8}, {4, 8}, {3, 8}}     // left
                    );
                    break;
                }

                case 'R': {
                    rotateFaceClockwise(3, 6);
                    rotateEdges(
                        new int[][]{{2, 5}, {1, 5}, {0, 5}},    // top
                        new int[][]{{3, 9}, {4, 9}, {5, 9}},    // right
                        new int[][]{{8, 5}, {7, 5}, {6, 5}},    // bottom
                        new int[][]{{5, 5}, {4, 5}, {3, 5}}     // left
                    );
                    break;
                }

                case 'L': {
                    rotateFaceClockwise(3, 0);
                    rotateEdges(
                        new int[][]{{0, 3}, {1, 3}, {2, 3}},    // top
                        new int[][]{{3, 3}, {4, 3}, {5, 3}},    // right
                        new int[][]{{6, 3}, {7, 3}, {8, 3}},    // bottom
                        new int[][]{{5, 11}, {4, 11}, {3, 11}}     // left
                    );
                    break;
                }

                case 'U': {
                    rotateFaceClockwise(0, 3);
                    rotateEdges(
                        new int[][]{{3, 9}, {3, 10}, {3, 11}},   // top
                        new int[][]{{3, 6}, {3, 7}, {3, 8}},   // right
                        new int[][]{{3, 3}, {3, 4}, {3, 5}}, // bottom
                        new int[][]{{3, 0}, {3, 1}, {3, 2}}    // left
                    );
                    break;
                }

                case 'D': {
                    rotateFaceClockwise(6, 3);
                    rotateEdges(
                        new int[][]{{5, 3}, {5, 4}, {5, 5}},   // top
                        new int[][]{{5, 6}, {5, 7}, {5, 8}},   // right
                        new int[][]{{5, 9}, {5, 10}, {5, 11}}, // bottom
                        new int[][]{{5, 0}, {5, 1}, {5, 2}}    // left
                    );
                    break;
                }
            }
        }
    }

    /**
     * returns true if the current state of the Cube is solved,
     * i.e., it is in this state:
     *    OOO
     *    OOO
     *    OOO
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     * GGGWWWBBBYYY
     *    RRR
     *    RRR
     *    RRR
     */
    public boolean isSolved() {
        // Create a new default "solved" cube
        RubiksCube solvedCube = new RubiksCube();
        return this.toString().equals(solvedCube.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cube.length; i++) {
            sb.append(cube[i]);
            sb.append('\n'); // add newline after each row
        }
        return sb.toString();
    }

    /**
     *
     * @param moves
     * @return the order of the sequence of moves
     */
    public static int order(String moves) {
        // 1. Start with a solved cube (initial state)
        RubiksCube cube = new RubiksCube();
        String initialState = cube.toString();
        
        // 2. Apply moves once
        cube.applyMoves(moves);
        int n = 1;

        // 3. Keep applying moves until the cube returns to the initial state
        while (!cube.toString().equals(initialState)) {
            cube.applyMoves(moves);
            n++;
        }
        return n;
    }

    // === HELPER FUNCTIONS === 

    // Replace one character in a string (since Strings are immutable in Java)
    private String replaceChar(String s, int index, char c) {
        return s.substring(0, index) + c + s.substring(index + 1);
    }

    // Rotate a 3x3 face in place (given top-left corner coordinates in the cube)
    private void rotateFaceClockwise(int startRow, int startCol) {
        char[][] temp = new char[3][3];

        // Copy face
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                temp[i][j] = cube[startRow + i].charAt(startCol + j);
            }
        }

        // Write rotated back into cube
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cube[startRow + i] = replaceChar(
                    cube[startRow + i],
                    startCol + j,
                    temp[2 - j][i]
                );
            }
        }
    }

    // Rotate 4 edge strips (each strip = 3 coordinates [row, col]) safely
    private void rotateEdges(int[][] top, int[][] right, int[][] bottom, int[][] left) {
        // save originals
        char[] topVal = new char[3];
        char[] rightVal = new char[3];
        char[] bottomVal = new char[3];
        char[] leftVal = new char[3];

        for (int i = 0; i < 3; i++) {
            topVal[i] = cube[top[i][0]].charAt(top[i][1]);
            rightVal[i] = cube[right[i][0]].charAt(right[i][1]);
            bottomVal[i] = cube[bottom[i][0]].charAt(bottom[i][1]);
            leftVal[i] = cube[left[i][0]].charAt(left[i][1]);
        }

        // write rotated values (clockwise): top <- left, right <- top, bottom <- right, left <- bottom
        for (int i = 0; i < 3; i++) {
            cube[top[i][0]] = replaceChar(cube[top[i][0]], top[i][1], leftVal[i]);
        }
        for (int i = 0; i < 3; i++) {
            cube[right[i][0]] = replaceChar(cube[right[i][0]], right[i][1], topVal[i]);
        }
        for (int i = 0; i < 3; i++) {
            cube[bottom[i][0]] = replaceChar(cube[bottom[i][0]], bottom[i][1], rightVal[i]);
        }
        for (int i = 0; i < 3; i++) {
            cube[left[i][0]] = replaceChar(cube[left[i][0]], left[i][1], bottomVal[i]);
        }
    }
}