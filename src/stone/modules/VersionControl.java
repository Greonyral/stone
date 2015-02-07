package stone.modules;

import java.io.IOException;
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
import org.eclipse.jgit.api.TransportConfigCallback;
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
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

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

	private final static int VERSION = 9;

	private final static String SECTION = Main.VC_SECTION;

	private static final String DEFAULT_GIT_URL_SSH = "git@github.com:Greonyral/lotro-songs.git";

	private static final String DEFAULT_GIT_URL_HTTPS = "https://github.com/Greonyral/lotro-songs.git";

	private static final String AES_KEY = "aes-key";

	private final static String tmpBranchName = "tmpMerge_pull";

	private static final BooleanOption createBooleanOption(
			final OptionContainer oc, final String key, boolean defaultValue,
			final String label, final String tooltip, boolean store) {
		return VersionControl.createBooleanOption(oc, key, Flag.NoShortFlag,
				Flag.NoLongFlag, defaultValue, label, tooltip, store);
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
		io = null;
		GIT_URL_SSH = null;
		GIT_URL_HTTPS = null;
		PWD = null;
		EMAIL = null;
		USERNAME = null;
		BRANCH = null;
		COMMIT = null;
		DIFF = null;
		RESET = null;
		USE_SSH = null;
		VC_DIR = null;
		master = null;
		repoRoot = base = null;
		main = null;
	}

	/**
	 * Creates a new instance
	 * 
	 * @param sc
	 * @throws InterruptedException
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
		io = sc.getIO();
		GIT_URL_SSH = VersionControl.createStringOption(oc, "url_ssh",
				VersionControl.DEFAULT_GIT_URL_SSH, Flag.NoShortFlag,
				"git-url-ssh",
				"Changes the url to use when using ssh to connect");
		GIT_URL_HTTPS = VersionControl.createStringOption(oc, "url_https",
				VersionControl.DEFAULT_GIT_URL_HTTPS, Flag.NoShortFlag,
				"git-url-https",
				"Changes the url to use when using https to connect");
		PWD = VersionControl
				.createPwdOption(oc, "github-pwd", "Password at github",
						"Changes the password to login at github");
		EMAIL = VersionControl.createStringOption(oc, "email", null,
				"Changes the email supplied as part of commit messages",
				"Email");
		USERNAME = VersionControl.createStringOption(oc, "login", null,
				"The login name at remote repository, default github",
				"Username");
		BRANCH = VersionControl.createStringOption(oc, "branch", "master",
				Flag.NoShortFlag, "branch",
				"Changes the working branch to work on");
		COMMIT = VersionControl
				.createBooleanOption(
						oc,
						"commit",
						'c',
						"commit",
						false,
						"Commits changes in local repository und uploads them to remote repository",
						"Commit", false);
		DIFF = VersionControl.createBooleanOption(oc, "diff", false,
				"Displays differences after downloading changes", "Show diffs",
				false);
		RESET = VersionControl
				.createBooleanOption(
						oc,
						"reset",
						false,
						"Discards uncommited changes, the working branch will be set to default branch",
						"RESET", false);
		USE_SSH = VersionControl
				.createBooleanOption(
						oc,
						"use_ssh",
						Flag.NoShortFlag,
						"use-ssh",
						false,
						"Uses ssh protocol for connections. This option should be used only on Unix",
						"SSH", true);
		VC_DIR = VersionControl
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
		master = sc.getMaster();
		main = sc.getMain();
		repoRoot = base = null;
	}

	private VersionControl(final VersionControl vc) {
		main = vc.main;
		final String baseValue = main.getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		io = vc.io;
		GIT_URL_SSH = vc.GIT_URL_SSH;
		GIT_URL_HTTPS = vc.GIT_URL_HTTPS;
		RESET = vc.RESET;
		EMAIL = vc.EMAIL;
		USERNAME = vc.USERNAME;
		BRANCH = vc.BRANCH;
		PWD = vc.PWD;
		DIFF = vc.DIFF;
		COMMIT = vc.COMMIT;
		USE_SSH = vc.USE_SSH;
		VC_DIR = vc.VC_DIR;
		master = vc.master;
		base = Path.getPath(baseValue.split("/")).resolve("Music");
		repoRoot = base.resolve(main.getConfigValue(Main.VC_SECTION,
				Main.REPO_KEY, "band").split("/"));
	}

	/** */
	@Override
	public final List<Option> getOptions() {
		final List<Option> list = new ArrayList<>();
		list.add(EMAIL);
		if (!USE_SSH.getValue()) {
			list.add(USERNAME);
			list.add(PWD);
		}
		list.add(DIFF);
		list.add(COMMIT);
		list.add(RESET);
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
		final String baseValue = main.getConfigValue(Main.GLOBAL_SECTION,
				Main.PATH_KEY, null);
		if (baseValue == null) {
			System.out
					.println("Unable to determine base - The local repository could not been deleted");
			return;
		}
		final Path base_ = Path.getPath(baseValue.split("/")).resolve("Music");
		final Path repoRoot_ = base_.resolve(main.getConfigValue(
				Main.VC_SECTION, Main.REPO_KEY, "band"));
		if (repoRoot_.exists()) {
			final NoYesPlugin plugin = new NoYesPlugin(
					"Delete local repository?",
					repoRoot_
							+ "\nand all its contents will be deleted. You can\n"
							+ "answer with NO and delete only the data used for git",
					io.getGUI(), false);
			synchronized (io) {
				io.handleGUIPlugin(plugin);
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
		if (master.isInterrupted()) {
			return;
		}

		final String name = main.getConfigValue(Main.GLOBAL_SECTION,
				Main.NAME_KEY, null);
		final Git gitSession_band;

		final String branch = BRANCH.value();
		final boolean ssh = USE_SSH.getValue();
		final String remoteURL = (ssh ? GIT_URL_SSH : GIT_URL_HTTPS).value();
		try {
			if (!repoRoot.resolve(".git").exists()) {
				checkoutBand();
				final long end = System.currentTimeMillis();
				System.out.println("needed "
						+ stone.util.Time.delta(end - start) + " for clone");
				if (!repoRoot.resolve(".git").exists()) {
					gitSession_band = null;
				} else {
					gitSession_band = Git.open(repoRoot.toFile());
				}
			} else {
				Git git = null;
				try {
					git = Git.open(repoRoot.toFile());
				} catch (final Exception e) {
					if (!RESET.getValue())
						io.printError("failed to open the repository", false);

				}
				gitSession_band = git;
			}

			reset(gitSession_band);

			if (gitSession_band != null) {
				if (COMMIT.getValue()) {
					if (EMAIL.value() == null) {
						io.printError("For commits a valid email is needed",
								false);
						return;
					}
					if ((name == null) || name.isEmpty()) {
						io.printError("For commits a valid name is needed",
								false);
						return;
					}
					if (!USE_SSH.getValue()) {
						if (PWD.value() == null) {
							io.printError(
									"For commits a valid password is needed",
									false);
							return;
						}
						if (USERNAME.value() == null) {
							io.printError(
									"For commits a valid username is needed",
									false);
							return;
						}
					}
				}
				final StoredConfig config = gitSession_band.getRepository()
						.getConfig();
				config.setString("user", null, "name", name);
				config.setString("user", null, "email", EMAIL.value());
				config.setString("branch", branch, "merge", "refs/heads/"
						+ branch);
				config.setString("branch", branch, "remote", "origin");
				config.setString("remote", "origin", "url", remoteURL);
				config.save();
			}
		} catch (final JGitInternalException | IOException | GitAPIException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
			return;
		}
		try {
			if (master.isInterrupted()) {
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
			throws IOException, GitAPIException {
		final Status status = gitSession.status().call();
		stone.util.Debug.print("modified: %s\n" + "untracked: %s\n"
				+ "missing: %s\n" + "added: %s\n" + "changed: %s\n"
				+ "removed %s\n", status.getModified().toString(), status
				.getUntracked().toString(), status.getMissing().toString(),
				status.getAdded().toString(), status.getChanged().toString(),
				status.getRemoved().toString());
		final StagePlugin stage = new StagePlugin(status, COMMIT.getValue(),
				repoRoot, master);
		io.handleGUIPlugin(stage);
		if (stage.doCommit(gitSession)) {
			final RevCommit commitRet = commit(gitSession);

			io.printMessage(
					null,
					"commit: "
							+ commitRet.getFullMessage()
							+ "\nStarting to upload changes after checking remote repository for changes",
					false);
		}
		if (master.isInterrupted())
			return;
		update(gitSession);
		if (COMMIT.getValue()) {
			push(gitSession);
		}
	}

	private final RevCommit commit(final Git gitSession)
			throws NoHeadException, NoMessageException, UnmergedPathsException,
			ConcurrentRefUpdateException, WrongRepositoryStateException,
			GitAPIException {
		final CommitCommand commit = gitSession.commit();

		commit.setAuthor(
				main.getConfigValue(Main.GLOBAL_SECTION, Main.NAME_KEY, null),
				EMAIL.value());
		commit.setMessage("update " + commit.getAuthor().getName() + ", "
				+ new Date(System.currentTimeMillis()));

		return commit.call();
	}

	private final void checkoutBand() {
		final NoYesPlugin plugin = new NoYesPlugin("Local repository "
				+ repoRoot.getFilename() + " does not exist",
				Main.formatMaxLength(repoRoot, null, "The directory ",
						" does not exist or is no git-repository.\n")
						+ "It can take a while to create it. Continue?",
				io.getGUI(), false);
		io.handleGUIPlugin(plugin);
		start = System.currentTimeMillis();
		if (!plugin.get()) {
			return;
		}
		repoRoot.getParent().toFile().mkdirs();
		try {
			Git.init().setDirectory(repoRoot.toFile()).call();
		} catch (final GitAPIException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
			return;
		}

		try {
			final Git gitSession = Git.open(repoRoot.toFile());
			final StoredConfig config = gitSession.getRepository().getConfig();
			config.setString("remote", "origin", "url",
					VersionControl.DEFAULT_GIT_URL_HTTPS);
			config.setString("branch", BRANCH.value(), "remote", BRANCH.value());
			config.setString("branch", BRANCH.value(), "merge", "+refs/heads/"
					+ BRANCH.value());
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

			io.startProgress("checking out", diffs.size());

			for (final DiffEntry diff : diffs) {
				if (diff.getChangeType() != ChangeType.DELETE) {
					io.setProgressTitle("checking out ...");
					io.updateProgress(1);
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
				io.setProgressTitle("checking out " + file);
				// unstage to make checkout working
				gitSession.reset().setRef(remoteHead.getName()).addPath(file)
						.call();
				final CheckoutCommand checkout = gitSession.checkout().addPath(
						file);
				final boolean existing = repoRoot.resolve(file).exists();
				if (existing) {
					final String old = repoRoot.resolve(file).createBackup(
							"_old");
					if (old == null) {
						io.printError("failed to checkout " + old, true);
						io.updateProgress(1);
						continue;
					}
					io.printError(
							String.format("%-40s renamed to %s\n", file, old),
							true);
				}
				checkout.call();

				io.updateProgress(1);
			}

			io.endProgress();
		} catch (final GitAPIException | IOException e) {
			repoRoot.resolve(".git").delete();
			io.handleException(ExceptionHandle.CONTINUE, e);
			return;
		}
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

		// TODO test decrypt
		for (final String created : encodedCreated) {
			gitSession.checkout().addPath(created).setStartPoint(commitNew)
					.call();
			encrypt(created, created.substring(4).replace(".abc", ".enc.abc"),
					false);
		}
		for (final String deleted : encodedDeleted) {
			repoRoot.resolve(deleted.substring(4)).delete();
			repoRoot.resolve(deleted.substring(4).replace(".abc", ".enc.abc"))
					.delete();
		}
		return sbHead.append(sbBody.toString()).toString();

	}

	private final void encrypt(final String source, final String target,
			boolean encrypt) {
		final AESEngine engine = new AESEngine();
		final String savedKey = main.getConfigValue(Main.VC_SECTION,
				VersionControl.AES_KEY, null);
		final byte[] key;
		if (savedKey == null) {
			final SecretKeyPlugin secretKeyPlugin = new SecretKeyPlugin();
			io.handleGUIPlugin(secretKeyPlugin);
			key = secretKeyPlugin.getKey();
			main.setConfigValue(Main.VC_SECTION, VersionControl.AES_KEY,
					secretKeyPlugin.getValue());
		} else {
			key = SecretKeyPlugin.decode(savedKey);
		}
		final KeyParameter keyParam;
		keyParam = new KeyParameter(key);
		if (savedKey == null) {
			main.flushConfig();
		}
		final Path input = repoRoot.resolve(source.split("/"));
		final Path output = repoRoot.resolve(target.split("/"));
		if (input.equals(output)) {
			final Path tmp = Path.getTmpDirOrFile("");
			encrypt(source, tmp.toString(), encrypt);
			input.delete();
			tmp.toFile().renameTo(input.toFile());
		}

		output.getParent().toFile().mkdirs();
		final byte[] bufferIn = new byte[engine.getBlockSize()];
		final byte[] bufferOut = new byte[engine.getBlockSize()];
		final InputStream streamIn = io.openIn(input.toFile());
		final OutputStream streamOut = io.openOut(output.toFile());
		engine.init(encrypt, keyParam);
		streamIn.registerProgressMonitor(io);
		if (encrypt) {
			io.setProgressTitle("Encrypting " + output.getFilename());
		} else {
			io.setProgressTitle("Decrypting " + output.getFilename());
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
				io.write(streamOut, bufferOut);
			}
		} catch (final IOException e) {
			e.printStackTrace();
			return;
		} finally {
			io.close(streamIn);
			io.close(streamOut);
			io.endProgress();
		}
	}

	private final ProgressMonitor getProgressMonitor() {
		final stone.io.ProgressMonitor monitor = io.getProgressMonitor();

		return new ProgressMonitor() {

			@Override
			public final void beginTask(final String arg0, int arg1) {
				monitor.beginTask(arg0, arg1);
			}

			@Override
			public final void endTask() {
				monitor.endProgress();
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
			throws InvalidRemoteException, TransportException, GitAPIException {
		final String refS = "refs/heads/" + BRANCH.value();
		final FetchCommand fetch = gitSession
				.fetch()
				.setRefSpecs(
						new RefSpec(refS + ":refs/remotes/origin/"
								+ BRANCH.value()))
				.setProgressMonitor(getProgressMonitor());
		if (USE_SSH.getValue()) {
			final String url = GIT_URL_SSH.value();
			final String[] split = url.replace("ssh://", "").split(":", 2);
			if (split.length == 2) {
				final Path configFile = Path.getPath("~", ".ssh", "config");
				String hostname = "github.com", user = "git", id = "id_rsa";
				if (configFile.exists()) {
					final InputStream in = io.openIn(configFile.toFile());
					while (true) {
						try {
							String line = in.readLine();
							if (line == null)
								break;
							if (line.equalsIgnoreCase("HOST " + split[0])) {
								line = in.readLine();
								while (line != null) {
									final String[] ls = line.split(" ", 2);
									final String key = ls[0].toLowerCase();
									final String value = ls[1];
									if (key.equals("hostname")) {
										hostname = value;
									} else if (key.equals("user")) {
										user = value;
									} else if (key.equals("identityfile")) {
										id = value;
									} else if (key.equals("host"))
										break;
									line = in.readLine();
								}
								break;
							}
						} catch (final IOException e) {
							io.handleException(ExceptionHandle.CONTINUE, e);
							return null;
						} finally {
							io.close(in);
						}
					}
					final Path idFile = Path.getPath("~", ".ssh", id);
					if (!idFile.exists()) {
						io.printError("Missing file for ssh connection", false);
						master.interrupt();
						return null;
					}
					final String userFinal = user, hostnameFinal = hostname;
					final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {

						@Override
						protected void configure(final Host host,
								final Session session) {
							// do nothing
							Debug.print("Configure\nhost %s\n", host);
						}

						@Override
						protected JSch createDefaultJSch(FS fs)
								throws JSchException {
							JSch defaultJSch = super.createDefaultJSch(fs);
							defaultJSch.addIdentity(idFile.toString());
							return defaultJSch;
						}
					};
					fetch.setTransportConfigCallback(new TransportConfigCallback() {

						@Override
						public void configure(final Transport transport) {
							SshTransport sshTransport = (SshTransport) transport;
							sshTransport
									.setSshSessionFactory(sshSessionFactory);
							
						}
					});
					// TODO adjust url
				}
			}
		}
		final Ref ref = fetch.call().getAdvertisedRef(refS);
		if (ref == null)
			return null;
		return ref.getObjectId();
	}

	private final void gotoBand(final Git gitSession) {
		if (gitSession == null) {
			return;
		}
		try {
			final String branch = gitSession.getRepository().getBranch();
			if (!branch.equals(BRANCH.value())) {
				io.printMessage(null,
						"not on working branch \"" + BRANCH.value()
								+ "\"\nCurrent branch is \"" + branch
								+ "\"\nCheckout branch \"" + BRANCH.value()
								+ "\" or work on branch \"" + branch + "\"",
						true);
				return;
			}
			if (master.isInterrupted()) {
				return;
			}
			checkForLocalChanges(gitSession);
		} catch (final IOException | GitAPIException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}

	private final boolean merge(final Git gitSession, final RevWalk walk,
			final RevCommit commitLocal, final RevCommit commitRoot,
			final RevCommit commitRemote) throws IOException {
		if (commitRoot == null)
			return true;
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
			final String name = main.getConfigValue(Main.GLOBAL_SECTION,
					Main.NAME_KEY, null);

			int time = commitLocal.getCommitTime();
			io.startProgress("Merging", time - commitRoot.getCommitTime());
			final Set<String> merged = new HashSet<>();

			commits.add(commitLocal);
			boolean doCommit = false;
			while (!commits.isEmpty()) {
				// go back until the latest commit contained in both branches
				final RevCommit c = commits.pollLast();
				if (c.equals(commitRoot)) {
					io.updateProgress(time - c.getCommitTime());
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
								repoRoot.resolve(old).delete();
							}
							gitSession.add().addFilepattern(old).call();
							break;
						case DELETE:
							if (merged.add(old)) {
								repoRoot.resolve(old).delete();
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
						}
					}
				}
				for (final RevCommit cP : c.getParents()) {
					if (cP.getCommitTime() < commitRoot.getCommitTime()) {
						continue;
					}
					commits.add(cP);
				}
				io.updateProgress(time - c.getCommitTime());
				time = c.getCommitTime();
			}
			reader.release();
			treeParserOld.stopWalk();
			treeParserNew.stopWalk();
			if (doCommit) {
				io.startProgress("Creating new commit", -1);
				gitSession
						.commit()
						.setMessage(
								"update "
										+ name
										+ ", "
										+ new Date(System.currentTimeMillis())
										+ "\n\n"
										+ "merge branch \'"
										+ BRANCH.value()
										+ "\' of "
										+ (USE_SSH.getValue() ? GIT_URL_SSH
												: GIT_URL_HTTPS).value())
						.setCommitter(name, EMAIL.value()).call();
			}
			gitSession.branchCreate().setForce(true).setName(BRANCH.value())
					.call();
			gitSession.checkout().setName(BRANCH.value()).call();
			gitSession.branchDelete().setBranchNames(tmpBranchName).call();
			io.endProgress();
			return true;
		} catch (final GitAPIException e) {
			e.printStackTrace();
			try {
				// reset previous state
				gitSession.checkout().setName(BRANCH.value()).call();
				gitSession.branchDelete().setBranchNames(tmpBranchName).call();
				io.printError(
						"Encountered a problem. The previous state has been recovered\n"
								+ e.getLocalizedMessage(), false);
			} catch (final GitAPIException ee) {
				io.printError(
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
	private final void push(final Git gitSession) throws GitAPIException {
		final PushCommand push = gitSession.push();
		final RefSpec ref = new RefSpec("refs/heads/"
				+ main.getConfigValue(VersionControl.SECTION, "branch",
						"master"));
		push.setRefSpecs(ref).setProgressMonitor(getProgressMonitor());
		if (!USE_SSH.getValue()) {
			final CredentialsProvider login = new UsernamePasswordCredentialsProvider(
					USERNAME.value(), PWD.value());
			push.setCredentialsProvider(login);
		}
		push.call();
		io.printMessage(null, "Push (upload) finished successfully", true);
	}

	private final void reset(final Git gitSession) throws GitAPIException,
			IOException {
		if (RESET.getValue()) {
			final ObjectId remoteHead;
			if (gitSession == null) {
				if (!repoRoot.exists() || repoRoot.delete()) {
					checkoutBand();
					reset(Git.open(repoRoot.toFile()));
				} else
					io.printError("Reset failed", true);
				return;
			} else
				remoteHead = getRemoteHead(gitSession);

			// remove lock
			repoRoot.resolve(".git", "index.lock").delete();
			// remove branches
			gitSession.branchDelete()
					.setBranchNames(BRANCH.value(), tmpBranchName).call();
			// set to remote head
			io.startProgress("Checking out " + remoteHead.getName(), -1);
			gitSession.checkout().setName(remoteHead.getName().substring(0, 8))
					.call();
			io.startProgress("Resetting local to remote head", -1);
			gitSession.reset().setRef(remoteHead.getName())
					.setMode(ResetType.HARD).call();
			gitSession.branchCreate().setName(BRANCH.value()).call();
			gitSession.checkout().setName(BRANCH.value()).call();
			io.endProgress();
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
				io.printError("The remote branch does not exist", false);
				return;
			}
			Debug.print("Remote head: %s\n", remoteHead.getName());
		} catch (final TransportException e) {
			io.printError(
					"Failed to contact github.com.\nCheck if you have internet access and try again.",
					false);
			return;
		}
		final ObjectId localHead = gitSession.getRepository().getRef("HEAD")
				.getObjectId();
		if (localHead == null) {
			io.printError("Unable to determine current head", false);
			return;
		}
		Debug.print("Local  head: %s\n", localHead.getName());
		if (remoteHead.equals(localHead)) {
			io.printMessage(null, "Your repository is up-to-date", true);
			return;
		}
		final RevWalk walk = new RevWalk(gitSession.getRepository());
		final RevCommit commitRemote = walk.parseCommit(remoteHead);

		final RevCommit commitLocal = walk.parseCommit(localHead);

		final String diffString;

		boolean success = true;

		final RevCommit commitRoot = CommitComparator
				.init(walk, gitSession, io)
				.getParent(commitLocal, commitRemote);
		if (commitRoot == null) {
			historyRewritten(gitSession, commitLocal, commitRemote, walk);
			diffString = null;
			DIFF.setValue(false);
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

		if (DIFF.getValue()) {
			io.printMessage("Changes", diffString, true);
		}

		if (!success) {
			io.printMessage(null, "Update failed", true);
		} else {
			io.printMessage(null, "Update completed succesully", true);
		}
	}

	private final void historyRewritten(final Git gitSession,
			final RevCommit commitLocal, final RevCommit commitRemote,
			final RevWalk walk) throws NoHeadException, NoMessageException,
			UnmergedPathsException, ConcurrentRefUpdateException,
			WrongRepositoryStateException, GitAPIException,
			IncorrectObjectTypeException, IOException {
		io.startProgress("Reset - History has been rewritten", -1);
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
			io.endProgress();
		}
	}
}
