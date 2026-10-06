package com.example.data.model

data class SampleDocument(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val pageCount: Int,
    val content: String
)

object SampleDocumentProvider {
    val samples = listOf(
        SampleDocument(
            id = "bio_101",
            title = "Cell Biology & Cellular Respiration",
            category = "Biology",
            description = "Covers cell organelles, ATP synthesis, glycolysis, the citric acid cycle, and mitochondrial membranes.",
            pageCount = 4,
            content = """
                CHAPTER 4: CELLULAR ENERGETICS AND METABOLISM

                1. Overview of Cellular Respiration
                Cellular respiration is the biochemical process through which eukaryotic and prokaryotic cells convert chemical energy from nutrient molecules (primarily glucose, C6H12O6) into adenosine triphosphate (ATP), while releasing metabolic waste products. The overall balanced chemical equation is:
                C6H12O6 + 6 O2 -> 6 CO2 + 6 H2O + ~30 to 32 ATP.

                2. Stage 1: Glycolysis
                Glycolysis occurs exclusively in the cytosol (cytoplasm) of the cell and does not require oxygen (it is an anaerobic pathway). During glycolysis, a six-carbon molecule of glucose is split through an enzymatic cascade into two three-carbon molecules of pyruvate. Glycolysis consists of an energy investment phase requiring 2 ATP, and an energy payoff phase yielding 4 ATP and 2 NADH molecules. Therefore, the net gain of glycolysis is 2 ATP (via substrate-level phosphorylation) and 2 NADH.

                3. The Pyruvate Oxidation and Krebs Cycle (Citric Acid Cycle)
                Pyruvate is actively transported into the mitochondrial matrix. There, pyruvate dehydrogenase converts pyruvate into Acetyl-CoA, producing 1 NADH and releasing 1 molecule of CO2 per pyruvate.
                The Citric Acid Cycle takes place inside the mitochondrial matrix. Acetyl-CoA (2 carbons) combines with oxaloacetate (4 carbons) to form citrate (6 carbons). For each glucose molecule (2 acetyl groups), the Krebs cycle turns twice, generating 2 ATP/GTP, 6 NADH, 2 FADH2, and 4 CO2 molecules.

                4. Stage 4: Oxidative Phosphorylation and the Electron Transport Chain (ETC)
                Oxidative phosphorylation occurs along the inner mitochondrial membrane (cristae). High-energy electrons donated by NADH and FADH2 are transferred along a series of multi-protein complexes (Complexes I through IV).
                As electrons cascade down energy gradients, hydrogen ions (protons, H+) are pumped across the inner membrane from the matrix into the intermembrane space, creating a steep electrochemical proton gradient known as the proton motive force.
                Finally, protons flow back into the mitochondrial matrix through ATP synthase via chemiosmosis, driving the rotational phosphorylation of ADP into ATP. Molecular oxygen (O2) acts as the terminal electron acceptor, binding free protons to yield water (H2O). If oxygen is absent, the electron transport chain backs up, oxidative phosphorylation ceases, and the cell is forced into anaerobic fermentation (producing lactic acid in animals or ethanol in yeast).

                5. Key Organelles
                - Mitochondria: Double-membraned powerhouses of eukaryotic cells. The inner membrane is heavily folded into cristae to maximize surface area for electron transport chains.
                - Endoplasmic Reticulum (Rough and Smooth): Rough ER is studded with ribosomes for protein synthesis; Smooth ER handles lipid synthesis and detoxification.
                - Golgi Apparatus: Modifies, sorts, and packages proteins for secretion or delivery to lysosomes.
            """.trimIndent()
        ),
        SampleDocument(
            id = "cs_algorithms",
            title = "Data Structures & Algorithmic Complexity",
            category = "Computer Science",
            description = "Detailed guide on Big-O analysis, Hash Tables, Binary Search Trees, and Graph Traversal algorithms.",
            pageCount = 5,
            content = """
                DATA STRUCTURES & ALGORITHM DESIGN SPECIFICATION

                1. Asymptotic Notation (Big-O Complexity)
                Big-O notation describes the upper bound of the execution time or space requirement of an algorithm as the input size n approaches infinity.
                - O(1): Constant time (e.g., hash table lookup on average, array index access).
                - O(log n): Logarithmic time (e.g., Binary Search on a sorted array).
                - O(n): Linear time (e.g., linear search, single pass through an array).
                - O(n log n): Linearithmic time (e.g., Merge Sort, Heap Sort, average Quick Sort).
                - O(n^2): Quadratic time (e.g., Bubble Sort, Insertion Sort, nested loops).

                2. Arrays and Linked Lists
                - Arrays provide contiguous memory storage. Reading an element by index takes O(1) time. However, inserting or deleting an arbitrary element takes O(n) time due to element shifting.
                - Singly and Doubly Linked Lists store non-contiguous nodes linked by pointers. Inserting at the head takes O(1) time, but random index lookup requires traversing pointers in O(n) time.

                3. Hash Tables and Collision Resolution
                A hash table maps keys to values using a hashing function. In the ideal case, lookups, insertions, and deletions take O(1) time.
                When two distinct keys hash to the same bucket index, a collision occurs. Common resolution strategies include:
                - Separate Chaining: Each bucket holds a linked list or self-balancing tree of colliding entries.
                - Open Addressing (Linear/Quadratic Probing): Probes successive memory slots in the array until an empty cell is found.

                4. Trees and Binary Search Trees (BST)
                A Binary Search Tree maintains the invariant that for any node X, all values in its left subtree are strictly less than X, and all values in its right subtree are strictly greater than X.
                In a balanced BST (such as an AVL Tree or Red-Black Tree), search, insertion, and deletion take O(log n) time. In a degenerate (skewed) BST resembling a linked list, operations degrade to O(n).

                5. Graph Algorithms
                Graphs consist of vertices (V) and edges (E).
                - Breadth-First Search (BFS): Uses a FIFO Queue; explores neighbor nodes level by level; optimal for finding shortest paths in unweighted graphs. Time complexity: O(V + E).
                - Depth-First Search (DFS): Uses a LIFO Stack or recursion; traverses down branches before backtracking; widely used for topological sorting and cycle detection. Time complexity: O(V + E).
                - Dijkstra's Algorithm: Uses a min-priority heap to find shortest paths from a single source in graphs with non-negative edge weights. Time complexity: O((V + E) log V).
            """.trimIndent()
        ),
        SampleDocument(
            id = "history_indrev",
            title = "The Industrial Revolution & Global Economy",
            category = "History",
            description = "Covers mechanization, the steam engine, textile mills, urban migration, and 19th century social reforms.",
            pageCount = 3,
            content = """
                THE INDUSTRIAL REVOLUTION (1760 - 1850)

                1. Origins in Great Britain
                The Industrial Revolution originated in Great Britain during the mid-18th century due to a unique combination of geographic, economic, and political factors:
                - Abundant natural resources: Plentiful domestic deposits of iron ore and coal, especially in northern England, Wales, and Scotland.
                - Agricultural Revolution: Innovations like the four-field crop rotation system (Charles Townshend) and Jethro Tull's seed drill increased food production while reducing labor needs, creating an available urban workforce.
                - Capital and Global Trade: British colonial merchant networks provided raw materials (such as American and Indian cotton) and captive export markets.

                2. Key Technological Innovations
                - The Steam Engine: Improved decisively by James Watt in 1769 with a separate condenser, making steam power fuel-efficient and adaptable for driving factory machinery beyond riverbanks.
                - Textile Mechanization: John Kay's Flying Shuttle (1733), James Hargreaves' Spinning Jenny (1764), and Richard Arkwright's Water Frame (1769) radically multiplied yarn and cloth output.
                - Smelting with Coke: Abraham Darby pioneered smelting iron using coke (purified coal) instead of charcoal, drastically lowering the cost of high-grade pig iron.

                3. Socio-Economic Consequences and Urbanization
                The transition from home-based cottage industries to centralized factory systems accelerated rapid urbanization. Cities like Manchester, Birmingham, and Leeds swelled in population without adequate sanitation, clean water, or housing infrastructure, leading to outbreaks of cholera and typhus.
                Factory conditions were characterized by 12-to-16-hour shifts, six days a week, hazardous machinery without safety guards, and widespread child labor.

                4. Social Movements and Legislative Reforms
                - The Luddite Movement (1811-1816): Artisanal textile weavers smashed mechanical looms in protest against wage depression and deskilling.
                - Factory Act of 1833: British Parliament restricted daily working hours for children and mandated factory inspectors.
                - Labor Unions and Chartism: Workers organized mutual aid societies and trade unions to demand universal male suffrage, secret ballots, and improved workplace protections.
            """.trimIndent()
        )
    )
}
