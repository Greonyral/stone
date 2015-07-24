package stone.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import stone.Config;
import stone.MasterThread;
import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.io.OutputStream;
import stone.modules.versionControl.CommitComparator;
import stone.modules.versionControl.NoYesPlugin;
import stone.modules.versionControl.SecretKeyPlugin;
import stone.modules.versionControl.StagePlugin;
import stone.util.BooleanOption;
import stone.util.Debug;
import stone.util.Flag;
import stone.util.MaskedStringOption;
import stone.util.Option;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.StringOption;

/**
 * The class handling all interaction with the jgit library
 * 
 * @author Nelphindal
 */
public final class VersionControl implements Module {

	private final static int VERSION = 12;

	private final static String SECTION = Main.VC_SECTION;

	private static final String URL_HTTPS_KEY = "url_https";

	private static final String DEFAULT_GIT_URL_SSH = stone.Config
			.getInstance().getValue("url_ssh");

	private static final String DEFAULT_GIT_URL_HTTPS = stone.Config
			.getInstance().getValue(URL_HTTPS_KEY);

	private static final String AES_KEY = "aes-key";

	private final static String tmpBranchName = "tmpMerge_pull";

	private static final BooleanOption createBooleanOption(
			final OptionContainer oc, final String key, boolean defaultValue,
			final String label, final String tooltip, boolean store) {
		return VersionControl.createBooleanOption(oc, key, Flag.NoShortFlag,
				key, defaultValue, label, tooltip, store);
	}

	private static final BooleanOption createBooleanOption(
			final OptionContainer oc, final String key, char shortFlag,
			final String longFlag, boolean defaultValue, final String tooltip,
			final String label, boolean store) {
		return new BooleanOption(oc, VersionControl.SECTION + key, tooltip,
				label, shortFlag, longFlag, VersionControl.SECTION, store ? key
						: null, defaultValue);
	}

	private static final MaskedStringOption createPwdOption(
			final OptionContainer oc, final String key, final String tooltip,
			final String description) {
		return new MaskedStringOption(oc, VersionControl.SECTION + key,
				description, tooltip, Flag.NoShortFlag, Flag.NoLongFlag,
				VersionControl.SECTION, key);
	}

	private static final StringOption createStringOption(
			final OptionContainer oc, final String key,
			final String defaultValue, char shortFlag, final String longFlag,
			final String helpText) {
		return new StringOption(oc, VersionControl.SECTION + key, helpText,
				helpText, shortFlag, longFlag, VersionControl.SECTION, key,
				defaultValue);
	}

	private static final StringOption createStringOption(
			final OptionContainer oc, final String key,
			final String defaultValue, final String tooltip, final String label) {
		return new StringOption(oc, VersionControl.SECTION + key, tooltip,
				label, Flag.NoShortFlag, Flag.NoLongFlag,
				VersionControl.SECTION, key, defaultValue);
	}

	private final StringOption USERNAME, EMAIL, BRANCH, GIT_URL_HTTPS,
			GIT_URL_SSH, VC_DIR;

	private final MaskedStringOption PWD;

	private final BooleanOption DIFF, COMMIT, RESET, USE_SSH;

	private final IOHandler io;

	private final MasterThread master;

	private final Path base, repoRoot;

	private final Main main;

	private long start;

