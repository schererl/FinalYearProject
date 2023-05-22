import app.StartDesktopApp;
import kilothon.Kilothon;
import manager.ai.AIRegistry;
import personalAgents.MCTS;
import personalAgents.SHOT;
import personalAgents.UCTSQRTSH;

import personalAgents.MCTSLudii;

public class LaunchLudii {

	public static void main(final String[] args) {

		final int ITERATIONS = 10000;
		AIRegistry.registerAI("UCT-SCHERER", () -> {
			//return new MCTS(ITERATIONS, false, false);
			return new MCTS(ITERATIONS);
		}, (game) -> {
			return true;
		});
		AIRegistry.registerAI("UCT_SQRTSH", () -> {
			return new UCTSQRTSH(ITERATIONS, false);
		}, (game) -> {
			return true;
		});
		
		
		// AIRegistry.registerAI("UCT-CBT", () -> {
		// 	return new MCTS(false, true);
		// }, (game) -> {
		// 	return true;
		// });

		// AIRegistry.registerAI("UCT_SQRTSH-CBT", () -> {
		// 	return new UCTSQRTSH(true, true);
		// }, (game) -> {
		// 	return true;
		// });
	
		AIRegistry.registerAI("SHOT", () -> {
			return new SHOT(ITERATIONS, false);
		}, (game) -> {
			return true;
		});

		AIRegistry.registerAI("LudiiUCT", () -> {
			return new MCTSLudii(ITERATIONS);
		}, (game) -> {
			return true;
		});

		// AIRegistry.registerAI("SHOT-CBT", () -> {
		// 	return new SHOT(true);
		// }, (game) -> {
		// 	return true;
		// });

		// if (args.length > 1) {
		// 	String agent = args[0];
		// 	String fileName = args[1] + ".csv";
		// 	Kilothon.main(new String[] { "victorputrich", agent, fileName });
		// } else {
		// 	Kilothon.main(new String[] { "victorputrich", "UCT_SQRTSH-CBT", "UCT_SQRTSH-CBT.csv" });
		// }
		StartDesktopApp.main(new String[0]);

	}
}
