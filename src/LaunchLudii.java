import app.StartDesktopApp;
import kilothon.Kilothon;
import manager.ai.AIRegistry;
import personalAgents.MCTS;
import personalAgents.SHOT;
import personalAgents.UCTSQRTSH;

import personalAgents.MCTSLudii;

public class LaunchLudii {

	public static void main(final String[] args) {
		AIRegistry.registerAI("UCT_SQRTSH", () -> {
			return new UCTSQRTSH(true, false);
		}, (game) -> {
			return true;
		});
		
		AIRegistry.registerAI("UCT_SQRTSH-CBT", () -> {
			return new UCTSQRTSH(true, true);
		}, (game) -> {
			return true;
		});


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
