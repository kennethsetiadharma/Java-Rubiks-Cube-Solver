package rubikscube;

import java.io.*;
import java.util.*;

/**
 * Rubik's Cube representation using cubies (corner + edge pieces).
 * This file:
 *  - loads scramble01.txt in the Assignment 1 format
 *  - converts the 54 stickers into a cubie-level representation
 *  - prints all cubies with their orientations
 *
 * This is ONLY for verifying your cube representation is correct
 * before writing moves / search / solver.
 *
 * Faces: (assignment standard)
 *   0–8   = Up
 *   9–17  = Left
 *   18–26 = Front
 *   27–35 = Right
 *   36–44 = Back
 *   45–53 = Down
 *
 * Cubies stored in fixed order so solved state = identity.
 */

public class RubiksCube {

    // --- Face colors ---
    // NEW – matches assignment picture
    static final char F = 'W';   // Front  = White
    static final char B = 'Y';   // Back   = Yellow
    static final char R = 'B';   // Right  = Blue
    static final char L = 'G';   // Left   = Green
    static final char U = 'O';   // Up     = Orange
    static final char D = 'R';   // Down   = Red

    // --- Cubie indices ---
    // Corners (0..7)
    // Using classic orientation: UFR = 0, URB = 1, UBL = 2, ULF = 3, DFL = 4, DFR = 5, DRB = 6, DBL = 7
    static final String[] CORNER_NAMES = {
            "UFR", "URB", "UBL", "ULF",
            "DFL", "DFR", "DRB", "DBL"
    };

    // Edges (0..11)
    // Using classic ordering: UF, UR, UB, UL, FR, BR, BL, FL, DF, DR, DB, DL
    static final String[] EDGE_NAMES = {
            "UF","UR","UB","UL",
            "FR","BR","BL","FL",
            "DF","DR","DB","DL"
    };

    // Data structures
    int[] cornerPerm = new int[8];
    int[] cornerOri  = new int[8];   // 0..2

    int[] edgePerm = new int[12];
    int[] edgeOri  = new int[12];    // 0..1

    // Stickers
    char[] s = new char[54];

    public RubiksCube() {
        // Initialize to solved cube
        for (int i = 0; i < 8; i++) { cornerPerm[i] = i; cornerOri[i] = 0; }
        for (int i = 0; i < 12; i++) { edgePerm[i] = i; edgeOri[i] = 0; }
    }

