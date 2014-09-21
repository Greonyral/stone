package stone.modules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import stone.MasterThread;
import stone.StartupContainer;
import stone.io.ExceptionHandle;
import stone.io.IOHandler;
import stone.io.InputStream;
import stone.io.OutputStream;
import stone.modules.versionControl.CommitComparator;
import stone.modules.versionControl.NoYesPlugin;
import stone.modules.versionControl.SecretKeyPlugin;
import stone.modules.versionControl.SortingComparator;
import stone.modules.versionControl.UpdateType;
import stone.util.BooleanOption;
import stone.util.Flag;
import stone.util.MaskedStringOption;
import stone.util.Option;
import stone.util.OptionContainer;
import stone.util.Path;
import stone.util.StringOption;
import stone.util.Time;


/**
 * The class handling all interaction with the jgit library
 * 
 * @author Nelphindal
 */
public final class VersionControl implements Module {

	private final static int VERSION = 4;

	private final static String SECTION = Main.VC_SECTION;

	private static final String DEFAULT_GIT_URL_SSH =
			"git@github.com:Greonyral/lotro-songs.git";

	private static final String DEFAULT_GIT_URL_HTTPS =
			"https://github.com/Greonyral/lotro-songs.git";

	private static final String AES_KEY = "aes-key";

	private static final BooleanOption createBooleanOption(
			final OptionContainer oc, final String key,
			boolean defaultValue, final String label,
			final String tooltip, boolean store) {
		return VersionControl.createBooleanOption(oc, key,
				Flag.NoShortFlag, Flag.NoLongFlag, defaultValue, label,
				tooltip, store);
	}

	private static final BooleanOption createBooleanOption(
			final OptionContainer oc, final String key, char shortFlag,
			final String longFlag, boolean defaultValue,
			final String tooltip, final String label, boolean store) {
		return new BooleanOption(oc, VersionControl.SECTION + key,
				tooltip, label, shortFlag, longFlag,
				VersionControl.SECTION, store ? key : null, defaultValue);
	}

	private static final MaskedStringOption createPwdOption(
			final OptionContainer oc, final String key,
			final String tooltip, final String description) {
		return new MaskedStringOption(oc, VersionControl.SECTION + key,
				description, tooltip, Flag.NoShortFlag, Flag.NoLongFlag,
				VersionControl.SECTION, key);
	}

	private static final StringOption createStringOption(
			final OptionContainer oc, final String key,
			final String defaultValue, char shortFlag,
			final String longFlag, final String helpText) {
		return new StringOption(oc, VersionControl.SECTION + key,
				helpText, helpText, shortFlag, longFlag,
				VersionControl.SECTION, key, defaultValue);
	}

