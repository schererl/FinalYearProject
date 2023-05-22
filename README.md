# GraduateThesis

**Student: Victor Scherer Putrich (PUCRS)**

Advisor: Felipe Rech Meneguzzi (Aberdeen University)

Acknowledgment (co-advisor): Anderson Rocha Tavares (UFRGS)

General Game Playing (GGP) is a challenging domain for AI agents, as it requires them to play diverse games without prior knowledge. 
We present two strategies to improve move suggestions in time-constrained GGP settings. 
The first strategy consists of a hybrid version of UCT, favoring information acquisition in the root node, rather than overspend time on the best immediate reward.
Second is an adaptation of SHOT algorithm to fit a GGP environment using time instead of a fixed number of playouts.
We also present a clock bonus time method for improving time estimation for unkown environemnts.
Empirical evaluation using the GGP competition scheme from the Ludii framework shows that our strategy improves the average payoff over the entire competition set of games.

*The paper about my work will be available soon*

There are implementation of 4 different agents available to interact with Ludii environment. (Ludii jar is available at lib folder)

* MCTS Ludii: Instantiation of Ludii UCT agent with a fixed number of playouts
* MCTS: Our MCTS implementation
* SHOT: Modified SHOT using warmup model to define the budget to use, based on thinking time.
* UCTSQRTSH: Variation of simple regret + cumulative regret proposed at my thesis. 

The 'LaunchLudii' file starts Ludii environemnt (for running kilothon use Kilothon.main.

- The Clock Bonus Time method is not fully available on these version due to some modifications I made for running performance comparisons against UCT on 5 distinct games. 
- If you want to use it, uncomment the <name_agent>-CBT methods at the LaunchLudii file and make pay attention to possible unexpected behavior.
