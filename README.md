# GraduateThesis

**Student: Victor Scherer Putrich (PUCRS)**

Advisor: Felipe Rech Meneguzzi (Aberdeen University/PUCRS)

co-advisor: Anderson Rocha Tavares (UFRGS)

This repository holds the work done on General Game Playing (GGP) for my final year project at PUCRS, and BRACIS publication.

# OVERVIEW

The focus of this work is to propose and evaluate two strategies aimed at improving move suggestions in time-constrained GGP settings:

1- Hybrid UCT Strategy: This is a hybrid version of UCT that prioritizes information acquisition in the root node over spending excessive time on immediate reward maximization.

2- Adapted SHOT Algorithm: This is an adaptation of the SHOT algorithm that is tailored to GGP environments. It uses time instead of a fixed number of playouts.

Additionally, we introduce clock bonus time ($cbt$) method, aimed at enhancing time estimation for unknown environments. An empirical evaluation, following the GGP competition scheme from the Ludii framework, showcases that our strategies augment the average payoff across the full competition set of games.

## AGENTS
This repository includes implementations of four different agents, all of which are capable of interacting with the Ludii environment (the Ludii jar is available in the 'lib' folder).

* MCTS Ludii: A Ludii UCT agent instantiated with a fixed number of playouts.
* MCTS: Our own MCTS implementation.
* SHOT: An adjusted SHOT using a warmup model to define the budget based on thinking time.
* UCTSQRTSH ($\text{UCT}_{\sqrt{sh}}$): A variant of the simple regret + cumulative regret proposed in my thesis.

## BRACIS 2023

For running Kilothon trials, uncomment the following lines:

``` Java
    if (args.length > 1) {
		String agent = args[0];
		String fileName = args[1] + ".csv";
		Kilothon.main(new String[] { "victorputrich", agent, fileName });
	} else {
		Kilothon.main(new String[] { "victorputrich", "UCT_SQRTSH-CBT", "UCT_SQRTSH-CBT.csv" });
	}
```
Make sure to comment out the following line:

``` Java
StartDesktopApp.main(new String[0]);
```

Please note that each Kilothon run take, on average, 18 hours for UCT and $\text{UCT}_{\sqrt{sh}}$, and 20 hours for both using $cbt$.

### AGENT COMPARISON

If you want to compare our agent with others, you can do using Ludii graphical interface. You can search for the game you want to test at 'load game' menu, searching for a game name (we encourage you to search for starting 'Nogo', 'AtariGo', 'Breakthrough', 'Amazons', and 'Pentalath')

Set our agent in the 'Player' component of the Ludii application. Note that our agent's thinking time cannot be changed within this interface. You'll need to adjust the THINKING_TIME attribute for UCTSQRTSH and _UCT within the code. Then, navigate to the Analysis menu and Compare Agents, specifying the number of games you want them to play.



