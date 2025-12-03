# Java-Rubiks-Cube-Solver
A project designing a rubik's cube solver in Java using a 3 layered solver system. 
1. Bidirectional BFS
2. IDA* 
3. Simple BFS

To compile:

include a scramble.txt file with the same cube net format in the sample testcases folder.

cd into src and run these:

```bash
javac rubikscube/*.java       
java rubikscube.Solver ../testcases/scramble-number-.txt ../mysol.txt
```

Work still yet to be fully optimized. 