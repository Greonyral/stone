package stone.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import stone.Main;
import stone.StartupContainer;


/**
 * Contains any methods usable for debugging on stdout/stderr.
 *
 * @author Nelphindal
 */
public abstract class Debug {

    private static final Debug instance = Flag.getInstance().isEnabled(
            Main.DEBUG_ID) ? new Debug() {


        @Override
        protected final synchronized void printImpl(final String string,
                final Object[] args) {
            System.out.printf(string, args);

        }

        @Override
        protected void printImpl(Throwable t) {
            t.printStackTrace(System.out);
        }
    } : new Debug() {

        private final File log = StartupContainer
                .getDatadirectory()
                .resolve(
                        FileSystem.type == FileSystem.OSType.WINDOWS ? "log.txt"
                                : "log").toFile();

        private boolean first = true;

        private PrintStream createLogStream() throws FileNotFoundException {
            boolean append = !this.first;
            if (this.first) {
                this.first = false;
                if (this.log.exists()) {
                    final String s0 = this.log.toString();
                    final String s1 = s0.replace("log", "log.0");
                    final String s2 = s0.replace("log", "log.1");
                    final String s3 = s0.replace("log", "log.2");
                    final File f0 = new File(s0);
                    final File f1 = new File(s1);
                    final File f2 = new File(s2);
                    final File f3 = new File(s3);
                    if (f2.exists()) {
                        if (f3.exists()) {
                            f3.delete();
                        }
                    }
                    f2.renameTo(f3);
                    if (f1.exists()) {
                        f1.renameTo(f2);
                    }
                    if (f0.exists()) {
                        f0.renameTo(f1);
                    }
                }
                printImpl("Started %s\n",
                        new Object[]{
                                stone.util.Time.date(System
                                        .currentTimeMillis())
                        });
            }
            return new PrintStream(new FileOutputStream(this.log, append));
        }


        @Override
        protected final void printImpl(final String string, final Object[] args) {

            if (!string.endsWith("\n")) {
                printImpl(string + "\n", args);
                return;
            }
            final String format = string.replaceAll("\r*\n",
                    FileSystem.getLineSeparator());
            try (final java.io.OutputStream out = this.createLogStream()) {
                out.write(String.format(format, args).getBytes());
                out.flush();
            } catch (final IOException e) {
                return;
            }
        }

        protected final void printImpl(Throwable t) {
            try (final PrintStream out = this.createLogStream()) {
                out.write(("++++start of trace++++" + FileSystem.getLineSeparator()).getBytes());
                t.printStackTrace(out);
                out.write(("++++endf of trace++++" + FileSystem.getLineSeparator()).getBytes());
                out.flush();
            } catch (final IOException e) {
                return;
            }
        }
    };

    /**
     * Prints an ouput to stdout if debuging-mode has been enabled.
     *
     * @param string A format string as described in Format string syntax.
     * @param args Arguments referenced by the format specifiers in the format
     * string.
     */
    public final static void print(final String string, final Object... args) {
        Debug.instance.printImpl(string, args);
    }

    public final static void print(Throwable t) {
        Debug.instance.printImpl(t);
    }

    /**
     * The actual implementation of {@link Debug#print(String, Object...)}
     *
     * @param string A format string as described in Format string syntax.
     * @param args Arguments referenced by the format specifiers in the format
     * string.
     */
    protected abstract void printImpl(String string, Object[] args);

    protected abstract void printImpl(Throwable t);

}