	private static final StringOption createStringOption(
			final OptionContainer oc, final String key,
			final String defaultValue, final String tooltip,
			final String label) {
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
		GIT_URL_SSH =
				VersionControl
						.createStringOption(oc, "url_ssh",
								VersionControl.DEFAULT_GIT_URL_SSH,
								Flag.NoShortFlag, "git-url-ssh",
								"Changes the url to use when using ssh to connect");
		GIT_URL_HTTPS =
				VersionControl
						.createStringOption(oc, "url_https",
								VersionControl.DEFAULT_GIT_URL_HTTPS,
								Flag.NoShortFlag, "git-url-https",
								"Changes the url to use when using https to connect");
		PWD =
				VersionControl.createPwdOption(oc, "github-pwd",
						"Password at github",
						"Changes the password to login at github");
		EMAIL =
				VersionControl
						.createStringOption(
								oc,
								"email",
								null,
								"Changes the email supplied as part of commit messages",
								"Email");
		USERNAME =
				VersionControl
						.createStringOption(
								oc,
								"login",
								null,
								"The login name at remote repository, default github",
								"Username");
		BRANCH =
				VersionControl.createStringOption(oc, "branch_repo",
						"master", Flag.NoShortFlag, "branch",
						"Changes the working branch to work on");
		COMMIT =
				VersionControl
						.createBooleanOption(
								oc,
								"commit",
								'c',
								"commit",
								false,
								"Commits changes in local repository und uploads them to remote repository",
								"Commit", false);
		DIFF =
				VersionControl.createBooleanOption(oc, "diff", false,
						"Displays differences after downloading changes",
						"Show diffs", false);
		RESET =
				VersionControl
						.createBooleanOption(
								oc,
								"reset",
								false,
								"Discards uncommited changes, the working branch will be set to default branch",
								"RESET", false);
		USE_SSH =
				VersionControl
						.createBooleanOption(
								oc,
								"use_ssh",
								Flag.NoShortFlag,
								"use-ssh",
								false,
								"Uses ssh protocol for connections. This option should be used only on Unix",
								"SSH", true);
		VC_DIR =
				VersionControl
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
		final String baseValue =
				main.getConfigValue(Main.GLOBAL_SECTION, Main.PATH_KEY,
						null);
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
		repoRoot =
				base.resolve(main.getConfigValue(Main.VC_SECTION,
						Main.REPO_KEY, "band"));
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
		final String baseValue =
				main.getConfigValue(Main.GLOBAL_SECTION, Main.PATH_KEY,
						null);
		if (baseValue == null) {
			System.out
					.println("Unable to determine base - The local repository could not been deleted");
			return;
		}
		final Path base_ =
				Path.getPath(baseValue.split("/")).resolve("Music");
		final Path repoRoot_ =
				base_.resolve(main.getConfigValue(Main.VC_SECTION,
						Main.REPO_KEY, "band"));
		final Path board_ =
				Path.getPath(main.getConfigValue(
						VersionControl.SECTION,
						"boardPath",
						base_.resolve("..", "PluginData", "BulletinBoard")
								.toString()).split("/"));
		if (board_.exists()) {
			final boolean success = board_.delete();
			System.out.printf("Delet%s %s%s\n", success ? "ed" : "ing",
					board_.toString(), success ? "" : " failed");
		}
		if (repoRoot_.exists()) {
			final NoYesPlugin plugin =
					new NoYesPlugin(
							"Delete local repository?",
							repoRoot_
									+ "\nand all its contentns will be deleted. You can\n"
									+ "answer with NO and delete only the data used for git",
							io.getGUI(), false);
			synchronized (io) {
				io.handleGUIPlugin(plugin);
			}
			if (plugin.get()) {
				final boolean success = repoRoot_.delete();
				System.out.printf("Delet%s %s%s\n",
						success ? "ed" : "ing", repoRoot_.toString(),
						success ? "" : " failed");
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
		final String name =
				main.getConfigValue(Main.GLOBAL_SECTION, Main.NAME_KEY,
						null);
		final Git gitSession_band;

		final String branch = BRANCH.value();
		final boolean ssh = USE_SSH.getValue();
		final String remoteURL =
				(ssh ? GIT_URL_SSH : GIT_URL_HTTPS).value();

		if (COMMIT.getValue()) {
			if (EMAIL.value() == null) {
				io.printError("For commits a valid email is needed", false);
				return;
			}
			if ((name == null) || name.isEmpty()) {
				io.printError("For commits a valid name is needed", false);
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
		try {
			if (!repoRoot.resolve(".git").exists()) {
				checkoutBand();
				if (!repoRoot.resolve(".git").exists()) {
					gitSession_band = null;
				} else {
					gitSession_band = Git.open(repoRoot.toFile());
				}
			} else {
				gitSession_band = Git.open(repoRoot.toFile());
			}
			if (gitSession_band != null) {
				final StoredConfig config =
						gitSession_band.getRepository().getConfig();
				config.setString("user", null, "name", name);
				config.setString("user", null, "email", EMAIL.value());
				config.setString("branch", branch, "merge", "refs/heads/"
						+ branch);
				config.setString("branch", branch, "remote", "origin");
				config.setString("remote", "origin", "url", remoteURL);
				config.save();
			}
		} catch (final JGitInternalException | IOException e) {
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
		// io.log("modified: " + status.getModified().toString());
		// io.log("untracked: " + status.getUntracked().toString());
		// io.log("missing: " + status.getMissing().toString());
		// io.log("added: " + status.getAdded().toString());
		// io.log("changed: " + status.getChanged().toString());
		// io.log("removed: " + status.getRemoved().toString());
		if (!status.isClean()) {

			final Set<String> add = new HashSet<>();
			final Set<String> rm = new HashSet<>();

			if (COMMIT.getValue()) {
				processNewAndChanges(add, status, gitSession);
			}

			processMissing(rm, status, gitSession);

			final boolean doCommit =
					COMMIT.getValue()
							&& !(add.isEmpty() && rm.isEmpty()
									&& status.getAdded().isEmpty() && status
									.getRemoved().isEmpty());

			if (doCommit) {
				final CommitCommand commit = gitSession.commit();

				commit.setAuthor(main.getConfigValue(Main.GLOBAL_SECTION,
						Main.NAME_KEY, null), EMAIL.value());
				commit.setMessage("update " + commit.getAuthor().getName()
						+ ", " + new Date(System.currentTimeMillis()));

				final RevCommit commitRet = commit.call();

				io.printMessage(
						null,
						"commit: "
								+ commitRet.getFullMessage()
								+ "\nStarting to upload changes after checking remote repository for changes",
						false);
			}
		}
		if (COMMIT.getValue()) {
			update(gitSession);
			push(gitSession);
			return;
		}
	}

	private final void checkoutBand() {
		final NoYesPlugin plugin =
				new NoYesPlugin(
						"Local repository " + repoRoot.getFileName()
								+ " does not exist",
						Main.formatMaxLength(repoRoot, null,
								"The directory ",
								" does not exist or is no git-repository.\n")
								+ "It can take a while to create it. Continue?",
						io.getGUI(), false);
		io.handleGUIPlugin(plugin);
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
			final StoredConfig config =
					gitSession.getRepository().getConfig();
			config.setString("remote", "origin", "url",
					VersionControl.DEFAULT_GIT_URL_HTTPS);
			config.setString("branch", BRANCH.value(), "remote", BRANCH
					.value());
			config.setString("branch", BRANCH.value(), "merge",
					"+refs/heads/" + BRANCH.value());
			config.save();

			final ObjectId remoteHead = getRemoteHead(gitSession);

			final DiffCommand diffCommand = gitSession.diff();
			final List<DiffEntry> diffs;

			// set head
			gitSession.reset().setMode(ResetType.SOFT).setRef(
					remoteHead.getName()).call();
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
				gitSession.reset().setRef(remoteHead.getName()).addPath(
						file).call();
				final CheckoutCommand checkout =
						gitSession.checkout().addPath(file);
				final boolean existing = repoRoot.resolve(file).exists();
				if (existing) {
					final String old =
							repoRoot.resolve(file).createBackup("_old");
					if (old == null) {
						io.printError("failed to checkout " + old, true);
						io.updateProgress(1);
						continue;
					}
					io.printError(String.format("%-40s renamed to %s\n",
							file, old), true);
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

	private final String diff(final RevWalk walk,
			final RevCommit commitOld, final RevCommit commitNew,
			final Git gitSession) throws MissingObjectException,
			IncorrectObjectTypeException, IOException, GitAPIException {
		final ObjectReader reader =
				gitSession.getRepository().newObjectReader();
		final RevTree treeOld;
		final RevTree treeNew;
		final CanonicalTreeParser treeParserOld =
				new CanonicalTreeParser();
		final CanonicalTreeParser treeParserNew =
				new CanonicalTreeParser();

		treeOld = walk.parseTree(commitOld.getTree().getId());
		treeNew = walk.parseTree(commitNew.getTree().getId());

		treeParserOld.reset(reader, treeOld.getId());
		treeParserNew.reset(reader, treeNew.getId());

		final List<DiffEntry> diffs =
				gitSession.diff().setOldTree(treeParserOld).setNewTree(
						treeParserNew).setShowNameAndStatusOnly(true)
						.call();
		final StringBuilder sbHead = new StringBuilder(), sbBody =
				new StringBuilder();
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

		// TODO test
		for (final String created : encodedCreated) {
			encrypt(created, created.substring(4) + ".abc", false);
		}
		for (final String deleted : encodedDeleted) {
			repoRoot.resolve(deleted.substring(4) + ".abc").delete();
		}
		return sbHead.append(sbBody.toString()).toString();

	}

	@SuppressWarnings("resource")
	private final void encrypt(final String source, final String target,
			boolean encrypt) {
		final AESEngine engine = new AESEngine();
		final String savedKey =
				main.getConfigValue(Main.VC_SECTION,
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
		output.getParent().toFile().mkdirs();
		final byte[] bufferIn = new byte[engine.getBlockSize()];
		final byte[] bufferOut = new byte[engine.getBlockSize()];
		final InputStream streamIn = io.openIn(input.toFile());
		final OutputStream streamOut = io.openOut(output.toFile());
		engine.init(encrypt, keyParam);
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
		}
	}

	private final RevCommit getParentOfLocalAndRemote(final RevWalk walk,
			final RevCommit commitLocal, final RevCommit commitRemote,
			final Git gitSession) throws MissingObjectException,
			IncorrectObjectTypeException, IOException {
		final Comparator<RevCommit> cmp = new CommitComparator();
		final TreeSet<RevCommit> commitLocalList = new TreeSet<>(cmp);
		final TreeSet<RevCommit> commitRemoteList = new TreeSet<>(cmp);
		commitLocalList.add(commitLocal);
		commitRemoteList.add(commitRemote);
		int min =
				Math.min(commitLocal.getCommitTime(), commitRemote
						.getCommitTime());
		io.startProgress("Checking history of commits", Math
				.abs(commitLocal.getCommitTime()
						- commitRemote.getCommitTime()));
		while (true) {
			final RevCommit commonParent =
					getParentOfLocalAndRemote(walk, commitLocalList,
							commitRemoteList);
			if (commonParent == null) {
				if ((commitLocalList.last().getCommitTime() < min)
						|| (commitRemoteList.last().getCommitTime() < min)) {
					final int max =
							Math.max(commitLocalList.last()
									.getCommitTime(), commitRemoteList
									.last().getCommitTime());
					min =
							Math.min(commitLocalList.last()
									.getCommitTime(), commitRemoteList
									.last().getCommitTime());
					final String delta =
							Time.delta(System.currentTimeMillis()
									- TimeUnit.SECONDS.toMillis(min));
					io.startProgress("Looking for start point of merge "
							+ delta + " ago", max - min);
				}
				continue;
			}
			io.endProgress();
			if (commitRemote.equals(commonParent)) {
				return commitRemote;
			}
			return commonParent;
		}
	}

	private final RevCommit getParentOfLocalAndRemote(final RevWalk walk,
			final TreeSet<RevCommit> commitLocalList,
			final TreeSet<RevCommit> commitRemoteList)
			throws MissingObjectException, IncorrectObjectTypeException,
			IOException {
		final RevCommit commitLocal = commitLocalList.last();
		final RevCommit commitRemote = commitRemoteList.last();

		if (commitLocal.equals(commitRemote)) {
			return commitLocal;
		}
		if (commitLocal.getCommitTime() > commitRemote.getCommitTime()) {
			commitLocalList.remove(commitLocal);
			for (final RevCommit c : commitLocal.getParents()) {
				walk.parseCommit(c);
				commitLocalList.add(c);
			}
			io.updateProgress(commitLocal.getCommitTime()
					- commitLocalList.last().getCommitTime());
		} else {
			commitRemoteList.remove(commitRemote);
			for (final RevCommit c : commitRemote.getParents()) {
				walk.parseCommit(c);
				commitRemoteList.add(c);
			}
			io.updateProgress(commitRemote.getCommitTime()
					- commitRemoteList.last().getCommitTime());
		}
		return null;
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
			throws InvalidRemoteException, TransportException,
			GitAPIException {
		final FetchResult fetch =
				gitSession.fetch()
						.setRefSpecs(
								new RefSpec("refs/heads/" + BRANCH.value()
										+ ":refs/remotes/origin/"
										+ BRANCH.value()))
						.setProgressMonitor(getProgressMonitor()).call();
		return fetch.getAdvertisedRef("HEAD").getObjectId();
	}

	private final void gotoBand(final Git gitSession) {
		if (gitSession == null) {
			return;
		}
		try {
			final String branch = gitSession.getRepository().getBranch();
			if (!branch.equals(BRANCH.value())) {
				io.printMessage(null, "not on working branch \""
						+ BRANCH.value() + "\"\nCurrent branch is \""
						+ branch + "\"\nCheckout branch \""
						+ BRANCH.value() + "\" or work on branch \""
						+ branch + "\"", true);
				return;
			}
			reset(gitSession);
			if (master.isInterrupted()) {
				return;
			}
			checkForLocalChanges(gitSession);
			if (master.isInterrupted()) {
				return;
			}
			if (!COMMIT.getValue()) {
				update(gitSession);
			}
		} catch (final IOException | GitAPIException e) {
			io.handleException(ExceptionHandle.CONTINUE, e);
		}
	}

	private final boolean merge(final Git gitSession, final RevWalk walk,
			final RevCommit commitLocal, final RevCommit commitRoot,
			final RevCommit commitRemote) throws IOException {
		final String tmpBranch = "tmpMerge_pull";
		try {
			gitSession.branchCreate().setName(tmpBranch).setForce(true)
					.call();
			gitSession.checkout().setName(tmpBranch).call();
			gitSession.reset().setMode(ResetType.HARD).setRef(
					commitRemote.getName()).call();
			// tmp branch is equal to FETCH_HEAD now

			final ObjectReader reader =
					gitSession.getRepository().newObjectReader();
			final TreeSet<RevCommit> commits =
					new TreeSet<>(new CommitComparator());
			final CanonicalTreeParser treeParserNew =
					new CanonicalTreeParser();
			final CanonicalTreeParser treeParserOld =
					new CanonicalTreeParser();
			final String name =
					main.getConfigValue(Main.GLOBAL_SECTION,
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
					final List<DiffEntry> diffs =
							gitSession.diff().setOldTree(treeParserOld)
									.setNewTree(treeParserNew)
									.setShowNameAndStatusOnly(true).call();
					for (final DiffEntry e : diffs) {
						final String old = e.getOldPath();
						final String add = e.getNewPath();
						doCommit = true;
						switch (e.getChangeType()) {
							case RENAME:
								if (merged.add(add)) {
									gitSession.checkout().setStartPoint(
											commitLocal).addPath(add)
											.call();
								}
								gitSession.add().addFilepattern(add)
										.call();

								if (merged.add(old)) {
									repoRoot.resolve(old).delete();
								}
								gitSession.add().addFilepattern(old)
										.call();
								break;
							case DELETE:
								if (merged.add(old)) {
									repoRoot.resolve(old).delete();
								}
								gitSession.add().addFilepattern(old)
										.call();
								break;
							case ADD:
							case COPY:
							case MODIFY:
								if (merged.add(add)) {
									gitSession.checkout().setStartPoint(
											commitLocal).addPath(add)
											.call();
								}
								gitSession.add().addFilepattern(add)
										.call();
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
				gitSession.commit().setMessage(
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
			gitSession.branchCreate().setForce(true).setName(
					BRANCH.value()).call();
			gitSession.checkout().setName(BRANCH.value()).call();
			gitSession.branchDelete().setBranchNames(tmpBranch).call();
			io.endProgress();
			return true;
		} catch (final GitAPIException e) {
			e.printStackTrace();
			try {
				// reset previous state
				gitSession.checkout().setName(BRANCH.value()).call();
				gitSession.branchDelete().setBranchNames(tmpBranch).call();
				io.printError(
						"Encountered a problem. The previous state has been recovered\n"
								+ e.getLocalizedMessage(), false);
			} catch (final GitAPIException ee) {
				io.printError("Encountered a problem.\n"
						+ e.getLocalizedMessage()
						+ "\nRecovering from it another problem occured\n"
						+ ee.getMessage(), false);
			}
			return false;
		}
	}

	/**
	 * do tests if file is candidate to be uploaded/deleted from the central
	 * repository and if yes ask user, whether this file should be added/deleted
	 * uploaded
	 * 
	 * @param type
	 * @param file
	 * @return <i>true</i> if user hit yes
	 */
	private final boolean processChangedFile(final String file,
			final UpdateType type) {
		final NoYesPlugin plugin =
				new NoYesPlugin(type.getQuestionPart0(), type
						.getQuestionPart0()
						+ "\n" + file + "\n" + type.getQuestionPart1(), io
						.getGUI(), true);
		io.handleGUIPlugin(plugin);
		return plugin.get();
	}

	private final void processMissing(final Set<String> rm,
			final Status status, final Git gitSession)
			throws RefAlreadyExistsException, RefNotFoundException,
			InvalidRefNameException, CheckoutConflictException,
			GitAPIException {
		final List<String> missingList =
				new ArrayList<>(status.getMissing());
		Collections.sort(missingList, new SortingComparator(repoRoot));

		io.startProgress("Missing files", missingList.size());
		for (final String fileMissing0 : missingList) {
			if (processChangedFile(fileMissing0,
					UpdateType.RESTORE_MISSING)) {
				final String fileMissing;
				if (fileMissing0.startsWith("enc/")) {
					fileMissing = fileMissing0.substring(4) + ".abc";
				} else {
					fileMissing = fileMissing0;
				}
				// restore it
				final CheckoutCommand checkoutCommand =
						gitSession.checkout();
				checkoutCommand.addPath(fileMissing0);
				checkoutCommand.call();
				if (fileMissing != fileMissing0) {
					encrypt(fileMissing0, fileMissing, false);
				}
			} else if (COMMIT.getValue()) {
				final String fileMissing =
						fileMissing0.startsWith("enc/") ? fileMissing0
								.substring(4) : fileMissing0;
				final boolean isOwner =
						fileMissing.startsWith(main.getConfigValue(
								Main.GLOBAL_SECTION, Main.NAME_KEY, null)
								+ "/");
				if (isOwner) {
					if (processChangedFile(fileMissing, UpdateType.DELETE)) {
						// do delete it
						rm.add(fileMissing);
						final RmCommand removeCommand = gitSession.rm();
						removeCommand.addFilepattern(fileMissing);
						removeCommand.call();
					}
				}
			}
			io.updateProgress();
		}
		io.endProgress();
	}

	private final void processNewAndChanges(final Set<String> add,
			final Status status, final Git gitSession)
			throws NoFilepatternException, GitAPIException {
		final List<String> untrackedList =
				new ArrayList<>(status.getUntracked());
		final List<String> modList = new ArrayList<>(status.getModified());
		Collections.sort(untrackedList, new SortingComparator(repoRoot));
		final Iterator<String> iterFile = untrackedList.iterator();
		while (iterFile.hasNext()) {
			final String file = iterFile.next();
			if (file.endsWith(".enc.abc")) {
				final File encoded =
						repoRoot.resolve(
								("enc/" + file.substring(0,
										file.length() - 4)).split("/"))
								.toFile();
				if (!encoded.exists()) {
					continue;
				}
				if (encoded.lastModified() < repoRoot.resolve(file)
						.toFile().lastModified()) {
					modList.add(file);
				}
			} else if (file.endsWith(".abc")) {
				continue;
			}
			iterFile.remove();
		}
		io.startProgress("Untracked files", untrackedList.size());
		final Set<String> conflicts = new HashSet<>();
		for (final String fileUntracked0 : untrackedList) {
			if (processChangedFile(fileUntracked0, UpdateType.ADD)) {
				final String fileUntracked;
				if (fileUntracked0.endsWith("enc.abc")) {
					fileUntracked =
							"enc/"
									+ fileUntracked0.substring(0,
											fileUntracked0.length() - 4);
					encrypt(fileUntracked0, fileUntracked, true);
				} else {
					fileUntracked = fileUntracked0;
				}
				add.add(fileUntracked);
				final DirCache addRet =
						gitSession.add().addFilepattern(fileUntracked)
								.call();
				if (addRet.hasUnmergedPaths()) {
					conflicts.add(fileUntracked);
				}
			}
			io.updateProgress();
		}

		Collections.sort(modList, new SortingComparator(repoRoot));
		io.startProgress("Modified files", modList.size());
		for (final String fileModified0 : modList) {
			if (processChangedFile(fileModified0,
					UpdateType.RESTORE_CHANGED)) {
				// restore it
				final String fileModified;
				if (fileModified0.endsWith("enc.abc")) {
					fileModified =
							"enc/"
									+ fileModified0.substring(0,
											fileModified0.length() - 4);
					encrypt(fileModified, fileModified0, false);
				} else {
					fileModified = fileModified0;
				}
				final CheckoutCommand checkoutCommand =
						gitSession.checkout();
				checkoutCommand.addPath(fileModified);
				checkoutCommand.call();
			} else if (processChangedFile(fileModified0, UpdateType.UPDATE)) {
				final String fileModified;
				if (fileModified0.endsWith("enc.abc")) {
					fileModified =
							"enc/"
									+ fileModified0.substring(0,
											fileModified0.length() - 4);
					encrypt(fileModified0, fileModified, true);
				} else {
					fileModified = fileModified0;
				}
				add.add(fileModified);
				gitSession.add().addFilepattern(fileModified).call();
			}
			io.updateProgress();
		}

		io.endProgress();
		if (!conflicts.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			sb.append("There are conflicts with following file(s):\n");
			for (final String c : conflicts) {
				sb.append(c);
				sb.append("\n");
			}
			io.printError(sb.toString(), false);
			return;
		}
	}

	/*
	 * do the upload of commits
	 */
	private final void push(final Git gitSession) throws GitAPIException {
		final PushCommand push = gitSession.push();
		final RefSpec ref =
				new RefSpec("refs/heads/"
						+ main.getConfigValue(VersionControl.SECTION,
								"branch", "master"));
		push.setRefSpecs(ref).setProgressMonitor(getProgressMonitor());
		if (!USE_SSH.getValue()) {
			final CredentialsProvider login =
					new UsernamePasswordCredentialsProvider(USERNAME
							.value(), PWD.value());
			push.setCredentialsProvider(login);
		}
		push.call();
		io.printMessage(null, "Push (upload) finished successfully", true);
	}

	private final void reset(final Git gitSession) throws GitAPIException,
			IOException {
		if (RESET.getValue()) {
			final ObjectId localHead =
					gitSession.getRepository().getRef("HEAD")
							.getObjectId();
			final CheckoutCommand checkout =
					gitSession.checkout().setName(localHead.name());
			final ResetCommand reset = gitSession.reset();
			checkout.call();
			reset.setMode(ResetCommand.ResetType.HARD);
			reset.call();
		}
	}

	/*
	 * download new songs
	 */
	private final void update(final Git gitSession)
			throws GitAPIException, IOException {
		final ObjectId remoteHead;
		try {
			remoteHead = getRemoteHead(gitSession);
		} catch (final TransportException e) {
			io.printError(
					"Failed to contact github.com.\nCheck if you have internet access and try again.",
					false);
			return;
		}
		final ObjectId localHead =
				gitSession.getRepository().getRef("HEAD").getObjectId();

		if (remoteHead.equals(localHead)) {
			io.printMessage(null, "Your repository is up-to-date", true);
			return;
		}
		final RevWalk walk = new RevWalk(gitSession.getRepository());
		final RevCommit commitRemote = walk.parseCommit(remoteHead);
		if (localHead == null) {
			io.printError("Unable to determine current head", false);
			return;
		}

		final RevCommit commitLocal = walk.parseCommit(localHead);

		final String diffString;

		boolean success = true;

		final RevCommit commitRoot =
				getParentOfLocalAndRemote(walk, commitLocal, commitRemote,
						gitSession);

		diffString = diff(walk, commitRoot, commitRemote, gitSession);

		try {
			success =
					merge(gitSession, walk, commitLocal, commitRoot,
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
}