	/**
	 * Constructor for building versionInfo
	 */
	public VersionControl() {
		this.io = null;
		this.GIT_URL_SSH = null;
		this.GIT_URL_HTTPS = null;
		this.PWD = null;
		this.EMAIL = null;
		this.USERNAME = null;
		this.BRANCH = null;
		this.COMMIT = null;
		this.DIFF = null;
		this.RESET = null;
		this.USE_SSH = null;
		this.VC_DIR = null;
		this.master = null;
		this.repoRoot = this.base = null;
		this.main = null;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param sc -
	 * @throws InterruptedException -
	 */
	public VersionControl(final StartupContainer sc)
			throws InterruptedException {
		final OptionContainer oc;
		while (sc.getOptionContainer() == null) {
			synchronized (sc) {
				sc.wait();
			}
		}
		oc = sc.getOptionContainer();
		this.io = sc.getIO();
		this.GIT_URL_SSH = VersionControl.createStringOption(oc, "url_ssh",
				VersionControl.DEFAULT_GIT_URL_SSH, Flag.NoShortFlag,
				"git-url-ssh",
				"Changes the url to use when using ssh to connect");
		this.GIT_URL_HTTPS = VersionControl.createStringOption(oc, "url_https",
				VersionControl.DEFAULT_GIT_URL_HTTPS, Flag.NoShortFlag,
				"git-url-https",
				"Changes the url to use when using https to connect");
		this.PWD = VersionControl
				.createPwdOption(oc, "github-pwd", "Password at github",
						"Changes the password to login at github");
		this.EMAIL = VersionControl.createStringOption(oc, "email", null,
				"Changes the email supplied as part of commit messages",
				"Email");
		this.USERNAME = VersionControl.createStringOption(oc, "login", null,
				"The login name at remote repository, default github",
				"Username");
		this.BRANCH = VersionControl.createStringOption(oc, "branch", "master",
				Flag.NoShortFlag, "branch",
				"Changes the working branch to work on");
		this.COMMIT = VersionControl
				.createBooleanOption(
						oc,
						"commit",
						'c',
						"commit",
						false,
						"Commits changes in local repository und uploads them to remote repository",
						"Commit", false);
		this.DIFF = VersionControl.createBooleanOption(oc, "diff", false,
				"Displays differences after downloading changes", "Show diffs",
				false);
		this.RESET = VersionControl
				.createBooleanOption(
						oc,
						"reset",
						false,
						"Discards uncommited changes and unlocks any locks created by git clients. The working branch will be set to default branch.",
						"RESET", false);
		this.USE_SSH = VersionControl
				.createBooleanOption(
						oc,
						"use_ssh",
						Flag.NoShortFlag,
						"use-ssh",
						false,
						"Uses ssh protocol for connections. This option should be used only on Unix",
						"SSH", true);
		this.VC_DIR = VersionControl
				.createStringOption(
						oc,
						"repo",
						"Music/band",
						Flag.NoShortFlag,
						"repo",
						"Changes the location of the local repository, its relative to the path in section "
								+ Main.GLOBAL_SECTION
								+ " at key "
								+ Main.PATH_KEY);
		this.master = sc.getMaster();
		this.main = sc.getMain();
		this.repoRoot = this.base = null;
	}

	private VersionControl(final VersionControl vc) {
		this.main = vc.main;
		final String baseValue = this.main.getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		this.io = vc.io;
		this.GIT_URL_SSH = vc.GIT_URL_SSH;
		this.GIT_URL_HTTPS = vc.GIT_URL_HTTPS;
		this.RESET = vc.RESET;
		this.EMAIL = vc.EMAIL;
		this.USERNAME = vc.USERNAME;
		this.BRANCH = vc.BRANCH;
		this.PWD = vc.PWD;
		this.DIFF = vc.DIFF;
		this.COMMIT = vc.COMMIT;
		this.USE_SSH = vc.USE_SSH;
		this.VC_DIR = vc.VC_DIR;
		this.master = vc.master;
		this.base = Path.getPath(baseValue.split("/")).resolve("Music");
		this.repoRoot = this.base.resolve(this.main.getConfigValue(
				Main.VC_SECTION, Main.REPO_KEY, "band").split("/"));
	}

	/**
	 * 
	 * @param source input
	 * @param target output
	 * @param encrypt encrypt else decrypt
	 * @return <i>target</i>
	 */
	public final String encrypt(final String source, final String target,
			boolean encrypt) {
		if (target == null) {
			if (encrypt) {
				return encrypt(source,
						"enc/" + source.substring(0, source.length() - 8)
								+ ".abc", true);
			}
			throw new IllegalArgumentException("target is null");
		}
		final AESEngine engine = new AESEngine();
		final String savedKey = this.main.getConfigValue(Main.VC_SECTION,
				VersionControl.AES_KEY, null);
		final byte[] key;
		if (savedKey == null) {
			final SecretKeyPlugin secretKeyPlugin = new SecretKeyPlugin();
			this.io.handleGUIPlugin(secretKeyPlugin);
			key = secretKeyPlugin.getKey();
			this.main.setConfigValue(Main.VC_SECTION, VersionControl.AES_KEY,
					secretKeyPlugin.getValue());
		} else {
			key = SecretKeyPlugin.decode(savedKey);
		}
		final KeyParameter keyParam;
		keyParam = new KeyParameter(key);
		if (savedKey == null) {
			this.main.flushConfig();
		}
		final Path input = this.repoRoot.resolve(source.split("/"));
		final Path output = this.repoRoot.resolve(target.split("/"));
		if (input.equals(output)) {
			final Path tmp = Path.getTmpDirOrFile("");
			encrypt(source, tmp.toString(), encrypt);
			input.delete();
			tmp.toFile().renameTo(input.toFile());
		}

		output.getParent().toFile().mkdirs();
		final byte[] bufferIn = new byte[engine.getBlockSize()];
		final byte[] bufferOut = new byte[engine.getBlockSize()];
		final InputStream streamIn = this.io.openIn(input.toFile());
		final OutputStream streamOut = this.io.openOut(output.toFile());
		engine.init(encrypt, keyParam);
		streamIn.registerProgressMonitor(this.io);
		if (encrypt) {
			this.io.setProgressTitle("Encrypting " + output.getFilename());
		} else {
			this.io.setProgressTitle("Decrypting " + output.getFilename());
		}
		try {
			while (true) {
				final int read;
				if ((read = streamIn.read(bufferIn)) < 0) {
					break;
				}
				if ((read < bufferIn.length) && encrypt) {
					for (int i = read; i < bufferIn.length; i++) {
						bufferIn[i] = ' ';
					}
				}
				engine.processBlock(bufferIn, 0, bufferOut, 0);
				this.io.write(streamOut, bufferOut);
			}
			return target;
		} catch (final IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			this.io.close(streamIn);
			this.io.close(streamOut);
			this.io.endProgress("");
		}
	}

	/** */
	@Override
	public final List<Option> getOptions() {
		final List<Option> list = new ArrayList<>();
		list.add(this.EMAIL);
		if (!this.USE_SSH.getValue()) {
			list.add(this.USERNAME);
			list.add(this.PWD);
		}
		list.add(this.DIFF);
		list.add(this.COMMIT);
		list.add(this.RESET);
		return list;
	}

	/** */
	@Override
	public final int getVersion() {
		return VersionControl.VERSION;
	}

	/** */
	@Override
	public final Module init(final StartupContainer sc) {
		return new VersionControl(this);
	}

	/**
	 * /* Deletes any meta-data created by this tool-box.
	 */
	@Override
	public final void repair() {
		final String baseValue = this.main.getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		if (baseValue == null) {
			System.out
					.println("Unable to determine base - The local repository could not been deleted");
			return;
		}
		final Path base_ = Path.getPath(baseValue.split("/")).resolve("Music");
		final Path repoRoot_ = base_.resolve(this.main.getConfigValue(
				Main.VC_SECTION, Main.REPO_KEY, "band"));
		if (repoRoot_.exists()) {
			final NoYesPlugin plugin = new NoYesPlugin(
					"Delete local repository?",
					repoRoot_
							+ "\nand all its contents will be deleted. You can\n"
							+ "answer with NO and delete only the data used for git",
					this.io.getGUI(), false, "");
			synchronized (this.io) {
				this.io.handleGUIPlugin(plugin);
			}
			if (plugin.get()) {
				final boolean success = repoRoot_.delete();
				System.out.printf("Delet%s %s%s\n", success ? "ed" : "ion of",
						repoRoot_.toString(), success ? "" : " failed");
			}
		}
	}

	/**
	 * Runs the bulletin board and the synchronizer for the local repository.
	 */
	@Override
	public final void run() {
		if (this.master.isInterrupted()) {
			return;
		}

		final String name = this.main.getConfigValue(Main.GLOBAL_SECTION,
				Main.NAME_KEY, null);
		final Git gitSession_band;

		final String branch = this.BRANCH.value();
		final boolean ssh = this.USE_SSH.getValue();
		final String remoteURL = (ssh ? this.GIT_URL_SSH : this.GIT_URL_HTTPS)
				.value();
		try {
			if (!this.repoRoot.resolve(".git").exists()) {
				checkoutBand();
				final long end = System.currentTimeMillis();
				System.out.println("needed "
						+ stone.util.Time.delta(end - this.start)
						+ " for clone");
				if (!this.repoRoot.resolve(".git").exists()) {
					gitSession_band = null;
				} else {
					gitSession_band = Git.open(this.repoRoot.toFile());
				}
			} else {
				Git git = null;
				try {
					git = Git.open(this.repoRoot.toFile());
				} catch (final Exception e) {
					if (!this.RESET.getValue()) {
						this.io.printError("failed to open the repository",
								false);
					}

				}
				gitSession_band = git;
			}

			reset(gitSession_band);

			if (gitSession_band != null) {
				if (this.COMMIT.getValue()) {
					if (this.EMAIL.value() == null) {
						this.io.printError(
								"For commits a valid email is needed", false);
						return;
					}
					if ((name == null) || name.isEmpty()) {
						this.io.printError(
								"For commits a valid name is needed", false);
						return;
					}
					if (!this.USE_SSH.getValue()) {
						if (this.PWD.value() == null) {
							this.io.printError(
									"For commits a valid password is needed",
									false);
							return;
						}
						if (this.USERNAME.value() == null) {
							this.io.printError(
									"For commits a valid username is needed",
									false);
							return;
						}
					}
				}
				final StoredConfig config = gitSession_band.getRepository()
						.getConfig();
				config.setString("user", null, "name", name);
				config.setString("user", null, "email", this.EMAIL.value());
				if (!this.RESET.getValue()) {
					config.setString("branch", branch, "merge", "refs/heads/"
							+ branch);
					config.setString("branch", branch, "remote", "origin");
					config.setString("remote", "origin", "url", remoteURL);
				}
				config.save();
			}
		} catch (final JGitInternalException | IOException | GitAPIException
				| InterruptedException e) {
			this.io.handleException(ExceptionHandle.CONTINUE, e);
			return;
		}
		try {
			if (this.master.isInterrupted()) {
				return;
			}
			gotoBand(gitSession_band);
		} finally {
			if (gitSession_band != null) {
				gitSession_band.close();
			}
		}
	}

	/*
	 * ask for changes to commit, and invokes upload if pending commit(s) are
	 * there.
	 */
	private final void checkForLocalChanges(final Git gitSession)
			throws IOException, GitAPIException, InterruptedException {
		final Status status = gitSession.status().call();
		stone.util.Debug.print("modified: %s\n" + "untracked: %s\n"
				+ "missing: %s\n" + "added: %s\n" + "changed: %s\n"
				+ "removed %s\n", status.getModified().toString(), status
				.getUntracked().toString(), status.getMissing().toString(),
				status.getAdded().toString(), status.getChanged().toString(),
				status.getRemoved().toString());
		final DirCache cache =  gitSession.getRepository().readDirCache();
		final StagePlugin stage = new StagePlugin(status, cache,
				this.COMMIT.getValue(), this.master);
		this.io.handleGUIPlugin(stage);
		if (stage.doCommit(gitSession, this)) {
			final RevCommit commitRet = commit(gitSession);

			this.io.printMessage(
					null,
					"commit: "
							+ commitRet.getFullMessage()
							+ "\nStarting to upload changes after checking remote repository for changes",
					false);
		}
		if (this.master.isInterrupted()) {
			return;
		}
		update(gitSession);
		if (stage.doCommit(gitSession, this)) {
			push(gitSession);
		}
	}

	private final void checkoutBand() {
		final NoYesPlugin plugin = new NoYesPlugin("Local repository "
				+ this.repoRoot.getFilename() + " does not exist",
				Main.formatMaxLength(this.repoRoot, null, "The directory ",
						" does not exist or is no git-repository.\n")
						+ "It can take a while to create it. Continue?",
				this.io.getGUI(), false, "Checking out");
		this.io.handleGUIPlugin(plugin);
		this.start = System.currentTimeMillis();
		if (!plugin.get()) {
			return;
		}
		this.repoRoot.getParent().toFile().mkdirs();
		try {
			Git.init().setDirectory(this.repoRoot.toFile()).call();
		} catch (final GitAPIException e) {
			this.io.handleException(ExceptionHandle.CONTINUE, e);
			return;
		}

		try {
			final Git gitSession = Git.open(this.repoRoot.toFile());
			final StoredConfig config = gitSession.getRepository().getConfig();
			config.setString("remote", "origin", "url",
					VersionControl.DEFAULT_GIT_URL_HTTPS);
			config.setString("branch", this.BRANCH.value(), "remote",
					this.BRANCH.value());
			config.setString("branch", this.BRANCH.value(), "merge",
					"+refs/heads/" + this.BRANCH.value());
			config.save();

			final ObjectId remoteHead = getRemoteHead(gitSession);
			final DiffCommand diffCommand = gitSession.diff();
			final List<DiffEntry> diffs;

			// set head
			gitSession.reset().setMode(ResetType.SOFT)
					.setRef(remoteHead.getName()).call();
			diffCommand.setCached(true);
			diffCommand.setShowNameAndStatusOnly(true);

			diffs = diffCommand.call();

			this.io.startProgress("checking out", diffs.size());

			for (final DiffEntry diff : diffs) {
				if (diff.getChangeType() != ChangeType.DELETE) {
					this.io.setProgressTitle("checking out ...");
					this.io.updateProgress(1);
					continue;
				}
				final String file0 = diff.getOldPath();
				final String file;
				if (file0.startsWith("enc")) {
					file = file0.substring(4) + ".abc";
					encrypt(file0, file, false);
				} else {
					file = file0;
				}
				this.io.setProgressTitle("checking out " + file);
				// unstage to make checkout working
				gitSession.reset().setRef(remoteHead.getName()).addPath(file)
						.call();
				final CheckoutCommand checkout = gitSession.checkout().addPath(
						file);
				final boolean existing = this.repoRoot.resolve(file).exists();
				if (existing) {
					final String old = this.repoRoot.resolve(file)
							.createBackup("_old");
					if (old == null) {
						this.io.printError("failed to checkout " + old, true);
						this.io.updateProgress(1);
						continue;
					}
					this.io.printError(
							String.format("%-40s renamed to %s\n", file, old),
							true);
				}
				checkout.call();

				this.io.updateProgress(1);
			}

			this.io.endProgress("Checkout done");
		} catch (final GitAPIException | IOException | InterruptedException e) {
			this.repoRoot.resolve(".git").delete();
			this.io.handleException(ExceptionHandle.CONTINUE, e);
			return;
		}
	}

	private final RevCommit commit(final Git gitSession)
			throws NoHeadException, NoMessageException, UnmergedPathsException,
			ConcurrentRefUpdateException, WrongRepositoryStateException,
			GitAPIException {
		final CommitCommand commit = gitSession.commit();

		commit.setAuthor(this.main.getConfigValue(Main.GLOBAL_SECTION,
				Main.NAME_KEY, null), this.EMAIL.value());
		commit.setMessage("update " + commit.getAuthor().getName() + ", "
				+ new Date(System.currentTimeMillis()));

		return commit.call();
	}

	private final String diff(final RevWalk walk, final RevCommit commitOld,
			final RevCommit commitNew, final Git gitSession)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException, GitAPIException {
		final ObjectReader reader = gitSession.getRepository()
				.newObjectReader();
		final RevTree treeOld;
		final RevTree treeNew;
		final CanonicalTreeParser treeParserOld = new CanonicalTreeParser();
		final CanonicalTreeParser treeParserNew = new CanonicalTreeParser();

		treeOld = walk.parseTree(commitOld.getTree().getId());
		treeNew = walk.parseTree(commitNew.getTree().getId());

		treeParserOld.reset(reader, treeOld.getId());
		treeParserNew.reset(reader, treeNew.getId());

		final List<DiffEntry> diffs = gitSession.diff()
				.setOldTree(treeParserOld).setNewTree(treeParserNew)
				.setShowNameAndStatusOnly(true).call();
		final StringBuilder sbHead = new StringBuilder(), sbBody = new StringBuilder();
		int adds = 0, copies = 0, dels = 0, mods = 0, renames = 0;
		final Set<String> encodedDeleted = new HashSet<>();
		final Set<String> encodedChanged = new HashSet<>();
		final Set<String> encodedCreated = new HashSet<>();
		for (final DiffEntry diff : diffs) {
			final String file, symbol;
			switch (diff.getChangeType()) {
			case ADD:
				++adds;
				symbol = "+  ";
				file = diff.getNewPath();
				if (diff.getNewPath().startsWith("enc/")) {
					encodedCreated.add(diff.getNewPath());
				}
				break;
			case COPY:
				++copies;
				symbol = "o-o";
				file = diff.getOldPath() + " -> " + diff.getNewPath();
				if (diff.getNewPath().startsWith("enc/")) {
					encodedCreated.add(diff.getNewPath());
				}
				break;
			case DELETE:
				++dels;
				symbol = "  -";
				file = diff.getOldPath();
				if (diff.getOldPath().startsWith("enc/")) {
					encodedDeleted.add(diff.getOldPath());
				}
				break;
			case MODIFY:
				++mods;
				symbol = " M ";
				file = diff.getNewPath();
				if (diff.getNewPath().startsWith("enc/")) {
					encodedChanged.add(diff.getNewPath());
				}
				break;
			case RENAME:
				++renames;
				symbol = "->";
				file = diff.getOldPath() + " -> " + diff.getNewPath();
				if (diff.getNewPath().startsWith("enc/")) {
					encodedCreated.add(diff.getNewPath());
				}
				if (diff.getOldPath().startsWith("enc/")) {
					encodedDeleted.add(diff.getOldPath());
				}
				break;
			default:
				// not existent but to make compiler happy
				continue;
			}
			sbBody.append(String.format("\n%3s  %s", symbol, file));
		}
		sbHead.append(String
				.format("new(+): %-3d deleted(-): %-3d modified(M): %-3d\n  renamed(->): %-3d copied(o-o): %-3d",
						adds, dels, mods, renames, copies));
		reader.release();
		treeParserOld.stopWalk();
		treeParserNew.stopWalk();

		for (final String created : encodedCreated) {
			gitSession.checkout().addPath(created).setStartPoint(commitNew)
					.call();
			encrypt(created, created.substring(4), false);
		}
		for (final String deleted : encodedDeleted) {
			this.repoRoot.resolve(deleted).delete();
			this.repoRoot.resolve(deleted.substring(4)).delete();
		}
		return sbHead.append(sbBody.toString()).toString();

	}

	private int executeNativeGit(final String... cmd) throws IOException,
			InterruptedException {
		final Process p = Runtime.getRuntime().exec(cmd, null,
				this.repoRoot.toFile());
		final BufferedReader rOut = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		final BufferedReader rErr = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		final int exit = p.waitFor();
		while (true) {
			final String line = rOut.readLine();
			if (line == null) {
				break;
			}
			System.out.println(line);
		}
		while (true) {
			final String line = rErr.readLine();
			if (line == null) {
				break;
			}
			System.err.println(line);
		}
		return exit;
	}

	private final ProgressMonitor getProgressMonitor() {
		final stone.io.ProgressMonitor monitor = this.io.getProgressMonitor();

		return new ProgressMonitor() {

			@Override
			public final void beginTask(final String arg0, int arg1) {
				monitor.beginTask(arg0, arg1);
			}

			@Override
			public final void endTask() {
				monitor.endProgress("");
			}

			@Override
			public final boolean isCancelled() {
				return false;
			}

			@Override
			public final void start(int arg0) {
				monitor.beginTask("", arg0);
			}

			@Override
			public final void update(int arg0) {
				monitor.update(arg0);
			}
		};
	}

	private final ObjectId getRemoteHead(final Git gitSession)
			throws InvalidRemoteException, TransportException, GitAPIException,
			IOException, InterruptedException {
		if (!this.USE_SSH.getValue()) {
			final String refS = "refs/heads/" + this.BRANCH.value();
			final FetchCommand fetch = gitSession
					.fetch()
					.setRefSpecs(
							new RefSpec(refS + ":refs/remotes/origin/"
									+ this.BRANCH.value()))
					.setProgressMonitor(getProgressMonitor());


			final Ref ref = fetch.call().getAdvertisedRef(refS);
			if (ref == null) {
				return null;
			}
			return ref.getObjectId();
		}
		final File f = this.repoRoot.resolve(".git", "FETCH_HEAD").toFile();
		final InputStream in;
		final byte[] buffer = new byte[40];
		final ObjectId id;
		if (executeNativeGit("git", "fetch", "origin") != 0) {
			return null;
		}
		in = this.io.openIn(f);
		in.read(buffer);
		this.io.close(in);
		id = ObjectId.fromString(new String(buffer));
		return id;
	}

	private final void gotoBand(final Git gitSession) {
		if (gitSession == null) {
			return;
		}
		try {
			final String branch = gitSession.getRepository().getBranch();
			if (!branch.equals(this.BRANCH.value())) {
				this.io.printMessage(
						null,
						"Not on working branch \"" + this.BRANCH.value()
								+ "\"\nCurrent branch is \"" + branch
								+ "\"\nCheckout branch \""
								+ this.BRANCH.value()
								+ "\" or work on branch \"" + branch + "\"",
						true);
				return;
			}
			if (this.master.isInterrupted()) {
				return;
			}
			checkForLocalChanges(gitSession);
		} catch (final IOException | GitAPIException | InterruptedException e) {
			this.io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}

	private final void historyRewritten(final Git gitSession,
			final RevCommit commitLocal, final RevCommit commitRemote,
			final RevWalk walk) throws NoHeadException, NoMessageException,
			UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException,
			IncorrectObjectTypeException, IOException {
		this.io.startProgress("Reset - History has been rewritten", -1);
		gitSession.branchCreate().setForce(true).setName(tmpBranchName).call();
		try {
			gitSession.reset().setRef(commitRemote.getName())
					.setMode(ResetType.SOFT).call();

			// restore committed files
			final ObjectReader reader = gitSession.getRepository()
					.newObjectReader();
			final RevTree treeOld = walk.parseTree(commitLocal.getTree()
					.getId());
			final RevTree treeNew = walk.parseTree(commitRemote.getTree()
					.getId());
			final CanonicalTreeParser treeParserOld = new CanonicalTreeParser();
			final CanonicalTreeParser treeParserNew = new CanonicalTreeParser();

			treeParserOld.reset(reader, treeOld.getId());
			treeParserNew.reset(reader, treeNew.getId());

			final List<DiffEntry> diffs = gitSession.diff()
					.setOldTree(treeParserOld).setNewTree(treeParserNew)
					.setShowNameAndStatusOnly(true).call();

			if (!diffs.isEmpty()) {
				// clear stage
				final Status status = gitSession.status().call();
				final RevCommit stashUncommitted = gitSession.commit()
						.setMessage("stash").call();
				gitSession.branchCreate().setForce(true).setName("stash")
						.call();
				gitSession.reset().setMode(ResetType.SOFT)
						.setRef(commitRemote.getName()).call();

				// reset state to last commit of old tree
				for (final DiffEntry diff : diffs) {
					switch (diff.getChangeType()) {
					case ADD:
						Debug.print("+ s\n", diff.getNewPath());
						break;
					case COPY:
						break;
					case DELETE:
						Debug.print("- %s\n", diff.getOldPath());
						gitSession.checkout().addPath(diff.getOldPath())
								.setStartPoint(commitLocal).call();
						break;
					case MODIFY:
						Debug.print("M %s\n", diff.getOldPath());
						break;
					case RENAME:
						break;
					default:
						break;
					}
				}
				commit(gitSession);

				// restore stashed files
				final Set<String> changes = new HashSet<>();
				changes.addAll(status.getAdded());
				changes.addAll(status.getChanged());
				changes.addAll(status.getRemoved());

				treeParserOld.reset(reader, commitRemote.getTree().getId());
				treeParserNew.reset(reader, stashUncommitted.getTree().getId());
				final List<DiffEntry> diffsStash = gitSession.diff()
						.setOldTree(treeParserOld).setNewTree(treeParserNew)
						.setShowNameAndStatusOnly(true).call();

				for (final DiffEntry diff : diffsStash) {
					switch (diff.getChangeType()) {
					case ADD:
						Debug.print("(+) %s\n", diff.getNewPath());
						gitSession.checkout().addPath(diff.getNewPath())
								.setStartPoint(stashUncommitted).call();
						break;
					case COPY:
						break;
					case DELETE:
						Debug.print("(-) %s\n", diff.getOldPath());
						gitSession.rm().addFilepattern(diff.getOldPath())
								.call();
						break;
					case MODIFY:
						Debug.print("(M) %s\n", diff.getOldPath());
						gitSession.reset().addPath(diff.getNewPath())
								.setRef(stashUncommitted.getName()).call();
						gitSession.checkout().addPath(diff.getNewPath())
								.setName(stashUncommitted.getName()).call();
						gitSession.add().addFilepattern(diff.getNewPath())
								.call();
						break;
					case RENAME:
						break;
					default:
						break;
					}
				}
			}
		} finally {
			// clean up
			gitSession.branchDelete().setForce(true)
					.setBranchNames(tmpBranchName, "stash").call();
			this.io.endProgress("Branch deleted");
		}
	}

	private final boolean merge(final Git gitSession, final RevWalk walk,
			final RevCommit commitLocal, final RevCommit commitRoot,
			final RevCommit commitRemote) throws IOException {
		if (commitRoot == null) {
			return true;
		}
		try {
			gitSession.branchCreate().setName(tmpBranchName).setForce(true)
					.call();
			gitSession.checkout().setName(tmpBranchName).call();
			gitSession.reset().setMode(ResetType.HARD)
					.setRef(commitRemote.getName()).call();
			// tmp branch is equal to FETCH_HEAD now

			final ObjectReader reader = gitSession.getRepository()
					.newObjectReader();
			final TreeSet<RevCommit> commits = new TreeSet<>(
					CommitComparator.instance());
			final CanonicalTreeParser treeParserNew = new CanonicalTreeParser();
			final CanonicalTreeParser treeParserOld = new CanonicalTreeParser();
			final String name = this.main.getConfigValue(Main.GLOBAL_SECTION,
					Main.NAME_KEY, null);

			int time = commitLocal.getCommitTime();
			this.io.startProgress("Merging", time - commitRoot.getCommitTime());
			final Set<String> merged = new HashSet<>();

			commits.add(commitLocal);
			boolean doCommit = false;
			while (!commits.isEmpty()) {
				// go back until the latest commit contained in both branches
				final RevCommit c = commits.pollLast();
				if (c.equals(commitRoot)) {
					this.io.updateProgress(time - c.getCommitTime());
					time = c.getCommitTime();
					continue;
				}
				walk.parseCommit(c);
				final String author = c.getAuthorIdent().getName();
				if (author.equals(name)) {
					// discard foreign commits
					treeParserNew.reset(reader, c.getTree());
					treeParserOld.reset(reader, c.getParent(0).getTree());
					final List<DiffEntry> diffs = gitSession.diff()
							.setOldTree(treeParserOld)
							.setNewTree(treeParserNew)
							.setShowNameAndStatusOnly(true).call();
					for (final DiffEntry e : diffs) {
						final String old = e.getOldPath();
						final String add = e.getNewPath();
						doCommit = true;
						switch (e.getChangeType()) {
						case RENAME:
							if (merged.add(add)) {
								gitSession.checkout()
										.setStartPoint(commitLocal)
										.addPath(add).call();
							}
							gitSession.add().addFilepattern(add).call();

							if (merged.add(old)) {
								this.repoRoot.resolve(old).delete();
							}
							gitSession.add().addFilepattern(old).call();
							break;
						case DELETE:
							if (merged.add(old)) {
								this.repoRoot.resolve(old).delete();
							}
							gitSession.add().addFilepattern(old).call();
							break;
						case ADD:
						case COPY:
						case MODIFY:
							if (merged.add(add)) {
								gitSession.checkout()
										.setStartPoint(commitLocal)
										.addPath(add).call();
							}
							gitSession.add().addFilepattern(add).call();
							break;
						default:
							break;
						}
					}
				}
				for (final RevCommit cP : c.getParents()) {
					if (cP.getCommitTime() < commitRoot.getCommitTime()) {
						continue;
					}
					commits.add(cP);
				}
				this.io.updateProgress(time - c.getCommitTime());
				time = c.getCommitTime();
			}
			reader.release();
			treeParserOld.stopWalk();
			treeParserNew.stopWalk();
			if (doCommit) {
				this.io.startProgress("Creating new commit", -1);
				gitSession
						.commit()
						.setMessage(
								"update "
										+ name
										+ ", "
										+ new Date(System.currentTimeMillis())
										+ "\n\n"
										+ "merge branch \'"
										+ this.BRANCH.value()
										+ "\' of "
										+ (this.USE_SSH.getValue() ? this.GIT_URL_SSH
												: this.GIT_URL_HTTPS).value())
						.setCommitter(name, this.EMAIL.value()).call();
			}
			gitSession.branchCreate().setForce(true)
					.setName(this.BRANCH.value()).call();
			gitSession.checkout().setName(this.BRANCH.value()).call();
			gitSession.branchDelete().setBranchNames(tmpBranchName).call();
			this.io.endProgress("Merge done");
			return true;
		} catch (final GitAPIException e) {
			e.printStackTrace();
			try {
				// reset previous state
				gitSession.checkout().setName(this.BRANCH.value()).call();
				gitSession.branchDelete().setBranchNames(tmpBranchName).call();
				this.io.printError(
						"Encountered a problem. The previous state has been recovered\n"
								+ e.getLocalizedMessage(), false);
			} catch (final GitAPIException ee) {
				this.io.printError(
						"Encountered a problem.\n"
								+ e.getLocalizedMessage()
								+ "\nRecovering from it another problem occured\n"
								+ ee.getMessage(), false);
			}
			return false;
		}
	}

	/*
	 * do the upload of commits
	 */
	private final void push(final Git gitSession) throws GitAPIException,
			IOException, InterruptedException {
		if (!this.USE_SSH.getValue()) {
			final PushCommand push = gitSession.push();
			final RefSpec ref = new RefSpec("refs/heads/"
					+ this.main.getConfigValue(VersionControl.SECTION,
							"branch", "master"));
			push.setRefSpecs(ref).setProgressMonitor(getProgressMonitor());

			final CredentialsProvider login = new UsernamePasswordCredentialsProvider(
					this.USERNAME.value(), this.PWD.value());
			push.setCredentialsProvider(login);

			push.call();
		} else {
			if (executeNativeGit("git", "push", "origin", this.BRANCH.value()
					+ ":" + this.BRANCH.value()) != 0) {
				this.io.printMessage(null, "Push (upload) failed", true);
				return;
			}
		}
		this.io.printMessage(null, "Push (upload) finished successfully", true);
	}

	private final void reset(final Git gitSession) throws GitAPIException,
			IOException, InterruptedException {
		if (this.RESET.getValue()) {
			// set options to default
			this.GIT_URL_HTTPS.value(Config.getInstance().getValue(
					URL_HTTPS_KEY));
			this.main.flushConfig();
			this.USE_SSH.setValue(false);
			this.COMMIT.setValue(false);

			final ObjectId remoteHead;
			if (gitSession == null) {
				if (!this.repoRoot.exists() || this.repoRoot.delete()) {
					this.io.printError(
							"Reset failed\n"
									+ "The tool could not rebuild missing repo information",
							false);
					checkoutBand();
					reset(Git.open(this.repoRoot.toFile()));
				} else {
					this.io.printError("Reset failed", true);
				}
				return;
			}
			// remove lock
			this.repoRoot.resolve(".git", "index.lock").delete();

			gitSession
					.getRepository()
					.getConfig()
					.setString("remote", "origin", "url",
							this.GIT_URL_HTTPS.value());
			gitSession.getRepository().getConfig().save();
			final Set<String> refs = gitSession.getRepository()
					.getAllRefs().keySet();
			if (!refs.contains("refs/heads/" + this.BRANCH.value())) {
				this.io.printMessage("Specified branch missing in config",
						"Branch \"" + this.BRANCH.value()
								+ "\" does not exist.\n"
								+ "Resetting to default \"master\".", true);
				this.BRANCH.value("master");
				final InputStream in = this.io.openIn(this.repoRoot
						.resolve(".git", "HEAD").toFile());
				final String localHead;
				if (in == null) {
					localHead = "refs/heads/master";
					final OutputStream out = this.io.openOut(this.repoRoot
							.resolve(".git", "HEAD").toFile());
					out.write("ref: refs/heads/master");
					this.io.close(out);
				} else {
					localHead = in.readLine().replace("ref: ", "");
					this.io.close(in);
				}
				if (this.repoRoot.resolve(localHead.split("/")).exists()) {
					this.io.printError(
							"Reset failed\n"
									+ "The tool could not rebuild missing repository information",
							false);
					return;
				}
			}
			remoteHead = getRemoteHead(gitSession);


			// set to remote head
			this.io.startProgress("Checking out " + remoteHead.getName(), -1);
			gitSession.checkout().setName(remoteHead.getName().substring(0, 8))
					.call();
			this.io.startProgress("Resetting local to remote head", -1);
			gitSession.reset().setRef(remoteHead.getName())
					.setMode(ResetType.HARD).call();

			// remove possible old branches - active branch and the temporarily
			// branch
			gitSession.branchDelete().setForce(true)
					.setBranchNames(this.BRANCH.value(), tmpBranchName).call();

			// create and checkout active branch
			gitSession.branchCreate().setName(this.BRANCH.value()).call();
			gitSession.checkout().setName(this.BRANCH.value()).call();
			this.io.endProgress("Reset done");
		}
	}

	/*
	 * download new songs
	 */
	private final void update(final Git gitSession) throws GitAPIException,
			IOException {
		final ObjectId remoteHead;
		try {
			remoteHead = getRemoteHead(gitSession);
			if (remoteHead == null) {
				this.io.printError("The remote branch does not exist", false);
				return;
			}
			Debug.print("Remote head: %s\n", remoteHead.getName());
		} catch (final TransportException | InterruptedException e) {
			this.io.printError(
					"Failed to contact github.com.\nCheck if you have internet access and try again.",
					false);
			return;
		}
		final ObjectId localHead = gitSession.getRepository().getRef("HEAD")
				.getObjectId();
		if (localHead == null) {
			this.io.printError("Unable to determine current head", false);
			return;
		}
		Debug.print("Local  head: %s\n", localHead.getName());
		if (remoteHead.equals(localHead)) {
			this.io.printMessage(null, "Your repository is up-to-date", true);
			return;
		}
		final RevWalk walk = new RevWalk(gitSession.getRepository());
		final RevCommit commitRemote = walk.parseCommit(remoteHead);
		final RevCommit commitLocal = walk.parseCommit(localHead);

		final String diffString;

		boolean success = true;

		final RevCommit commitRoot = CommitComparator.init(walk, this.io).getParent(commitLocal, commitRemote);
		if (commitRoot == null) {
			historyRewritten(gitSession, commitLocal, commitRemote, walk);
			diffString = null;
			this.DIFF.setValue(false);
		} else {
			diffString = diff(walk, commitRoot, commitRemote, gitSession);
		}
		try {
			success = merge(gitSession, walk, commitLocal, commitRoot,
					commitRemote);
		} catch (final Exception e) {
			throw e;
		} finally {
			walk.release();
		}

		if (this.DIFF.getValue()) {
			this.io.printMessage("Changes", diffString, true);
		}

		if (!success) {
			this.io.printMessage(null, "Update failed", true);
		} else {
			this.io.printMessage(null, "Update completed succesully", true);
		}
	}

	@Override
	public void dependingModules(final Set<String> set) {
		return;
	}
}