    // ------------------------
    // FILE INPUT (scramble01.txt)
    // ------------------------
    public void loadAssignment1Format(String filename) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null)
                if (!line.trim().isEmpty())
                    lines.add(line);
        }

        /*
            Expected layout (as in your scramble):
               XXX
               XXX
               XXX
            XXXXX XXXXX XXXXX
            XXXXX XXXXX XXXXX
            XXXXX XXXXX XXXXX
               XXX
               XXX
               XXX
        */

        if (lines.size() != 9)
            throw new RuntimeException("Invalid cube net: expected 9 lines.");

        // Map into 54 stickers
        // Up face (indices 0–8)
        copyRow(lines.get(0), 3, s, 0);
        copyRow(lines.get(1), 3, s, 3);
        copyRow(lines.get(2), 3, s, 6);

        // Middle band: L F R B  (rows 3,4,5 in file)
        copyRow(lines.get(3), 0, s, 9);   // L row 1  (9–11)
        copyRow(lines.get(3), 3, s, 12);  // F row 1  (12–14)
        copyRow(lines.get(3), 6, s, 15);  // R row 1  (15–17)
        copyRow(lines.get(3), 9, s, 18);  // B row 1  (18–20)

        copyRow(lines.get(4), 0, s, 21);  // L row 2  (21–23)
        copyRow(lines.get(4), 3, s, 24);  // F row 2  (24–26)
        copyRow(lines.get(4), 6, s, 27);  // R row 2  (27–29)
        copyRow(lines.get(4), 9, s, 30);  // B row 2  (30–32)

        copyRow(lines.get(5), 0, s, 33);  // L row 3  (33–35)
        copyRow(lines.get(5), 3, s, 36);  // F row 3  (36–38)
        copyRow(lines.get(5), 6, s, 39);  // R row 3  (39–41)
        copyRow(lines.get(5), 9, s, 42);  // B row 3  (42–44)

        // Down face (indices 45–53)
        copyRow(lines.get(6), 3, s, 45);
        copyRow(lines.get(7), 3, s, 48);
        copyRow(lines.get(8), 3, s, 51);

        computeCubiesFromStickers();
    }

    private void copyRow(String line, int start, char[] arr, int dest) {
        for (int i = 0; i < 3; i++)
            arr[dest + i] = line.charAt(start + i);
    }

    // -------------------------------------------
    // Convert 54 stickers → cubies (the important part)
    // -------------------------------------------
    private void computeCubiesFromStickers() {
        // Corner sticker positions (UFR, URB, UBL, ULF, DFL, DFR, DRB, DBL)
        int[][] cornerStickerIdx = {
                { 8,  26,  27 }, // UFR
                { 2,  27,  36 }, // URB
                { 0,  44,   9 }, // UBL
                { 6,  18,   9 }, // ULF
                { 45, 18,  15 }, // DFL
                { 47, 26,  33 }, // DFR
                { 53, 36,  33 }, // DRB
                { 53, 44,  15 }  // DBL
        };

        // Edge sticker positions (UF, UR, UB, UL, FR, BR, BL, FL, DF, DR, DB, DL)
        int[][] edgeStickerIdx = {
                { 7,  25 },     // UF
                { 5,  28 },     // UR
                { 1,  37 },     // UB
                { 3,  10 },     // UL
                { 23, 26 },     // FR
                { 30, 39 },     // BR
                { 41, 12 },     // BL
                { 19, 13 },     // FL
                { 46, 22 },     // DF
                { 50, 29 },     // DR
                { 52, 38 },     // DB
                { 48, 16 }      // DL
        };

        // Reset
        Arrays.fill(cornerPerm, -1);
        Arrays.fill(edgePerm, -1);

        // CORNERS
        for (int i = 0; i < 8; i++) {
            char a = s[cornerStickerIdx[i][0]];
            char b = s[cornerStickerIdx[i][1]];
            char c = s[cornerStickerIdx[i][2]];

            int id = findCornerID(a, b, c);
            int ori = cornerOrientation(a, b, c, id);

            cornerPerm[i] = id;
            cornerOri[i] = ori;
        }

        // EDGES
        for (int i = 0; i < 12; i++) {
            char a = s[edgeStickerIdx[i][0]];
            char b = s[edgeStickerIdx[i][1]];

            int id = findEdgeID(a, b);
            int ori = edgeOrientation(a, b, id);

            edgePerm[i] = id;
            edgeOri[i] = ori;
        }
    }

    // ---------- Helper tables for cubie recognition ----------
    static final char[][] CORNER_COLORS = {
        {U, F, R},  // UFR  (O, W, B)
        {U, R, B},  // URB  (O, B, Y)
        {U, B, L},  // UBL  (O, Y, G)
        {U, L, F},  // ULF  (O, G, W)

        {D, F, L},  // DFL  (R, W, G)
        {D, F, R},  // DFR  (R, W, B)  
        {D, R, B},  // DRB  (R, B, Y)
        {D, B, L}   // DBL  (R, Y, G)  
    };


    static final char[][] EDGE_COLORS = {
        {U, F},  // O,W
        {U, R},  // O,B
        {U, B},  // O,Y
        {U, L},  // O,G
        {F, R},  // W,B
        {B, R},  // Y,B
        {B, L},  // Y,G
        {F, L},  // W,G
        {D, F},  // R,W
        {D, R},  // R,B
        {D, B},  // R,Y
        {D, L}   // R,G
    };

    private int findCornerID(char a, char b, char c) {
        char[] set = {a, b, c};
        Arrays.sort(set);

        for (int i = 0; i < 8; i++) {
            char[] cc = CORNER_COLORS[i].clone();
            Arrays.sort(cc);
            if (Arrays.equals(set, cc))
                return i;
        }
        throw new RuntimeException("Unknown corner: " + a + b + c);
    }

    private int cornerOrientation(char a, char b, char c, int id) {
        char[] target = CORNER_COLORS[id];
        if (a == target[0]) return 0;
        if (b == target[0]) return 1;
        return 2;
    }

    private int findEdgeID(char a, char b) {
        char[] set = {a, b};
        Arrays.sort(set);
        for (int i = 0; i < 12; i++) {
            char[] cc = EDGE_COLORS[i].clone();
            Arrays.sort(cc);
            if (Arrays.equals(set, cc))
                return i;
        }
        throw new RuntimeException("Unknown edge: " + a + b);
    }

    private int edgeOrientation(char a, char b, int id) {
        char[] t = EDGE_COLORS[id];
        return (a == t[0] ? 0 : 1);
    }

    // ---------------------------
    // PRINT FOR VERIFICATION
    // ---------------------------
    public void printCubies() {
        System.out.println("=== CORNERS ===");
        for (int i = 0; i < 8; i++) {
            System.out.printf("%s : perm=%d ori=%d\n", CORNER_NAMES[i], cornerPerm[i], cornerOri[i]);
        }
        System.out.println("\n=== EDGES ===");
        for (int i = 0; i < 12; i++) {
            System.out.printf("%s : perm=%d ori=%d\n", EDGE_NAMES[i], edgePerm[i], edgeOri[i]);
        }
    }

    // MOVE FUNCTIONS
    private void cycle4(int[] arr, int a, int b, int c, int d) {
        int t = arr[a];
        arr[a] = arr[d];
        arr[d] = arr[c];
        arr[c] = arr[b];
        arr[b] = t;
    }

    public void moveU() {
        cycle4(cornerPerm, 0, 1, 2, 3);
        cycle4(cornerOri, 0, 1, 2, 3);

        cycle4(edgePerm, 0, 1, 2, 3);
        cycle4(edgeOri, 0, 1, 2, 3);
    }

    public void moveD() {
        cycle4(cornerPerm, 4, 5, 6, 7);
        cycle4(cornerOri, 4, 5, 6, 7);

        cycle4(edgePerm, 8, 9, 10, 11);
        cycle4(edgeOri, 8, 9, 10, 11);
    }

    public void moveR() {
        // Permute corners
        cycle4(cornerPerm, 0, 1, 6, 5);

        // Rotate corner orientations
        cornerOri[0] = (cornerOri[0] + 2) % 3;
        cornerOri[1] = (cornerOri[1] + 1) % 3;
        cornerOri[6] = (cornerOri[6] + 2) % 3;
        cornerOri[5] = (cornerOri[5] + 1) % 3;

        // Permute edges
        cycle4(edgePerm, 1, 5, 9, 4);

        // Edge orientations do NOT flip on R
    }

    public void moveL() {
        cycle4(cornerPerm, 3, 2, 7, 4);

        cornerOri[3] = (cornerOri[3] + 1) % 3;
        cornerOri[2] = (cornerOri[2] + 2) % 3;
        cornerOri[7] = (cornerOri[7] + 1) % 3;
        cornerOri[4] = (cornerOri[4] + 2) % 3;

        cycle4(edgePerm, 3, 6, 11, 7);
    }

    public void moveF() {
        int[] cp = cornerPerm.clone();
        int[] co = cornerOri.clone();

        // Corners: 0→3→4→5
        cornerPerm[0] = cp[3]; cornerOri[0] = (co[3] + 1) % 3;
        cornerPerm[3] = cp[4]; cornerOri[3] = (co[4] + 2) % 3;
        cornerPerm[4] = cp[5]; cornerOri[4] = (co[5] + 1) % 3;
        cornerPerm[5] = cp[0]; cornerOri[5] = (co[0] + 2) % 3;

        cycle4(edgePerm, 0, 7, 8, 4);

        edgeOri[0] ^= 1;
        edgeOri[7] ^= 1;
        edgeOri[8] ^= 1;
        edgeOri[4] ^= 1;
    }

    public void moveB() {
        int[] cp = cornerPerm.clone();
        int[] co = cornerOri.clone();

        // corners: 1→2→7→6
        cornerPerm[1] = cp[2]; cornerOri[1] = (co[2] + 2) % 3;
        cornerPerm[2] = cp[7]; cornerOri[2] = (co[7] + 1) % 3;
        cornerPerm[7] = cp[6]; cornerOri[7] = (co[6] + 2) % 3;
        cornerPerm[6] = cp[1]; cornerOri[6] = (co[1] + 1) % 3;

        cycle4(edgePerm, 2, 5, 10, 6);

        edgeOri[2] ^= 1;
        edgeOri[5] ^= 1;
        edgeOri[10] ^= 1;
        edgeOri[6] ^= 1;
    }

    public void applyMove(char m) {
        switch (m) {
            case 'U': moveU(); break;
            case 'D': moveD(); break;
            case 'L': moveL(); break;
            case 'R': moveR(); break;
            case 'F': moveF(); break;
            case 'B': moveB(); break;
            default: throw new IllegalArgumentException("Invalid move: " + m);
        }
    }

    // converting cubies to sticker for visualization
    public char[] toStickers() {
        char[] out = new char[54];
        Arrays.fill(out, '?');

        int[][] cornerIdx = {
            {8, 20, 27},   // 0: UFR
            {2, 29, 36},   // 1: URB
            {0, 38, 9},   // 2: UBL
            {6, 11, 18},    // 3: ULF

            {45, 24, 17},  // 4: DFL
            {47, 26, 33},  // 5: DFR
            {53, 35, 42},  // 6: DRB
            {51, 44, 15}   // 7: DBL
        };

        // Fill corners
        for (int pos = 0; pos < 8; pos++) {
            int cubie = cornerPerm[pos];
            int ori = cornerOri[pos];

            char[] cols = CORNER_COLORS[cubie];

            out[cornerIdx[pos][0]] = cols[(0 + ori) % 3];  // U or D
            out[cornerIdx[pos][1]] = cols[(1 + ori) % 3];  // F or B
            out[cornerIdx[pos][2]] = cols[(2 + ori) % 3];  // R or L
        }

        // --- EDGE sticker positions ---
        int[][] edgeIdx = {
            {7, 19},     // 0: UF
            {5, 28},     // 1: UR
            {1, 37},     // 2: UB
            {3, 10},     // 3: UL

            {23, 30},    // 4: FR
            {39, 32},    // 5: BR
            {41, 14},    // 6: BL
            {21, 12},    // 7: FL

            {46, 25},    // 8: DF
            {50, 34},    // 9: DR
            {52, 43},    // 10: DB
            {48, 16}     // 11: DL
        };

        
        // Fill edges
        for (int pos = 0; pos < 12; pos++) {
            int cubie = edgePerm[pos];
            int ori = edgeOri[pos];

            char[] cols = EDGE_COLORS[cubie];

            out[edgeIdx[pos][0]] = cols[ori];     // the sticker on face0
            out[edgeIdx[pos][1]] = cols[1 - ori]; // the sticker on face1
        }

        // --- NOW set the 6 center stickers ---
        out[4]  = U;  // Up center
        out[13] = L;  // Left center
        out[22] = F;  // Front center
        out[31] = R;  // Right center
        out[40] = B;  // Back center
        out[49] = D;  // Down center

    return out;
    }


    // PRINT
    public void printStickerNet() {
        char[] t = toStickers();

        // Up 0–8
        System.out.printf("   %c%c%c\n", t[0], t[1], t[2]);
        System.out.printf("   %c%c%c\n", t[3], t[4], t[5]);
        System.out.printf("   %c%c%c\n", t[6], t[7], t[8]);

        // Middle band Left–Front–Right–Back
        for (int r = 0; r < 3; r++) {
            System.out.printf("%c%c%c", t[9 + 3*r], t[10 + 3*r], t[11 + 3*r]);
            System.out.printf("%c%c%c", t[18 + 3*r], t[19 + 3*r], t[20 + 3*r]);
            System.out.printf("%c%c%c", t[27 + 3*r], t[28 + 3*r], t[29 + 3*r]);
            System.out.printf("%c%c%c\n", t[36 + 3*r], t[37 + 3*r], t[38 + 3*r]);
        }

        // Down 45–53
        System.out.printf("   %c%c%c\n", t[45], t[46], t[47]);
        System.out.printf("   %c%c%c\n", t[48], t[49], t[50]);
        System.out.printf("   %c%c%c\n", t[51], t[52], t[53]);
    }

    public static void main(String[] args) {
        RubiksCube cube = new RubiksCube();

        System.out.println("Solved cube:");
        cube.printStickerNet();

        cube.applyMove('F');

        System.out.println("\nAfter F:");
        cube.printStickerNet();
    }


    // ---------------------------
    // // MAIN for testing
    // // ---------------------------
    // public static void main(String[] args) {
    // RubiksCubeCubies cube = new RubiksCubeCubies();

    // // DO NOT load any scramble file
    // // Just test the default solved cube representation
    // System.out.println("Testing cube representation on SOLVED state:\n");
    // cube.printCubies();
    // }

}
