package ca.codepit.tw2md;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static ca.codepit.tw2md.Main.BLOCK_TYPE.*;
import static picocli.CommandLine.*;

/**
 * @author evan
 */
@Command(name = "tw2md",
				mixinStandardHelpOptions = true,
				version = "tw2md 1.0",
				description = "Convert TiddlyWiki files to Obsidian compatible markdown files.")
public class Main implements Callable<Integer> {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	private static final String TAGS_HEADER = "tags";
	private static final String TYPE_HEADER = "type";
	private static final String TITLE_HEADER = "title";

	private static final String ALIASES_FRONTMATTER = "aliases";
	private static final String TAGS_FRONTMATTER = "tags";
	private static final String TIDDLERS_DIR = "/tiddlers";
	private static final String TIDDLYWIKI_TYPE = "text/vnd.tiddlywiki";
	private static final String OSX_DS_STORE_DIR = ".DS_Store";
	private static final String TIDDLER_EXT = ".tid";
	private static final String MARKDOWN_EXT = ".md";

	private static final String NL = System.lineSeparator();
	private static final String PATH_CHAR = File.separator;

	private final static Pattern UNDERLINE_REGEX = Pattern.compile("__");
	private final static Pattern SUPER_REGEX = Pattern.compile("\\^\\^");
	private final static Pattern SUB_REGEX = Pattern.compile(",,");
	private final static Pattern TITLE_REGEX = Pattern.compile("^(!+) *");
	private final static Pattern BULLET_LIST_REGEX = Pattern.compile("^ *([-*]+) *");
	private final static Pattern NUMBER_LIST_REGEX = Pattern.compile("^ *(#+) *");

	enum BLOCK_TYPE {
		BLOCK_END,
		TEXT,
		CODE_BLOCK,
		QUOTE_BLOCK,
		TABLE,
		HEADER,
		QUOTE,
		BULLET_LIST,
		NUMBER_LIST
	}

	enum CASE_CONVERTER {
		PASCAL(Main::pascalCaseConversion),
		CAMEL(Main::camelCaseConversion),
		UPPER(Main::upperCaseConversion),
		LOWER(Main::lowerCaseConversion),
		NONE(Main::noCaseConversion);

		private final Function<String, String> f;

		CASE_CONVERTER(Function<String, String> f) {

			this.f = f;
		}

		private String convert(String s) {

			return f.apply(s);
		}
	}

//	CONFIGURATION PARAMETERS
//	================================================================================================================

	/**
	 * tiddlywiki wiki root directory
	 */
	@SuppressWarnings("unused")
	@Parameters(index = "0", description = "The root directory containing the tiddlyWiki 'tiddlers' directory.")
	private File sourceDirectory;

	/**
	 * output root directory
	 */
	@SuppressWarnings("unused")
	@Parameters(index = "1", description = "The output directory were the Obsidian markdown files will be saved.")
	private File outputDirectory;

	/**
	 * additional assets to move into the output directory
	 */
	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	@Option(names = {"--tiddlywiki-assets-path"}, description = "Include assets from another directory into the Obsidian vault" +
					" subdirectory, path is relative to the tiddlywiki home directory.")
	private Optional<String> tiddlyWikiAssetsPath;

	/**
	 * collect assets to a directory
	 */
	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	@Option(names = {"--assets-path"}, description = "Collect assets into an Obsidian vault subdirectory, path is relative" +
					" to the output directory.")
	private Optional<String> assetPath;

	@Option(names = {"--illegal-tag-character"}, defaultValue = "_", description = "Character used to replace illegal Obsidian" +
					" tag characters (Default: ${DEFAULT-VALUE}).")
	protected String illegalTagCharacterReplacement;

	@Option(names = {"--numeric-tag-prefix"}, defaultValue = "t", description = "Prefix added in front of numeric tiddlywiki" +
					" tags (Default: ${DEFAULT-VALUE}).")
	protected String numericTagPrefix;

	@Option(names = {"--space-tag-character"}, defaultValue = "-", description = "Character used to replace space characters" +
					" used in tiddlywiki tags (Default: ${DEFAULT-VALUE}).")
	protected String spaceTagCharacterReplacement;

	@Option(names = {"--tag-case-conversion"}, defaultValue = "NONE", description = "Convert tag case, can be combined with" +
					" `--space-tag-character`, valid values: ${COMPLETION-CANDIDATES}")
	protected CASE_CONVERTER tagCaseConversion;

