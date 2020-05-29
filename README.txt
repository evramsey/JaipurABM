Ramsey, E. and Berglund, E.Z. (2020) Developing An Agent-Based Model of Dual-Flush Toilet Adoption. Publication Pending.

The model requires the following libraries:
Multi-Agent Simulation of Networks (MASON)	<https://cs.gmu.edu/~eclab/projects/mason/#Download>
GraphStream 					<http://graphstream-project.org/download/>
Java Universal Network/Graph Framework (JUNG)	<http://jung.sourceforge.net/download.html>
Java Development Kit (JDK) 1.8			<https://www.oracle.com/java/technologies/javase-jdk8-downloads.html>
Apache Commons Math 3.6				<https://commons.apache.org/proper/commons-math/download_math.cgi>
Agape						<http://www.univ-orleans.fr/lifo/software/Agape/javadoc/index.html?agape/tools/package-tree.html>

1) Import all these files as external libraries.
2) Set ModelRunner.java at the main class.
3) The parameters used within the model and input/output directories are all set from ModelRunner, line 13-15. The variables that must be passed to the model are:
	i.) average number of connections between agents
	ii.) parameter A, for similar behavior
	ii.) parameter B, for dissimilar behavior
	iii.) parameter beta, which sets the stochasticity of the model
	iv.) exogenous term - other
	v.) exogenous term - drought
	vi.) number of skipped communication steps
	vii.) number of skipped utility update steps
	viii.) number of jobs to run (to average outputs across)
	ix.) population file location
	x.) output file name
	xi.) delete auto-generated file - leave false (used for parameterization simplicity)
	xii.) integer value designating network structure: 1 is random, 2 is random with social networks,3 is Watts-Strogatz small world
4) Run ModelRunner.java once parameters are set.

Hints for broader application:

1) The model is currently set to collect values of S, the standard error of the regression. To change it to the Nash-Sutcliffe Efficiency or the modified Nash-Sutcliffe Efficiency (Krause et al 2005), change Line 130 in dftABM.java to return "nse" or "mnse", respectively.
2) The model initializes all new agents as non-adopters/non-conservers. To change the percentage of conserving agents present at beginning of the simulation, change Line 346 of dftABM.java to the desired number (from 0 to 1). 
3) Survey results are hardcoded in DataCollector.java at lines 126-176. To apply to a different case study, this must be replaced.
4) Drought results are also hardcoded in DataCollector.kava, from Lines 259-286. To apply to a different case study, this must also be replaced. 
