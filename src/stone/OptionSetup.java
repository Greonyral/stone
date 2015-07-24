package stone;

import stone.io.IOHandler;
import stone.util.Flag;
import stone.util.OptionContainer;
import stone.util.Path;


/**
 * An object holding all relevant objects for initializing options
 * 
 * @author Nelphindal
 */
public class OptionSetup {
	boolean initDone;
	OptionContainer optionContainer;
	Flag flags;
	Path workingDirectory;
	IOHandler io;
	MasterThread master;
	boolean jar;

	OptionSetup() {
		this.io = new IOHandler("Nelphi's Tool");
	}

	/**
	 * @return the IO-handler
	 */
	public final IOHandler getIO() {
		return this.io;
	}

	/**
	 * @return the master thread
	 */
	public final MasterThread getMaster() {
		return this.master;
	}

	/**
	 * @return the dir, where the class files are or the jar-archive containing
	 *         them
	 */
	public final Path getWorkingDir() {
		return this.workingDirectory;
	}

	/**
	 * @return true if {@link #getWorkingDir()} returns the path of an
	 *         jar-archive, false otherwise
	 */
	public final boolean wdIsJarArchive() {
		return this.jar;
	}
}