	@Option(names = {"--detect-checklists"}, description = "Treat lists that contain a struck out item as todo lists.")
	protected boolean detectChecklists;

	@Option(names = {"--detect-checklist-headers"}, description = "Do not add checkboxes to todo list items that have indented sub-items.")
	protected boolean detectChecklistHeaders;

	@Option(names = {"--include-system-tiddlers"}, description = "Include system tiddlers in the output directory.")
	protected boolean includeSystemTiddlers;

	@Option(names = {"--add-titles"}, description = "Add the TiddlyWiki title as a header to the top of all output documents.")
	protected boolean addTitles;

	@Option(names = {"--add-titles-tag"}, description = "Add the TiddlyWiki title as a header to the top of documents when" +
					" tagged with this tag.")
	protected List<String> addTitlesForTags = new ArrayList<>();

	@Option(names = {"-m", "--map-tag"}, description = "Map tiddlywiki tags into Obsidian vault subdirectories.")
	protected Map<String, String> tagToFolderMap = new HashMap<>();

//	INTERNAL STATE
//	================================================================================================================

	private boolean openUnderline = true;

	private boolean openSub = true;

	private boolean openSup = true;

	public static void main(String[] args) {

		int exitCode = new CommandLine(new Main()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() throws Exception {

		// make directories
		if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
			log.warn("Failed to create output directory: {}", outputDirectory);
		}

		final File assetDir = assetPath.map(p -> {
			final File aDir = new File(outputDirectory, p);
			if (!aDir.exists() && !aDir.mkdirs()) {
				log.warn("Failed to create asset directory: {}", aDir);
			}
			return aDir;
		}).orElse(outputDirectory);

		for (String mapFolders : tagToFolderMap.values()) {
			final File outDir = new File(outputDirectory, mapFolders);
			if (!outDir.exists() && !outDir.mkdirs()) {
				log.warn("Failed to create output map directory: {}", outDir);
			}
		}

		try (Stream<Path> path = Files.walk(new File(sourceDirectory, TIDDLERS_DIR).toPath())) {
			System.out.println();
			path.map(Path::toFile)
							.filter(File::isFile)
							.forEach(inFile -> {
								System.out.print("."); // show progress
								try {
									if (!includeSystemTiddlers && inFile.getName().startsWith("$_")) {
										log.debug("Skipping system tiddler file: {}", inFile);
									} else if (inFile.toString().endsWith(".meta")) {
										log.debug("Skipping meta file: {}", inFile);
									} else if (inFile.toString().endsWith(TIDDLER_EXT)) {
										readTiddler(inFile.toPath()).ifPresent(tiddler -> {
											File outFile;
											try {
												if (TIDDLYWIKI_TYPE.equals(tiddler.getHeader(TYPE_HEADER))) {
													// reset open/close tag flags
													openUnderline = openSub = openSup = true;
													String inFileName = inFile.getName();
													String md = toMarkdown(tiddler, inFileName);
													String outFileName = inFileName.substring(0, inFileName.length() - TIDDLER_EXT.length()) + MARKDOWN_EXT;
													outFile = new File(calculateOutputDirectory(tiddler), outFileName);
													log.debug("Saving tiddler {} -> {}", inFile, outFile);
													Files.writeString(outFile.toPath(), md);
												} else {
													final String header = tiddler.getHeader(TITLE_HEADER);
													outFile = new File(assetDir, header);
													log.debug("Saving binary tiddler {} -> {}", inFile, outFile);
													saveBinaryTiddler(tiddler, outFile.toPath());
												}
												tiddler.getCreatedTime().ifPresent(ct -> {
													final ZonedDateTime mt = tiddler.getLastUpdatedTime().orElse(ct);
													setFileTimestamps(outFile.toPath(), ct, mt);
												});
											} catch (IOException e) {
												log.error("{}", e.getMessage(), e);
											}
										});
									} else {
										final Path savePath = new File(assetDir, inFile.getName()).toPath();
										log.debug("Saving asset {} -> {}", inFile, savePath);
										Files.copy(inFile.toPath(), savePath, StandardCopyOption.REPLACE_EXISTING);
									}
								} catch (IOException e) {
									log.error("{}", e.getMessage(), e);
								}
							});
		}

		tiddlyWikiAssetsPath.ifPresent(ap -> {
			final File sourceAssetPath = new File(sourceDirectory, ap);
			final int assetPathRootLen = sourceAssetPath.getAbsolutePath().length();
			try (Stream<Path> path = Files.walk(sourceAssetPath.toPath())) {
				path.map(Path::toFile)
								.filter(f -> !f.getName().equals(OSX_DS_STORE_DIR))
								.forEach(file -> {
									System.out.print("."); // show progress
									String localPath = file.getAbsolutePath().substring(assetPathRootLen);
									final File outFile = new File(assetDir, localPath);
									if (file.isDirectory()) {
										if (outFile.exists() || outFile.mkdirs()) {
											log.debug("MKDIR: {}", outFile.getAbsolutePath());
										} else {
											log.warn("Failed to MKDIR: {}", outFile.getAbsolutePath());
										}
									} else {
										try {
											log.debug("COPY: {} -> {}", file, outFile.getAbsolutePath());
											Files.copy(file.toPath(), outFile.toPath());
										} catch (FileAlreadyExistsException e) {
											log.debug("FILE EXISTS: {}", e.getMessage());
										} catch (IOException e) {
											log.error("{}", e.getMessage(), e);
										}
									}
								});
			} catch (IOException e) {
				log.error("{}", e.getMessage(), e);
			}
		});

		System.out.print("Done!");
		System.out.println();

		return 0;
	}

	private File calculateOutputDirectory(Tiddler tiddler) {

		final List<String> tiddlerTags = splitTags(tiddler.getHeader(TAGS_HEADER));

		for (String tag : tiddlerTags) {
			String outFolder = tagToFolderMap.get(tag);
			if (outFolder != null) {
				outFolder = outFolder.startsWith(PATH_CHAR) ? outFolder : PATH_CHAR + outFolder;
				return new File(outputDirectory, outFolder);
			}
		}

		return outputDirectory;
	}

	public Optional<Tiddler> readTiddler(Path p) {

		final List<String> lines;
		try {
			lines = Files.readAllLines(p, StandardCharsets.UTF_8);
			List<String> header = new ArrayList<>();
			List<String> body = new ArrayList<>();
			List<String> active = header;
			for (String line : lines) {
				if (line.isBlank() && active == header) {
					active = body;
					continue;
				}
				active.add(line);
			}

			Map<String, String> headers = new HashMap<>();
			for (String s : header) {
				final String[] split = s.split(": ", 2);
				headers.put(split[0], split[1]);
			}

			return Optional.of(new Tiddler(headers, body));
		} catch (IOException e) {
			log.error("Error reading {}, {}", p, e.getMessage(), e);
		}

		return Optional.empty();
	}

	/**
	 * take the lines in a tiddler and group them together into TYPED blocks.
	 */
	private List<Block> blockify(List<String> lines) {

		List<Block> blocks = new ArrayList<>();
		List<String> currentBlock = new ArrayList<>();
		BLOCK_TYPE last_bt = TEXT;

		for (String line : lines) {
			BLOCK_TYPE bt = last_bt;
			if (bt == CODE_BLOCK || bt == QUOTE_BLOCK) {
				if (line.startsWith("```")) bt = BLOCK_END;
				if (line.startsWith("<<<")) bt = BLOCK_END;
			} else {
				if (line.startsWith("*") || line.startsWith("-")) {
					bt = BULLET_LIST;
				} else if (line.startsWith("#")) {
					bt = NUMBER_LIST;
				} else if (line.startsWith("!")) {
					bt = HEADER;
				} else if (line.startsWith("|")) {
					bt = TABLE;
				} else if (line.startsWith(">")) {
					bt = QUOTE;
				} else if (line.startsWith("```")) {
					bt = CODE_BLOCK;
				} else if (line.startsWith("<<<")) bt = QUOTE_BLOCK;
			}

			if ((last_bt != bt || line.isBlank())
							&& (bt != CODE_BLOCK && bt != QUOTE_BLOCK)) {
				if (bt == BLOCK_END) {
					currentBlock.add(line);
				}
				if (!currentBlock.isEmpty()) {
					blocks.add(new Block(last_bt, currentBlock));
				}
				currentBlock = new ArrayList<>();
			}

			switch (bt) {
				case HEADER:
					blocks.add(new Block(bt, line));
					bt = TEXT;
					break;
				case CODE_BLOCK:
				case QUOTE_BLOCK:
					currentBlock.add(line);
					break;
				case BLOCK_END:
					bt = TEXT;
					break;
				default:
					if (line.isBlank()) {
						bt = TEXT;
					} else {
						currentBlock.add(line);
					}
			}
			last_bt = bt;
		}

		// add the final block
		if (!currentBlock.isEmpty()) {
			blocks.add(new Block(last_bt, currentBlock));
		}

		return blocks;
	}

	/**
	 * render the tiddler as markdown
	 */
	public String toMarkdown(Tiddler t, String filename) {

		StringBuilder md = new StringBuilder();

		md.append(renderFrontMatter(t, filename));

		final List<Block> blocks = blockify(t.getBody());

		if (addTitleAsHeader(t) && t.getHeader(TITLE_HEADER) != null) {
			blocks.add(0, new Block(HEADER, "!" + t.getHeader(TITLE_HEADER)));
		}

		for (int i = 0; i < blocks.size(); i++) {
			Block block = blocks.get(i);
			Optional<Block> nextBlock = i < blocks.size() - 1 ? Optional.of(blocks.get(i + 1)) : Optional.empty();

			switch (block.getBlockType()) {
				case CODE_BLOCK:
					md.append(renderCodeBlock(block.getLines()));
					break;
				case QUOTE_BLOCK:
					md.append(renderQuoteBlock(t, block.getLines()));
					break;
				case TABLE:
					md.append(renderTableBlock(t, block.getLines()));
					break;
				case NUMBER_LIST:
				case BULLET_LIST:
					md.append(renderList(t, block.getLines()));
					break;
				default:
					md.append(renderTextBlock(t, block.getLines()));
			}
			md.append(NL);

			nextBlock.ifPresent(next -> {
				final BLOCK_TYPE nbt = next.getBlockType();
				if (block.getBlockType() != HEADER
								|| (block.getBlockType() == HEADER && nbt != HEADER)) {
					md.append(NL);
				}
			});
		}

		return md.toString();
	}

	private boolean addTitleAsHeader(Tiddler t) {

		if (addTitles) {
			return true;
		}

		boolean found = false;
		if (!addTitlesForTags.isEmpty()) {
			for (String tag : splitTags(t.getHeader(TAGS_HEADER))) {
				if (addTitlesForTags.contains(tag)) {
					found = true;
					break;
				}
			}
		}
		return found;
	}

	private String renderTextBlock(Tiddler t, List<String> block) {

		StringBuilder sb = new StringBuilder();

		for (String l : block) {

			sb.append(renderLine(t, l));
			sb.append(NL);
		}

		return sb.toString().trim();
	}

	/**
	 * render a complete line of text including headers, bullet etc
	 */
	private String renderLine(Tiddler t, String l) {

		String str = l;

		str = TITLE_REGEX.matcher(str).replaceAll(m -> m.group(1).replace('!', '#') + " ");

		return renderText(t, str, false);
	}

	/**
	 * render partial text
	 */
	private String renderText(Tiddler t, String str, boolean tableRow) {

		boolean code = false;

		StringBuilder out = new StringBuilder();
		StringBuilder sb = new StringBuilder();

		for (char c : str.toCharArray()) {
			if (c == '`') {
				if (code) {
					out.append(c);
				} else {
					sb.append(c);
					out.append(renderTextFragment(t, sb.toString(), tableRow));
					sb.setLength(0);
				}
				code = !code;
			} else if (code) {
				out.append(c);
			} else {
				sb.append(c);
			}
		}

		out.append(renderTextFragment(t, sb.toString(), tableRow));

		return out.toString();
	}

	private String renderTextFragment(Tiddler t, String s, boolean tableRow) {

		String escStr = tableRow ? "\\\\" : "";

		log.debug("Render: {}, ul: {}, sub: {}, sup: {}", s, openUnderline, openSub, openSup);


		while (UNDERLINE_REGEX.matcher(s).find()) {
			s = UNDERLINE_REGEX.matcher(s).replaceFirst(m -> openUnderline ? "<u>" : "</u>");
			openUnderline = !openUnderline;
		}

		while (SUPER_REGEX.matcher(s).find()) {
			s = SUPER_REGEX.matcher(s).replaceFirst(m -> openSup ? "<sup>" : "</sup>");
			openSup = !openSup;
		}

		while (SUB_REGEX.matcher(s).find()) {
			s = SUB_REGEX.matcher(s).replaceFirst(m -> openSub ? "<sub>" : "</sub>");
			openSub = !openSub;
		}

		// bold
		s = s.replaceAll("''", "**");

		// italic
		s = s.replaceAll("([^:])//", "$1_");
		s = s.replaceFirst("^//", "_");

		// external links
		s = s.replaceAll("\\[\\[([^|]+)\\|(http[^]]+)]]", "[$1]($2)");
		s = s.replaceAll("\\[\\[(http[^]]*)]]", "$1");

		// internal links with display text
		s = s.replaceAll("\\[\\[([^|]*)\\|([^]]*)]]", "[[$2" + escStr + "|$1]]");

		// image links with sizing [img width=100 height=90 [image.png]]
		s = s.replaceAll("\\[img *width=(\\d+) +height=(\\d+) +\\[([^]]*)]]", "![[$3" + escStr + "|$1x$2]]");
		s = s.replaceAll("\\[img *height=(\\d+) +width=(\\d+) +\\[([^]]*)]]", "![[$3" + escStr + "|$2x$1]]");
		s = s.replaceAll("\\[img *width=(\\d+) +\\[([^]]*)]]", "![[$2" + escStr + "|$1]]");

		// image links [img [image.png]]
		s = s.replaceAll("\\[img *\\[([^]]*)]]", "![[$1]]");

		// {{!!header-name}}
		s = Pattern.compile("\\{\\{!!([^]]*)}}").matcher(s).replaceAll(m -> renderHeader(t, m.group(0), m.group(1)));

		// transcoding
		s = s.replaceAll("\\{\\{([^]]*)}}", "![[$1]]");
		s = s.replaceAll("\\{\\{\\{([^}]*)}}}", "`$0`");

		// tag macro
		s = Pattern.compile("<<tag +([^>]+)>>").matcher(s).replaceAll(m -> renderTag(m.group(1)));

		// my custom macro
		s = s.replaceAll("<<tkt +([^ ]+) +'([^']+)'>>", "[[$1]] - $2");
		s = s.replaceAll("<<tkt +([^ ]+) *>>", "[[$1]]");

		// <<richlink "files/foo/bar.mp4">>
		s = Pattern.compile("<<richlink +\"([^\"]+)\" *>>").matcher(s).replaceAll(m -> renderRichlink(m.group(1)));

		// comment unknown macros
		s = s.replaceAll("<<.*>>", "`$0`");
		s = s.replaceAll("<\\$[^>]*>", "`$0`");
		s = s.replaceAll("</\\$[^>]*>", "`$0`");

		return s;
	}

	private String renderHeader(Tiddler t, String transclusion, String key) {

		final String header = t.getHeader(key);
		return header == null ? "`" + transclusion + "`" : header;
	}

	private String renderCodeBlock(List<String> block) {

		StringBuilder sb = new StringBuilder();

		for (String l : block) {
			sb.append(l).append(NL);
		}

		return sb.toString().trim();
	}

	private String renderTableBlock(Tiddler t, List<String> block) {

		StringBuilder sb = new StringBuilder();

		for (int i = 0, blockSize = block.size(); i < blockSize; i++) {
			String l = renderText(t, block.get(i), true).trim();

			// render the row cells
			String tr = l.substring(1, l.length() - 1);
			final String[] cells = tr.split("(?<!\\\\)\\|");
			log.debug("Cells: {} -> {}", tr, Arrays.toString(cells));
			for (int j = 0, cellsLength = cells.length; j < cellsLength; j++) {
				sb.append(cells[j].replaceAll("^ *!", "").trim());
				if (j < cellsLength - 1) {
					sb.append(" | ");
				}
			}
			sb.append(NL);

			// render the hyphen row, always row 2
			if (i == 0) {
				for (int j = 0, cellsLength = cells.length; j < cellsLength; j++) {
					sb.append("---");
					if (j < cellsLength - 1) {
						sb.append(" | ");
					} else {
						sb.append(NL);
					}
				}
			}
		}

		return sb.toString().trim();
	}

	private String renderQuoteBlock(Tiddler t, List<String> block) {

		StringBuilder sb = new StringBuilder();

		for (String l : block.subList(1, block.size() - 1)) {
			final ArrayList<String> list = new ArrayList<>();
			list.add(l);
			sb.append(("> " + renderTextBlock(t, list)).trim()).append(NL);
		}

		return sb.toString().trim();
	}

	private String renderList(Tiddler t, List<String> block) {

		StringBuilder sb = new StringBuilder();

		boolean isCheckList = false;

		if (detectChecklists) {
			for (String s : block) {
				if (isRowChecked(s)) {
					isCheckList = true;
					break;
				}
			}
		}

		for (int i = 0; i < block.size(); i++) {

			String str = block.get(i);
			boolean isHeader = false;

			if (detectChecklistHeaders) {
				if (i < block.size() - 1) {
					String strNext = block.get(i + 1);
					final int il1 = rowIndentLevel(str);
					final int il2 = rowIndentLevel(strNext);
					log.debug("Row indents: {} indent {}, next line {}", str, il1, il2);
					isHeader = il1 < il2;
				}
			}

			// check lists and convert to checklists if any item has been struck out
			final String checkboxMarkdown;
			final boolean checked = isRowChecked(str);
			if (!isHeader && isCheckList && checked) {
				checkboxMarkdown = "[x] ";
			} else if (!isHeader && isCheckList) {
				checkboxMarkdown = "[ ] ";
			} else {
				checkboxMarkdown = "";
			}

			if (checked) {
				// remove the existing strikethrough
				final Matcher matcher = Pattern.compile("^( *[#-*]+ *)~~(.*)~~$").matcher(str);
				if (matcher.matches()) {
					str = matcher.group(1) + matcher.group(2);
				}
			}

			str = NUMBER_LIST_REGEX.matcher(str)
							.replaceAll(m -> m.group(1).replace('#', '\t').substring(1) + "1. " + checkboxMarkdown);

			str = BULLET_LIST_REGEX.matcher(str)
							.replaceAll(m -> m.group(1).replaceAll("[-*]", "\t").substring(1) + "- " + checkboxMarkdown);

			sb.append(renderLine(t, str));
			sb.append(NL);
		}

		return sb.toString().trim();
	}

	private boolean isRowChecked(String s) {

		return Pattern.compile("^ *([#-*]+) *~~.*~~$").matcher(s).matches();
	}

	private int rowIndentLevel(String s) {

		final Matcher matcher = Pattern.compile("^ *([#*\\-]+).*$").matcher(s);

		if (matcher.matches()) {
			return matcher.group(1).length();
		} else {
			return 0;
		}
	}

	private String renderFrontMatter(Tiddler t, String filename) {

		Map<String, Object> data = new HashMap<>();

		Optional.ofNullable(t.getHeader(TITLE_HEADER))
						.filter(prefix -> !filename.equals(prefix + TIDDLER_EXT))
						.ifPresent(s -> {
							List<String> ss = new ArrayList<>();
							ss.add(s);
							data.put(ALIASES_FRONTMATTER, ss);
						});

		Optional.ofNullable(t.getHeader(TAGS_HEADER))
						.filter(s -> !s.isBlank())
						.map(this::splitTags)
						.map(this::renderTags)
						.ifPresent(tags -> data.put(TAGS_FRONTMATTER, tags));

		StringBuilder md = new StringBuilder();
		if (!data.isEmpty()) {
			Yaml yaml = new Yaml();
			StringWriter writer = new StringWriter();
			yaml.dump(data, writer);
			md.append("---").append(NL);
			md.append(writer);
			md.append("---").append(NL).append(NL);
		}

		return md.toString();
	}

	/**
	 * read the tiddlywiki tag header and split it into a list of tag strings.
	 */
	private List<String> splitTags(String ts) {

		if (ts == null) {
			return new ArrayList<>();
		}

		log.debug("Splitting tags: {}", ts);

		List<String> tagStrings = new ArrayList<>();
		Matcher matcher = Pattern.compile("\\[\\[([^]]+)]]|([^ ]+)").matcher(ts);
		while (matcher.find()) {
			final String tagStr = Optional.ofNullable(matcher.group(1)).orElse(matcher.group(2));
			tagStrings.add(tagStr);
		}

		return tagStrings;
	}

	/**
	 * render a list of tags into an Obsidian compatible form.
	 */
	private List<String> renderTags(List<String> tagStrings) {

		List<String> tags = new ArrayList<>();
		for (String tag : tagStrings) {
			tags.add(renderTag(tag));
		}
		return tags;
	}

	/**
	 * convert TiddlyWiki tags to Obsidian compatible tags
	 */
	private String renderTag(String tag) {

		// apply case conversion
		String newTag = tagCaseConversion.convert(tag);

		if (Pattern.compile("\\d+").matcher(newTag).matches()) {
			// it's a numeric tag, must be prefixed
			newTag = numericTagPrefix + newTag;
		} else {
			// sort out illegal characters and spaces, then clean up any doubling of illegal characters or spaces
			newTag = newTag.replace(" ", spaceTagCharacterReplacement);
			newTag = newTag.replaceAll("[^\\w1-9/_-]", illegalTagCharacterReplacement);

			if (!spaceTagCharacterReplacement.isEmpty()) {
				newTag = newTag.replaceAll(spaceTagCharacterReplacement + "+", spaceTagCharacterReplacement);
			}

			if (!illegalTagCharacterReplacement.isEmpty()) {
				newTag = newTag.replaceAll(illegalTagCharacterReplacement + "+", illegalTagCharacterReplacement);
			}
		}

		// add the # prefix
		newTag = '#' + newTag;

		log.debug("Convert Tag: {} -> {}", tag, newTag);
		return newTag;
	}

	/**
	 * no case conversion
	 */
	private static String noCaseConversion(String s) {

		return s;
	}

	/**
	 * UPPERCASE
	 */
	private static String upperCaseConversion(String s) {

		return s.toUpperCase(Locale.ROOT);
	}

	/**
	 * lowercase
	 */
	private static String lowerCaseConversion(String s) {

		return s.toLowerCase(Locale.ROOT);
	}

	/**
	 * camelCase
	 */
	private static String camelCaseConversion(String s) {

		return fancyCaseConversion(s, false);
	}

	/**
	 * PascalCase
	 */
	private static String pascalCaseConversion(String s) {

		return fancyCaseConversion(s, true);
	}

	/**
	 * code to convert initial character case only when preceded by a space.
	 */
	private static String fancyCaseConversion(String s, boolean makeFirstCharacterUpperCase) {

		StringBuilder sb = new StringBuilder();

		boolean seenSpace = makeFirstCharacterUpperCase;

		for (char c : s.toCharArray()) {
			if (c == ' ') {
				sb.append(c);
				seenSpace = true;
			} else {
				if (seenSpace) {
					sb.append(Character.toUpperCase(c));
					seenSpace = false;
				} else {
					sb.append(Character.toLowerCase(c));
				}
			}
		}
		return sb.toString();
	}

	/**
	 * I use richlinks sometimes http://richlinks.tiddlyspot.com/ but only to include mp4's and youtube links.
	 */
	private String renderRichlink(String link) {

		log.debug("RICHLINK: {}", link);

		if (link.contains("youtube.com")) {
			// <iframe src="https://www.youtube.com/embed/NnTvZWp5Q7o"></iframe>
			return link.replaceAll("https?://www\\.youtube\\.com/watch\\?v=(.*)$", "<iframe src=\"https://www.youtube.com/embed/$1\"></iframe>\n");
		} else if (link.startsWith("file://")) {
			// <<richlink "file://files...">>
			return link.replaceAll("file://(.*)", "![[$1]]");
		} else if (link.endsWith(".mp4") || link.endsWith(".pdf")) {
			return "![[" + link + "]]";
		} else {
			return "[[" + link + "]]";
		}
	}

	private void setFileTimestamps(Path filePath, ZonedDateTime created, ZonedDateTime modified) {

		try {
			BasicFileAttributeView attributes = Files.getFileAttributeView(filePath, BasicFileAttributeView.class);
			// seems like OSX doesn't like setting the created date :|
			FileTime ct = FileTime.fromMillis(created.toInstant().toEpochMilli());
			FileTime mt = FileTime.fromMillis(modified.toInstant().toEpochMilli());
			attributes.setTimes(mt, mt, ct);
		} catch (IOException e) {
			log.error("Error setting file timestamps {}, created {}, modified {}, {}", filePath, created, modified, e.getMessage(), e);
		}
	}

	private void saveBinaryTiddler(Tiddler t, Path outPath) throws IOException {

		final byte[] data = Base64.getMimeDecoder().decode(String.join(NL, t.getBody()));
		Files.write(outPath, data);
	}
}
